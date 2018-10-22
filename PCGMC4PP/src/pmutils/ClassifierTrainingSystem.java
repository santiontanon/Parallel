package pmutils;

import playermodeling.*;
import pmexperiments.TelemetryUtils35Dash;
import pmexperiments.TelemetryUtils9Dash;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ClassifierTrainingSystem {

    public boolean debug;
    protected double interval;

    /* Machine Learning-specific variables */
    protected Classifier classifier;
    protected Instances trainingDataset;
    protected ArrayList<Attribute> attributes;
    protected ArrayList<String> annotationValues;

    /* Player modeling-related components */
    private TelemetryUtils35Dash telemetryUtils35Dash;
    private TelemetryUtils9Dash telemetryUtils9Dash;

    public FeatureExtraction featureExtraction;
    public MEExecutionAnalyzer meExecutionAnalyzer35Dash;
    public MEExecutionAnalyzer meExecutionAnalyzer9Dash;
    private SkillAnalyzer analyzer;

    private String [] sliceFiles = {
            "player_modeling_files/slices.tsv",
            "player_modeling_files/9-dash-slices.tsv"
    };

    private String [] criticalSectionDirectories = {
            "player_model_files/critical_section_annotations_35_dash",
            "player_model_files/critical_section_annotations_9_dash"
    };

    private int [] studies = {
            35,
            9
    };

    private String [] trainingDataRelativePaths = {
            "../35-dash-dataset",
            "../9-dash-dataset-data-dump"
    };

    public ClassifierTrainingSystem(Classifier cls, double interval_, boolean debug_) {
        interval = interval_;
        classifier = cls;
        debug = debug_;

        /* Setup attributes */
        attributes = new ArrayList<>();
        for (int i = 0; i < FeatureExtraction.features.length; i++) {
            attributes.add(new Attribute(FeatureExtraction.features[i]));
        }
        annotationValues = new ArrayList<>();
        annotationValues.add("A");
        annotationValues.add("B");
        annotationValues.add("C");
        attributes.add(new Attribute("annotations", annotationValues));

        analyzer = new SkillAnalyzer("player_modeling_files/");

        telemetryUtils35Dash = new TelemetryUtils35Dash();
        telemetryUtils9Dash = new TelemetryUtils9Dash();

        featureExtraction = new FeatureExtraction(analyzer);
        meExecutionAnalyzer35Dash = new MEExecutionAnalyzer(analyzer, criticalSectionDirectories[0], false);
        meExecutionAnalyzer9Dash = new MEExecutionAnalyzer(analyzer, criticalSectionDirectories[1], false);
    }

    private ArrayList<String> getSlices(String username, double t1, double t2, String sliceFilePath) {
        ArrayList<String> slices = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(sliceFilePath));
            String line;
            br.readLine(); /* Skip first line */
            while ((line = br.readLine()) != null) {
                String[] tmp = line.split("\t");
                int offset = Integer.parseInt(tmp[2]);
                int start = Integer.parseInt(tmp[3]);
                int end = Integer.parseInt(tmp[4]);

                String label = tmp[5].trim();
                if (!tmp[0].equals(username)) {
                    continue;
                }
                if (start - offset == end - offset) {
                    continue;
                }

                if ((t1 < start - offset) && (t2 > end - offset)) {
                    /* Within the time window */
                    String s = String.format("%d,%d,%s", start - offset, end - offset, label);
                    slices.add(s);
                } else {
                    if ((t2 > end - offset && t1 < end - offset) || (t2 > start - offset && t1 < start - offset)) {
                        String s = String.format("%d,%d,%s", start - offset, end - offset, label);
                        slices.add(s);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return slices;
    }

    public void createTrainingDataset(String trainingDataRelativePath, int study) {
        if (debug) {
            System.out.println("-------------------Training dataset-------------------");
        }
        trainingDataset = new Instances("Training_dataset", attributes, 10);
        trainingDataset.setClassIndex(attributes.size() - 1);

        LinkedHashMap<String, ArrayList<String>> meExecutionFiles = null;

        if ( study == 35 ) {
            meExecutionFiles = telemetryUtils35Dash.getLevelData(trainingDataRelativePath + "/data/");
        } else {
            meExecutionFiles = telemetryUtils9Dash.getLevelData(trainingDataRelativePath + "/data/");
        }

        assert meExecutionFiles != null;

        File directory = new File(trainingDataRelativePath + "/log/");
        File[] logDirectoryFileList = directory.listFiles();
        if (debug) {
            System.out.println("Relative path of training data: " + trainingDataRelativePath);
            System.out.println(String.format("-------------------------- STUDY %d --------------------------", study));
        }
        for (File f : logDirectoryFileList) {
            if ( f.getName().equals(".DS_Store") ) {
                /* OSX generated file */
                continue;
            }

            ArrayList<String> telemetry = telemetryUtils35Dash.readTelemetryFile(f.getAbsolutePath());
            if (debug) {
                System.out.println("Log file absolute path: " + f.getAbsolutePath());
            }

            String username;
            if ( study == 35 ) {
                username = telemetryUtils35Dash.getUsername(telemetry);
            } else {
                username = telemetryUtils9Dash.getUsername(telemetry);
            }
            if (debug) {
                System.out.println("Currently looking at user " + username);
            }

            HashMap<String, ArrayList<String>> telemetryByLevel = telemetryUtils35Dash.splitTelemetryByLevels(telemetry);

            for (String level : telemetryByLevel.keySet()) {
                ArrayList<String> levelTelemetry = telemetryByLevel.get(level);

                LinkedHashMap<String, ArrayList<String>> telemetryByRun = telemetryUtils35Dash.splitTelemetryByRun(levelTelemetry);
                for ( String meExecutionFile : telemetryByRun.keySet() ) {

                    ArrayList<String> runTelemetry = telemetryByRun.get(meExecutionFile);
                    PersistentData persistentData;
                    /*
                        35 dash - filename is within the ME file
                         9 dash - filename is the ME file
                     */
                    if ( study == 35 ) {
                        String meExecutionFile_ = meExecutionFiles.get(meExecutionFile).get(0);
                        persistentData = meExecutionAnalyzer35Dash.analyzeMEExecution(trainingDataRelativePath + "/data/" + meExecutionFile_);
                    } else {
                        String meExecutionFile_ = meExecutionFile.substring(meExecutionFile.lastIndexOf(File.separator));
                        persistentData = meExecutionAnalyzer9Dash.analyzeMEExecution(trainingDataRelativePath + "/data/" + meExecutionFile_);
                    }

                    String firstLine = runTelemetry.get(0);
                    double runStartTime = Double.parseDouble(firstLine.split("\t")[3]);

                    String lastLine = runTelemetry.get(runTelemetry.size() - 1);
                    double runEndTime = Double.parseDouble(lastLine.split("\t")[3]);

                    double t1 = runStartTime;
                    double t2 = runStartTime + interval;

                    String date_ = firstLine.split("\t")[0];
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
                    LocalDateTime time = LocalDateTime.parse(date_, formatter);
                    persistentData.persistent_data.put("start_time", ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 ));

                    while (t1 < runEndTime) {
                        ArrayList<String> telemetryInInterval = telemetryUtils35Dash.getTelemetryInInterval(runTelemetry, t1, t2);
                        HashMap<String, Double> featureVector = featureExtraction.extractFeatureVectorPM(telemetryInInterval, persistentData);

                        ArrayList<String> slices;
                        if ( study == 35 ) {
                            slices = getSlices(username, t1, t2, sliceFiles[0]);
                        } else {
                            slices = getSlices(username, t1, t2, sliceFiles[1]);
                        }

                        ArrayList<String> labels = new ArrayList<String>();
                        for (String slice : slices) {
                            String[] split_slice = slice.split(",");
                            if (split_slice[2].equals("B/C") || split_slice[2].equals("A/B")) {
                                /* Just ignore this */
                                continue;
                            }
                            labels.add(split_slice[2]);
                        }

                        Set<String> uniqueLabels = new HashSet<String>(labels);
                        ArrayList<String> common = new ArrayList<String>();

                        t1 += interval / 2.0;
                        t2 = t1 + interval;

                        if (featureVector.size() == 0) {
                            /* Don't consider this */
                            continue;
                        }

                        int maxFrequency = 0;
                        for (String s : uniqueLabels) {
                            int freq = Collections.frequency(labels, s);
                            if (freq > maxFrequency) {
                                maxFrequency = freq;
                                common.clear();
                                common.add(s);
                            } else {
                                if (freq == maxFrequency) {
                                    /* Check the ranking */
                                    common.add(s);
                                }
                            }
                        }

                        String label = "";
                        if (common.size() == 0) {
                            continue;
                        } else if (common.size() == 1) {
                            label = common.get(0);
                        } else {
                            if (common.contains("C")) {
                                label = "C";
                            } else if (common.contains("B")) {
                                label = "B";
                            } else if (common.contains("A")) {
                                label = "A";
                            }
                        }

                        if (debug) {
                            System.out.println("----------------------------------");
                            int i = 0;
                            for (String s : FeatureExtraction.features) {
                                System.out.print((i == featureVector.size() - 1) ? featureVector.get(s) + "\n" : featureVector.get(s) + ",");
                                if (featureVector.get(s) < 0.0) {
                                    System.err.println(s + " is negative. This is inappropriate behavior. Exiting");
                                    System.exit(-1);
                                }
                                i++;
                            }
                            System.out.println("----------------------------------");
                        }

                        Instance trainingInstance = new DenseInstance(featureVector.size() + 1);
                        for (Attribute a : attributes) {
                            if (a.name().equals("annotations")) {
                                trainingInstance.setValue(a, label);
                            } else {
                                trainingInstance.setValue(a, featureVector.get(a.name()));
                            }
                        }
                        trainingDataset.add(trainingInstance);
                    }
                }
            }
        }
    }

    public void saveTrainingModel() {
        try {
            String directoryName =  "saved_models/";
            System.out.println("Saving training model to: " + directoryName + String.format("%s.%d.model", classifier.getClass().getName(), (int)interval));
            new File(directoryName).mkdir();
            weka.core.SerializationHelper.write( directoryName + String.format("%s.%d.model", classifier.getClass().getName(), (int)interval), classifier);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public void trainModel() {
        try {
            classifier.buildClassifier(trainingDataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void execute() {
        for ( int i = 0; i < studies.length; i++ ) {
            if (debug) {
                System.out.println(String.format("Creating training dataset for: %d", studies[i]));
            }
            createTrainingDataset(trainingDataRelativePaths[i], studies[i]);
        }
        if (debug) {
            System.out.println("---------------------- Training model! ----------------------");
            System.out.println("Classifier used: " + classifier.getClass().getSimpleName());
        }
        trainModel();
        if (debug) {
            System.out.println("---------------------- Saving model! ----------------------");
        }
        saveTrainingModel();
    }

    public static void main(String [] args) {
        ClassifierTrainingSystem classifierTrainingSystem = new ClassifierTrainingSystem(new BayesNet(), 10, true);
        classifierTrainingSystem.execute();
    }
}
