package pmexperiments;

import playermodeling.*;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.*;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TrainingExperiments359Dash extends AbstractPlayerModeler {

    public boolean debug;
    public String playerModelingDirectory = "player_modeling_files/";

    /* Player modeling-related components */
    private TelemetryUtils35Dash telemetryUtils35Dash;
    private TelemetryUtils8Dash telemetryUtils8Dash;

    public FeatureExtraction featureExtraction;
    public MEExecutionAnalyzer meExecutionAnalyzer35Dash;
    public MEExecutionAnalyzer meExecutionAnalyzer8Dash;

    private SkillAnalyzerExperiment8Dash skillAnalyzerExperiment8Dash;

    private String [] sliceFiles = {
            "player_modeling_files/slices.tsv",
            "player_modeling_files/9-dash-slices.tsv"
    };

    private String [] criticalSectionDirectories = {
            "player_model_files/critical_section_annotations_35_dash",
            "player_model_files/critical_section_annotations_8_dash"
    };

    private int [] studies = {
            35,
            9
    };

    private String [] trainingDataRelativePaths = {
            "../35-dash-dataset",
            "../9-dash-dataset-data-dump"
    };
    private String testingDataRelativePath = "../8-dash-dataset";

    public TrainingExperiments359Dash(Classifier cls, double interval_,  int skillVectorUpdateTechniqueFlag_, boolean debug_) {
        super(cls, interval_, skillVectorUpdateTechniqueFlag_);
        debug = debug_;

        skillAnalyzerExperiment8Dash = new SkillAnalyzerExperiment8Dash(playerModelingDirectory, "8_dash_skills_ordered.csv");
        featureExtraction = new FeatureExtraction(skillAnalyzerExperiment8Dash);

        telemetryUtils35Dash = new TelemetryUtils35Dash();
        telemetryUtils8Dash = new TelemetryUtils8Dash(playerModelingDirectory);

        meExecutionAnalyzer35Dash = new MEExecutionAnalyzer(skillAnalyzerExperiment8Dash, criticalSectionDirectories[0], false);
        meExecutionAnalyzer8Dash = new MEExecutionAnalyzer(skillAnalyzerExperiment8Dash, criticalSectionDirectories[1], false);

        training_dataset = new Instances("Training_dataset", attributes, 10);
        training_dataset.setClassIndex(attributes.size() - 1);
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

        LinkedHashMap<String, ArrayList<String>> meExecutionFiles;

        if ( study == 35 ) {
            meExecutionFiles = telemetryUtils35Dash.getLevelData(trainingDataRelativePath + "/data/");
        } else {
            meExecutionFiles = new LinkedHashMap<>();
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
                username = telemetryUtils8Dash.getUsername(telemetry);
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
                        persistentData = meExecutionAnalyzer8Dash.analyzeMEExecution(trainingDataRelativePath + "/data/" + meExecutionFile_);
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
                        training_dataset.add(trainingInstance);
                    }
                }
            }
        }
    }

    @Override
    public void saveTrainingModel() {
        try {
            String directoryName =  "saved_models/";
            if (debug) {
                System.out.println("Saving training model to: " + directoryName + String.format("%s.%d.model", classifier.getClass().getName(), (int) interval));
            }
            new File(directoryName).mkdir();
            weka.core.SerializationHelper.write( directoryName + String.format("%s.%d.model", classifier.getClass().getName(), (int)interval), classifier);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @Override
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

        LinkedHashMap<String, LinkedHashMap<String, Double> > totalSquareErrorPerWeek = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, Integer>> weekToSkillToNumUsers = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();

        /* For printing data */
        ArrayList<String> skillVectorAsCsv = new ArrayList<String>();
        ArrayList<String> groundTruthAsCsv = new ArrayList<String>();
        ArrayList<String> squaredErrorPerWeekAsCsv = new ArrayList<String>();
        ArrayList<String> ruleEvidenceAsCsv = new ArrayList<String>();

        /* Student -> Week -> list of telemetry files */
        HashMap< String, LinkedHashMap< String, ArrayList<String> > > players = telemetryUtils8Dash.getPlayers8Dash(testingDataRelativePath + "/log");

        /* NOTE: It is possible that we may not have ground truth for all players. Ignore these players */
        LinkedHashMap< String, LinkedHashMap< String, Double > > groundTruth = skillAnalyzerExperiment8Dash.ground_truth;
        ArrayList<String> playersWithGroundTruth = new ArrayList<String>();
        for ( String s : groundTruth.keySet() ) {
            String [] tmp = s.split(",");
            String player = tmp[0].replace(".", "-");
            if ( !playersWithGroundTruth.contains(player) ) {
                playersWithGroundTruth.add(player);
            }
        }

        if (debug) {
            System.out.println("Players to files played in a week");
            for (String player : players.keySet()) {
                for (String week : players.get(player).keySet()) {
                    System.out.println(player + "," + week + "," + String.join(",", players.get(player).get(week)));
                }
            }
        }

        LinkedHashMap<String, ArrayList<String>> meExecutionFiles = telemetryUtils8Dash.getLevelData(testingDataRelativePath + "/data/");

        /* User, Level, Skill list */
        System.out.print("User,Level,");
        System.out.println(String.join(",", skillAnalyzerExperiment8Dash.skill_vector.keySet()));

        for (String player : players.keySet()) {

            if ( !playersWithGroundTruth.contains(player) ) {
                continue;
            }

            skillAnalyzerExperiment8Dash.resetSkillVector();

            String reformatted_username = player.replace("-", ".");

            for ( String week : players.get(player).keySet() ) {
                for (String fname : players.get(player).get(week)) {
                    File f = new File(testingDataRelativePath + "/log/" + fname);
                    ArrayList<String> telemetry = telemetryUtils8Dash.readTelemetryFile(f.getAbsolutePath());

                    LinkedHashMap<String, ArrayList<String>> telemetryByLevels = telemetryUtils8Dash.splitTelemetryByLevels(telemetry);

                    for (String level : telemetryByLevels.keySet()) {

                        skillAnalyzerExperiment8Dash.resetSkillsPerLevel();
                        skillAnalyzerExperiment8Dash.resetRuleEvidence();

                        if (!skillAnalyzerExperiment8Dash.readSkillsForLevel(playerModelingDirectory + "/skill_list_per_level_8_dash/", level)) {
                            /* If level was not supposed to be played, then move to the next level */
                            continue;
                        }

                        ArrayList<String> levelTelemetry = telemetryByLevels.get(level);

                        LinkedHashMap<String, ArrayList<String>> telemetryByRun = telemetryUtils8Dash.splitTelemetryByRun(levelTelemetry);
                        for (String meExecutionFile : telemetryByRun.keySet()) {

                            ArrayList<String> runTelemetry = telemetryByRun.get(meExecutionFile);
                            String meExecutionFile_ = meExecutionFiles.get(meExecutionFile).get(0);
                            PersistentData persistentData = meExecutionAnalyzer8Dash.analyzeMEExecution(testingDataRelativePath + "/data/" + meExecutionFile_);

                            String firstLine = runTelemetry.get(0);
                            double runStartTime = Double.parseDouble(firstLine.split("\t")[3]);

                            String lastLine = runTelemetry.get(runTelemetry.size() - 1);
                            double runEndTime = Double.parseDouble(lastLine.split("\t")[3]);

                            double t1 = runStartTime;
                            double t2 = runStartTime + interval;

                            String date_ = firstLine.split("\t")[0];
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
                            LocalDateTime time = LocalDateTime.parse(date_, formatter);
                            persistentData.persistent_data.put("start_time", (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000));

                            if (debug) {
                                System.out.println("Level: " + level + ", start time: " + runStartTime + ", end time: " + runEndTime);
                            }
                            while (t1 < runEndTime) {
                                ArrayList<String> telemetryInInterval = telemetryUtils8Dash.getTelemetryInInterval(runTelemetry, t1, t2);
                                HashMap<String, Double> featureVector = featureExtraction.extractFeatureVectorPM(telemetryInInterval, persistentData);

                                t1 += interval / 2.0;
                                t2 = t1 + interval;

                                /* If feature vector is empty, then either there's no telemetry in the interval OR we have insufficient data to create the feature vector */
                                if (featureVector.size() == 0) {
                                    continue;
                                }

                                switch (skill_vector_update_technique_flag) {
                                    case 0:
                                        /* Both Machine Learning and Rules */
                                        double classification = classifyInstance(featureVector);
                                        skillAnalyzerExperiment8Dash.updateSkillVectorUsingMachineLearning(training_dataset.classAttribute().value((int) classification));
                                    case 1:
                                        /* Only machine learning */
                                        classification = classifyInstance(featureVector);
                                        skillAnalyzerExperiment8Dash.updateSkillVectorUsingMachineLearning(training_dataset.classAttribute().value((int) classification));
                                        break;
                                    case 2:
                                        /* Only rule_evidence */
                                        break;
                                }
                            }

                            /* Update from rule_evidence after completing the level */
                            switch (skill_vector_update_technique_flag) {
                                case 0:
                                    /* Both Machine Learning and Rules */
                                    skillAnalyzerExperiment8Dash.updateSkillVectorUsingRules();
                                case 1:
                                    break;
                                case 2:
                                    /* Only rule_evidence */
                                    skillAnalyzerExperiment8Dash.updateSkillVectorUsingRules();
                                    break;
                            }
                        }
                    }
                }

                /* Data structure should contain all skills that have ground truth */
                LinkedHashMap<String, Double> squared_error = skillAnalyzerExperiment8Dash.evaluateSkills(week, reformatted_username);

                if ( !weekToSkillToNumUsers.containsKey(week) ) {
                    LinkedHashMap<String, Integer> skill_to_num_users = new LinkedHashMap<>();
                    for ( String s : squared_error.keySet() ) {
                        if ( squared_error.get(s) < 0 ) {
                            /* SE doesn't exist for this skill in this week for this specific user.  */
                            skill_to_num_users.put(s,0);
                        } else {
                            skill_to_num_users.put(s,1);
                        }
                    }
                    weekToSkillToNumUsers.put(week,skill_to_num_users);
                } else {
                    LinkedHashMap<String, Integer> skill_to_num_users = weekToSkillToNumUsers.get(week);
                    for ( String s : squared_error.keySet() ) {
                        if ( squared_error.get(s) > -0.00000000000000000000000000001 ) {
                            skill_to_num_users.put(s, skill_to_num_users.get(s) + 1);
                        }
                    }
                    weekToSkillToNumUsers.put(week,skill_to_num_users);
                }

                if ( !totalSquareErrorPerWeek.containsKey(week) ) {
                    totalSquareErrorPerWeek.put(week,squared_error);
                } else {
                    LinkedHashMap<String, Double > square_error_for_week = totalSquareErrorPerWeek.get(week);
                    for (String s : squared_error.keySet()) {
                        if (!square_error_for_week.containsKey(s)) {
                            /* This should technically never happen */
                            System.err.println("A skill is missing when summing up the squared errors for a week???");
                            System.exit(-1);
                        } else {
                            if ( square_error_for_week.get(s) > -0.00000000000000000000000000001 ) {
                                if ( squared_error.get(s) >  -0.00000000000000000000000000001 ) {
                                    double val = square_error_for_week.get(s);
                                    square_error_for_week.replace(s,val + squared_error.get(s));
                                }
                            } else {
                                square_error_for_week.replace(s, squared_error.get(s));
                            }
                        }
                    }
                }

                skillVectorAsCsv.add(reformatted_username + "," + week + "," + skillAnalyzerExperiment8Dash.printSkillVector());
                groundTruthAsCsv.add(reformatted_username + "," + week + "," + skillAnalyzerExperiment8Dash.getGroundTruthCSV(week, reformatted_username));
                ruleEvidenceAsCsv.add(reformatted_username + "," + week + "," + skillAnalyzerExperiment8Dash.printRuleEvidence());
                squaredErrorPerWeekAsCsv.add(reformatted_username + "," + week + "," + skillAnalyzerExperiment8Dash.getSquaredErrorCSV(week, reformatted_username));
            }
        }

        System.out.println("Skill vectors");
        System.out.println(String.join("\n", skillVectorAsCsv));
        System.out.println("Rules skill vector");
        System.out.println(String.join("\n", ruleEvidenceAsCsv));
        System.out.println("Ground Truth");
        System.out.println(String.join("\n", groundTruthAsCsv));
        System.out.println("Squared Error (SE)");
        System.out.println(String.join("\n", squaredErrorPerWeekAsCsv));

        System.out.println("Mean Squared Error over all users per week ( For each skill, sum of squared error for all users in the week / number of users in that week )");
        for (String week : totalSquareErrorPerWeek.keySet()) {
            LinkedHashMap<String, Double> total_square_error = totalSquareErrorPerWeek.get(week);
            ArrayList<String> print_strings = new ArrayList<String>();
            for (String skill : total_square_error.keySet()) {
                if (total_square_error.get(skill) < 0) {
                    print_strings.add("");
                } else {
                    if (weekToSkillToNumUsers.get(week).get(skill) == 0) {
                        /* This should actually never happen ( means this skill was never used by anyone ) */
                        System.out.println("Skill was not used by anyone: " + skill);
                        System.exit(-1);
                    } else {
                        print_strings.add(Double.toString(total_square_error.get(skill) / weekToSkillToNumUsers.get(week).get(skill)));
                    }
                }
            }
            System.out.println("user," + week + "," + String.join(",", print_strings));
        }

    }

    public static void main(String [] args)  throws Exception  {

        int[] skill_vector_update_technique_flags = {0, 1, 2};
        String[] update_names = {"RM", "M", "R"};
        int[] time_intervals = {10, 20, 30};

        String directory_name = "accuracy-results-8-dash/";
        new File(directory_name).mkdir();

        String[] machine_learning_classes = {
                "weka.classifiers.trees.J48",
                "weka.classifiers.trees.RandomForest",
                "weka.classifiers.meta.Bagging",
                "weka.classifiers.meta.AdaBoostM1",
                "weka.classifiers.bayes.NaiveBayes",
                "weka.classifiers.bayes.BayesNet",
                "weka.classifiers.functions.MultilayerPerceptron"
        };
        String [] machine_learning_algorithms = new String[machine_learning_classes.length];
        for ( int i = 0; i < machine_learning_classes.length; i++ ) {
            String [] split_class_string = machine_learning_classes[i].split("\\.");
            machine_learning_algorithms[i] = split_class_string[split_class_string.length-1];
        }


        for (int time : time_intervals) {
            for (int utf : skill_vector_update_technique_flags) {
                if (utf == 0 || utf == 1) {
                    for (int i = 0; i < machine_learning_algorithms.length; i++) {
                        Class c1 = Class.forName(machine_learning_classes[i]);
                        Constructor con1 = c1.getConstructor();
                        Classifier classifier = (Classifier) con1.newInstance();
                        String fname = directory_name + String.format("%s.%s.%d.csv",update_names[utf],machine_learning_algorithms[i],time);
                        PrintStream old = System.out;

                        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fname)), true));

                        TrainingExperiments359Dash classifierTrainingSystem = new TrainingExperiments359Dash(classifier, time, utf, false);
                        classifierTrainingSystem.execute();

                        System.setOut(old);
                    }
                } else {
                    String fname = directory_name + String.format("%s.%d.csv",update_names[utf],time);
                    PrintStream old = System.out;

                    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fname)), true));

                    TrainingExperiments359Dash classifierTrainingSystem = new TrainingExperiments359Dash(new BayesNet(), time, utf, false);
                    classifierTrainingSystem.execute();

                    System.setOut(old);
                }
            }
        }
    }
}
