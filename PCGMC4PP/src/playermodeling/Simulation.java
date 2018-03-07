package playermodeling;

/**
 * Created by pavankantharaju on 3/1/18.
 */

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.io.FileReader;
import java.io.BufferedReader;

public class Simulation {

    private Classifier classifier;
    private TelemetryUtils tel_utils;
    private FeatureExtraction feat_extract;
    private SkillAnalysis analyzer;

    private String path;
    private String slices_file;
    private double interval;

    private ArrayList<Attribute> attributes;
    private Instances instances;

    public Simulation(String p, String slicefile, double ivl) {
        path = p;
        slices_file = slicefile;
        interval = ivl;

        classifier = new NaiveBayes();
        analyzer = new SkillAnalysis();
        tel_utils = new TelemetryUtils(path);
        feat_extract = new FeatureExtraction();


        /* Setup attributes */
        attributes = new ArrayList<Attribute>();
        for ( int i = 0; i < feat_extract.features.length; i++ ) {
            attributes.add(new Attribute(feat_extract.features[i]));
        }
        ArrayList<String> annotation_values = new ArrayList<String>();
        annotation_values.add("A"); annotation_values.add("B"); annotation_values.add("C");
        attributes.add(new Attribute("annotations",annotation_values));

    }

    private Instances getTrainingDataset(ArrayList<String> train_files) {
        /* Get all the players in the log files */
        instances = new Instances("Training_dataset",attributes,10);
        instances.setClassIndex(attributes.size()-1);

        HashMap<String, ArrayList<String> > data_files = tel_utils.getLevelData( path + "/data/" );

        /* For now, open a log file, and extract slices for a given time interval */
        File directory = new File(path + "/log/");
        File [] dir_list = directory.listFiles();
        for ( File f : dir_list ) {
            if ( !train_files.contains(f.getName()) ) {
                continue;
            }
            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());

            double t1 = 0;
            double t2 = interval;

            HashMap<String, Object > persistent_data = new HashMap<String,Object>();
            /* Initialize persistent data */

            persistent_data.put("cur_track", -1);
            persistent_data.put("cur_mouse_comp", "");
            persistent_data.put("cur_mouse_time", 0.0);
            persistent_data.put("linking", false);

            persistent_data.put("direction_layout",new ArrayList<String>());
            persistent_data.put("color_layout",new ArrayList<String>());
            persistent_data.put("comp_link_map",new HashMap<String, String >());
            persistent_data.put("comp_color_map",new HashMap<String, String >());

            /* Find the last possible time */
            String last_log = telemetry.get(telemetry.size()-1);
            double final_time_ = Double.parseDouble(last_log.split("\t")[3]);
            String username = getUser(telemetry);

            while ( t1 < final_time_ ) {
                ArrayList<String> tmp = tel_utils.getDataInIntervals(telemetry, t1, t2);
//                String print_statement = String.format("Printing telemetry data at interval %f to %f for %s",t1,t2,f.getName());
//                System.out.println(print_statement);
//                System.out.println("---------------------------------------");
//                for ( String s : tmp.p1 ) {
//                    System.out.printlnCyanide and Happiness' (s);
//                }
//                System.out.println("---------------------------------------");
                HashMap<String,Double> feature_vector = feat_extract.extractFeatureVector(path, tmp, persistent_data, data_files);

                t1 += interval/2.0;
                t2 = t1 + interval;

                ArrayList<String> slices = getSlices(username,t1,t2);
                ArrayList<String> labels = new ArrayList<String>();
                for ( String slice : slices ) {
                    String [] split_slice = slice.split(",");
                    labels.add(split_slice[2]);
                }

                Set<String> unique_values = new HashSet<String>(labels);
                ArrayList<String> common = new ArrayList<String>();

                int max_freq = 0;
                for ( String s : unique_values ) {
                    int freq = Collections.frequency(labels,s);
                    if ( freq >  max_freq ) {
                        max_freq = freq;
                        common.clear();
                        common.add(s);
                    } else {
                        if ( freq == max_freq ) {
                            /* Check the ranking */
                            common.add(s);
                        }
                    }
                }

                String label = "";
                if ( common.size() == 0 ) {
                    continue;
                } else if ( common.size() == 1 ) {
                    label = common.get(0);
                } else {
                    if ( common.contains("C") ) {
                        label = "C";
                        break;
                    } else if (common.contains("B")) {
                        label = "B";
                        break;
                    } else if (common.contains("A")) {
                        label = "A";
                        break;
                    }
                }
//                System.out.println("----------------------------------");
//                int i = 0;
//                for ( String s : feat_extract.features ) {
//                    System.out.print((i == feature_vector.size()-1) ? feature_vector.get(s) + "\n" : feature_vector.get(s) + ",");
//                    i++;
//                }
//                System.out.println("----------------------------------");
                Instance new_instance = new DenseInstance(feature_vector.size()+1);
                for ( Attribute a : attributes ) {
                    if ( a.name().equals("annotations") ) {
                        new_instance.setValue(a, label);
                    } else {
                        new_instance.setValue(a, feature_vector.get(a.name()));
                    }
                }
                instances.add(new_instance);
            }
        }
        return instances;
    }

    /* Given a feature vector, convert to an instance, and evaulate against our classifier */
    private double classifyInstance(HashMap<String,Double> feature_vector) {
        Instance new_instance = new DenseInstance(feature_vector.size()+1);
        for ( Attribute a : attributes ) {
            if ( a.name().equals("annotations") ) {
                continue;
            } else {
                new_instance.setValue(a, feature_vector.get(a.name()));
            }
        }
        new_instance.setDataset(instances);
        try {
            return classifier.classifyInstance(new_instance);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void simulate() {
        /* Get all the players in the log files */
        HashMap< String, ArrayList<String> > players = tel_utils.getPlayers();
        for ( String player : players.keySet() ) {
            Set<String> train_players = new TreeSet<String>(players.keySet());
            train_players.remove(player);

            /* Train on other_players. Test on player */
            ArrayList<String> train_files = new ArrayList<String>();
            for (String s : train_players) {
                train_files.addAll(players.get(s));
            }

            Instances instances = getTrainingDataset(train_files);

            try {
                classifier.buildClassifier(instances);
            } catch ( Exception e ) {
                e.printStackTrace();
            }

            /* Generate feature vector */
            HashMap<String, ArrayList<String> > data_files = tel_utils.getLevelData( path + "/data/" );

            File f = new File(path + "/log/" + players.get(player).get(0));
            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());

            double t1 = 0;
            double t2 = interval;

            HashMap<String, Object > persistent_data = new HashMap<String,Object>();
            /* Initialize persistent data */

            persistent_data.put("cur_track", -1);
            persistent_data.put("cur_mouse_comp", "");
            persistent_data.put("cur_mouse_time", 0.0);
            persistent_data.put("linking", false);

            persistent_data.put("direction_layout",new ArrayList<String>());
            persistent_data.put("color_layout",new ArrayList<String>());
            persistent_data.put("comp_link_map",new HashMap<String, String >());
            persistent_data.put("comp_color_map",new HashMap<String, String >());

            /* Find the last possible time */
            String last_log = telemetry.get(telemetry.size()-1);
            double final_time_ = Double.parseDouble(last_log.split("\t")[3]);
            String username = getUser(telemetry);

            while ( t1 < final_time_ ) {
                ArrayList<String> tmp = tel_utils.getDataInIntervals(telemetry, t1, t2);
//                String print_statement = String.format("Printing telemetry data at interval %f to %f for %s",t1,t2,f.getName());
//                System.out.println(print_statement);
//                System.out.println("---------------------------------------");
//                for ( String s : tmp.p1 ) {
//                    System.out.printlnCyanide and Happiness' (s);
//                }
//                System.out.println("---------------------------------------");
                HashMap<String, Double> feature_vector = feat_extract.extractFeatureVector(path, tmp, persistent_data, data_files);

                double classification = classifyInstance(feature_vector);
                System.out.println("Classified as: " + instances.classAttribute().value((int)classification));



                t1 += interval / 2.0;
                t2 = t1 + interval;
            }

        }
//        ArrayList<String> train_files = new ArrayList<String>();
//        File directory = new File(path + "/log/");
//        File [] dir_list = directory.listFiles();
//        for ( File f : dir_list ) {
//            train_files.add(f.getName());
//        }
//        getTrainingDataset(train_files);

//        HashMap<String, ArrayList<String> > data_files = tel_utils.getLevelData( path + "/data/" );
//
//        /* For now, open a log file, and extract slices for a given time interval */
//        File directory = new File(path + "/log/");
//        File [] dir_list = directory.listFiles();
//        for ( File f : dir_list ) {
//            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());
////            System.out.println("Printing telemetry data for : " + f.getName() );
////            System.out.println("---------------------------------------");
////            for ( String s : telemetry ) {
////                System.out.println("Log: " + s);
////            }
////            System.out.println("---------------------------------------");
//
//            double t1 = 0;
//            double t2 = interval;
//
//            HashMap<String, Object > persistent_data = new HashMap<String,Object>();
//
//            /* Find the last possible time */
//            String last_log = telemetry.get(telemetry.size()-1);
//            double final_time_ = Double.parseDouble(last_log.split("\t")[3]);
//            String username = getUser(telemetry);
//
//            while ( t1 < final_time_ ) {
//                ArrayList<String> tmp = tel_utils.getDataInIntervals(telemetry, t1, t2);
////                String print_statement = String.format("Printing telemetry data at interval %f to %f for %s",t1,t2,f.getName());
////                System.out.println(print_statement);
////                System.out.println("---------------------------------------");
////                for ( String s : tmp.p1 ) {
////                    System.out.println(s);
////                }
////                System.out.println("---------------------------------------");
//                feat_extract.extractFeatureVector(path, tmp, persistent_data, data_files);
//
//                t1 += interval/2.0;
//                t2 = t1 + interval;
//
//                ArrayList<String> slices = getSlices(username,t1,t2);
//                ArrayList<String> labels = new ArrayList<String>();
//                for ( String slice : slices ) {
//                    String [] split_slice = slice.split(",");
//                    labels.add(split_slice[2]);
//                }
//
//            }
//
//
//        }


//        HashMap< String, ArrayList<String> > players = tel_extract.getPlayers();
//        for ( String player : players.keySet() ) {
//            Set<String> train_players = new TreeSet<String>(players.keySet());
//            train_players.remove(player);
//
//            /* Train on other_players. Test on player */
//            ArrayList<String> train_files = new ArrayList<String>();
//            for (String s : train_players) {
//                train_files.addAll(players.get(s));
//            }
//
//
//        }
    }

    public String getUser(ArrayList<String> data) {
        for ( String d : data ) {
            String [] tmp = d.split("\t");
            if ( tmp[1].equals("SessionUser") ) {
                return tmp[2];
            }
        }
        return "";
    }

    public ArrayList<String> getSlices(String username, double t1, double t2) {
        ArrayList<String> slices = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(slices_file));
            String line = "";
            br.readLine(); /* Skip first line */
            while ((line = br.readLine()) != null) {
                String[] tmp = line.split("\t");
                int offset = Integer.parseInt(tmp[2]);
                int start = Integer.parseInt(tmp[3]);
                int end = Integer.parseInt(tmp[4]);

                String label = tmp[5].trim();
                if ( !tmp[0].equals(username) ) {
                    continue;
                }
                if ( start-offset == end-offset ) {
                    continue;
                }
                if ( ( t1 < start-offset ) && ( t2 > end-offset ) ) {
                    /* Within the time window */
                    String s = String.format("%d,%d,%s",start-offset,end-offset,label);
                    slices.add(s);
                } else {
                    if ( (t2 > end-offset && t1 < end-offset) || (t2 > start-offset && t1 < start-offset) ) {
                        String s = String.format("%d,%d,%s",start-offset,end-offset,label);
                        slices.add(s);
                    }
                }

            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return slices;
    }
}
