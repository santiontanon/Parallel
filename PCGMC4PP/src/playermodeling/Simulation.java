package playermodeling;

/**
 * Created by pavankantharaju on 3/1/18.
 */

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import game.GameState;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;

import weka.classifiers.evaluation.EvaluationUtils;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Simulation {

    /* Used for data analysis */

    private Classifier classifier;
    private TelemetryUtils tel_utils;
    private FeatureExtraction feat_extract;
    private SkillAnalysis analyzer;

    private String path;
    private String slices_file;
    private double interval;

    private ArrayList<Attribute> attributes;
    private Instances instances;
    private String cur_level;

    private Instances all_test_instances;

    //private Evaluation eval;

    int update_technique_flag;
    int rule_update_flag;

    public Simulation(String p, String slicefile, double ivl, int u_technique_flag, int u_flag ) {
        path = p;
        slices_file = slicefile;
        interval = ivl;
        cur_level = "";

        classifier = new RandomForest();
        analyzer = new SkillAnalysis(u_technique_flag);
        tel_utils = new TelemetryUtils(path);
        feat_extract = new FeatureExtraction(u_flag,analyzer);

        update_technique_flag = u_technique_flag;
        rule_update_flag = u_flag;

        /* Setup attributes */
        attributes = new ArrayList<Attribute>();
        for ( int i = 0; i < feat_extract.features.length; i++ ) {
            attributes.add(new Attribute(feat_extract.features[i]));
        }

        ArrayList<String> annotation_values = new ArrayList<String>();
        annotation_values.add("A"); annotation_values.add("B"); annotation_values.add("C"); annotation_values.add("B/C");
        annotation_values.add("A/B");
        attributes.add(new Attribute("annotations",annotation_values));

        all_test_instances = new Instances("Testing_dataset",attributes,10);
        all_test_instances.setClassIndex(attributes.size()-1);

    }

    private void getTrainingDataset(ArrayList<String> train_files) {
        /* Get all the players in the log files */
        instances = new Instances("Training_dataset", attributes, 10);
        instances.setClassIndex(attributes.size() - 1);

        HashMap<String, ArrayList<String>> data_files = tel_utils.getLevelData(path + "/data/");

        /* For now, open a log file, and extract slices for a given time interval */
        File directory = new File(path + "/log/");
        File[] dir_list = directory.listFiles();
        for (File f : dir_list) {
            if (!train_files.contains(f.getName())) {
                continue;
            }
            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());
            String username = tel_utils.getUser(telemetry);

            HashMap<String, ArrayList<String>> telemetry_by_level = tel_utils.getTelemetryByLevel(telemetry);

            /* Get calibration of user's monitor */
            String calibration = tel_utils.getCalibration(telemetry);

            for (String level : telemetry_by_level.keySet()) {
                ArrayList<String> tel_by_level = telemetry_by_level.get(level);

                analyzer.resetRules();

//                LinkedHashMap<String,Double> rules = new LinkedHashMap<String,Double>();
//                for ( String s : analyzer.skill_map.keySet() ) {
//                    rules.put(s,-1.0);
//                }

                Pair< HashMap<Integer,PersistentData>, ArrayList<Integer> > persistent_data_info = getPersistentDataForLevel(tel_by_level, data_files);

                String first_log = tel_by_level.get(0);
                double start_time_ = Double.parseDouble(first_log.split("\t")[3]);

                String last_log = tel_by_level.get(tel_by_level.size() - 1);
                double final_time_ = Double.parseDouble(last_log.split("\t")[3]);

                double t1 = start_time_;
                double t2 = start_time_ + interval;

                while (t1 < final_time_) {
                    Pair< ArrayList<String> , ArrayList<Integer> > tel_in_interval = tel_utils.getDataInIntervals(tel_by_level, t1, t2, persistent_data_info.p2);

                    HashMap<String, Double> feature_vector = feat_extract.extractFeatureVector(path, tel_in_interval.p1, tel_in_interval.p2, persistent_data_info.p1, data_files, calibration);

                    ArrayList<String> slices = getSlices(username, t1, t2);
                    ArrayList<String> labels = new ArrayList<String>();
                    for (String slice : slices) {
                        String[] split_slice = slice.split(",");
                        labels.add(split_slice[2]);
                    }

                    Set<String> unique_values = new HashSet<String>(labels);
                    ArrayList<String> common = new ArrayList<String>();

                    t1 += interval / 2.0;
                    t2 = t1 + interval;

                    if ( feature_vector.size() == 0 ) {
                        /* Don't consider this */
                        continue;
                    }

                    int max_freq = 0;
                    for (String s : unique_values) {
                        int freq = Collections.frequency(labels, s);
                        if (freq > max_freq) {
                            max_freq = freq;
                            common.clear();
                            common.add(s);
                        } else {
                            if (freq == max_freq) {
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

//                    System.out.println("----------------------------------");
//                    int i = 0;
//                    for ( String s : feat_extract.features ) {
//                        System.out.print((i == feature_vector.size()-1) ? feature_vector.get(s) + "\n" : feature_vector.get(s) + ",");
//                        if( feature_vector.get(s) < 0.0 ) {
//                            System.err.println(s + " is negative. This is inappropriate behavior. Exiting");
//                            System.exit(-1);
//                        }
//                        i++;
//                    }
//                    System.out.println("----------------------------------");

                    Instance new_instance = new DenseInstance(feature_vector.size() + 1);
                    for (Attribute a : attributes) {
                        if (a.name().equals("annotations")) {
                            new_instance.setValue(a, label);
                        } else {
                            new_instance.setValue(a, feature_vector.get(a.name()));
                        }
                    }
                    instances.add(new_instance);
                }
            }
        }
    }



    /* Given a feature vector, convert to an instance, and evaulate against our classifier */
    private double classifyInstance(HashMap<String,Double> feature_vector, String label) {
        Instance new_instance = new DenseInstance(feature_vector.size()+1);
        Instance batch_instance = new DenseInstance(feature_vector.size()+1);

        for ( Attribute a : attributes ) {
            if ( a.name().equals("annotations") ) {
                new_instance.setValue(a, label);
                batch_instance.setValue(a, label);
            } else {
                new_instance.setValue(a, feature_vector.get(a.name()));
                batch_instance.setValue(a, feature_vector.get(a.name()));
            }
        }
        new_instance.setDataset(instances);

        batch_instance.setDataset(all_test_instances);
        all_test_instances.add(batch_instance);

        try {
            //return eval.evaluateModelOnceAndRecordPrediction(classifier,new_instance);
            return classifier.classifyInstance(new_instance);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return 0.0;
    }

    public void simulate() {

        /* Average mean-squared errors (MSE) for each student over all levels */

        LinkedHashMap<String, LinkedHashMap<String,Double> > level_average_mses_for_each_skill = new LinkedHashMap<String, LinkedHashMap<String,Double> >();
//        LinkedHashMap<String, Integer > level_to_user_count = new LinkedHashMap<String, Integer >();
//        LinkedHashMap<String, Integer > level_to_num_skills = new LinkedHashMap<String, Integer >();

        /* Average MSE for each skill over all levels and users */
        LinkedHashMap<String, Double> overall_avg_mse_for_each_skill = new LinkedHashMap<String,Double>();

//        LinkedHashMap<String, Double > level_to_mse = new LinkedHashMap<String, Double>();

        ArrayList<String> csv_skill_predictions = new ArrayList<String>();
        ArrayList<String> csv_ground_truth = new ArrayList<String>();
        ArrayList<String> csv_mse = new ArrayList<String>();
        ArrayList<String> csv_average_mse_users = new ArrayList<String>();
        ArrayList<String> rules_skill_vector = new ArrayList<String>();

        /* Get all the players in the log files */
        HashMap< String, ArrayList<String> > players = tel_utils.getPlayers();

        /* User, Level, Skill list */
        System.out.print("User,Level,");
        System.out.println(String.join(",",analyzer.skill_map.keySet()));
        //System.out.println(",Average MSE/MSE");

//        double overall_mse = 0;
//        int num_iterations = 0;
        LinkedHashMap< String, Integer > skill_to_num_iterations = new LinkedHashMap< String, Integer >();
        LinkedHashMap< String, LinkedHashMap< String, Integer > > level_to_skill_to_num_users = new LinkedHashMap< String, LinkedHashMap< String, Integer > >();

        for ( String player : players.keySet() ) {
            Set<String> train_players = new TreeSet<String>(players.keySet());
            train_players.remove(player);

            /* Train on other_players. Test on player */
            ArrayList<String> train_files = new ArrayList<String>();
            for (String s : train_players) {
                train_files.addAll(players.get(s));
            }

            getTrainingDataset(train_files);

            try {
                classifier.buildClassifier(instances);
                //eval = new Evaluation(instances);
            } catch (Exception e) {
                e.printStackTrace();
            }

            /* Generate feature vector */
            LinkedHashMap<String, ArrayList<String>> data_files = tel_utils.getLevelData(path + "/data/");

            File f = new File(path + "/log/" + players.get(player).get(0));
            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());

            LinkedHashMap<String, ArrayList<String>> telemetry_by_level = tel_utils.getTelemetryByLevel(telemetry);

            /* Get calibration of user's monitor */
            String calibration = tel_utils.getCalibration(telemetry);

            String username = tel_utils.getUser(telemetry);
            String new_username_becuase_excel_think_it_is_a_date = username.replace("-",".");

            if ( username.equals("5-2") ) {
                /* Don't consider 5-2 */
                continue;
            }

//            double average_mse_over_skills = 0.0;
//            int num_levels_completed = 0;
            LinkedHashMap< String, Integer > skill_to_levels_used = new LinkedHashMap< String, Integer >();

            /* Average MSE for each skill for a given user over all levels */
            LinkedHashMap<String, Double> user_avg_mse_for_each_skill = new LinkedHashMap<String,Double>();

            analyzer.hardReset();

            /* If a player has definite knowledge about a given skill based on the set of hard-coded rules, it should persist over all levels */
            for (String level : telemetry_by_level.keySet()) {

                analyzer.resetRules();
//                LinkedHashMap<String,Double> rules = new LinkedHashMap<String,Double>();
//                for ( String s : analyzer.skill_map.keySet() ) {
//                    rules.put(s,0.0);
//                }

                analyzer.softReset();
                ArrayList<String> tel_by_level = telemetry_by_level.get(level);

                Pair< HashMap<Integer,PersistentData>, ArrayList<Integer> > persistent_data_info = getPersistentDataForLevel(tel_by_level, data_files);

                String first_log = tel_by_level.get(0);
                double start_time_ = Double.parseDouble(first_log.split("\t")[3]);

                String last_log = tel_by_level.get(tel_by_level.size() - 1);
                double final_time_ = Double.parseDouble(last_log.split("\t")[3]);

                double t1 = start_time_;
                double t2 = start_time_ + interval;

                if ( !analyzer.readSkillList(level) ) {
                    /* If level was not supposed to be played, then move to the next level */
                    continue;
                }

                //int num_instances = 0;
                while (t1 < final_time_) {
                    Pair<ArrayList<String>, ArrayList<Integer>> tel_in_interval = tel_utils.getDataInIntervals(tel_by_level, t1, t2, persistent_data_info.p2);

                    HashMap<String, Double> feature_vector = feat_extract.extractFeatureVector(path, tel_in_interval.p1, tel_in_interval.p2, persistent_data_info.p1, data_files, calibration);

                    /* If feature vector is empty, then either there's no telemetry in the interval OR we have insufficient data to create the feature vector */

                    ArrayList<String> slices = getSlices(username, t1, t2);
                    ArrayList<String> labels = new ArrayList<String>();
                    for (String slice : slices) {
                        String[] split_slice = slice.split(",");
                        labels.add(split_slice[2]);
                    }

                    Set<String> unique_values = new HashSet<String>(labels);
                    ArrayList<String> common = new ArrayList<String>();

                    t1 += interval / 2.0;
                    t2 = t1 + interval;

                    if ( feature_vector.size() == 0 ) {
                        continue;
                    }

                    int max_freq = 0;
                    for (String s : unique_values) {
                        int freq = Collections.frequency(labels, s);
                        if (freq > max_freq) {
                            max_freq = freq;
                            common.clear();
                            common.add(s);
                        } else {
                            if (freq == max_freq) {
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

                    switch ( update_technique_flag ) {
                        case 0:
                            /* Both Machine Learning and Rules */
                            double classification = classifyInstance(feature_vector, label);
                            analyzer.updateSkillsBasedOnClassification(instances.classAttribute().value((int) classification));
                            //analyzer.updateSkillsBasedOnRules( rules, rule_update_flag );
                            analyzer.updateSkillsBasedOnRules( rule_update_flag );
                            break;
                        case 1:
                            /* Only machine learning */
                            classification = classifyInstance(feature_vector, label);
                            analyzer.updateSkillsBasedOnClassification(instances.classAttribute().value((int) classification));
                            break;
                        case 2:
                            /* Only rules */
                            analyzer.updateSkillsBasedOnRules( rule_update_flag );
                            //analyzer.updateSkillsBasedOnRules( rules, rule_update_flag );
                            break;
                    }
                }

                //double mse = analyzer.evaluate(level, username);

                csv_skill_predictions.add(new_username_becuase_excel_think_it_is_a_date + "," + level + "," + analyzer.getPredictionsAsCSV());
                csv_ground_truth.add(new_username_becuase_excel_think_it_is_a_date + "," + level + "," + analyzer.getGroundTruthAsCSV(level, username));
                csv_mse.add(new_username_becuase_excel_think_it_is_a_date + "," + level + "," + analyzer.getMSEAsCSV(level, username));
                rules_skill_vector.add(new_username_becuase_excel_think_it_is_a_date + "," + level + "," + analyzer.getRulesAsCSV(analyzer.rules));

                /* MSE for each skills */
                LinkedHashMap<String, Double> tmp = analyzer.evaluateSkills(level, username);
//                boolean level_computable = false;

                for ( String s : tmp.keySet() ) {

                    /* Computation for overall */
                    if ( !overall_avg_mse_for_each_skill.containsKey(s) ) {
//                        if ( tmp.get(s) < 0 ) {
//                            overall_avg_mse_for_each_skill.put(s, -1.0);
//                        } else {
                        overall_avg_mse_for_each_skill.put(s, tmp.get(s));
//                        }
                    } else {
                        /* Don't consider this if tmp.get(s) < 0 */
                        if ( overall_avg_mse_for_each_skill.get(s) > -0.00000000000000000000000000001 ) {
                            /* There's a value associated with this skill. Keep adding */
                            if ( tmp.get(s) < 0 ) {
                                /* Skip value... */
                            } else {
                                double d = overall_avg_mse_for_each_skill.get(s);
                                overall_avg_mse_for_each_skill.replace(s, tmp.get(s) + d);
                            }
                        } else {
                            /* Replace ( if -1, then stays the same. If not, we get a new value */
                               overall_avg_mse_for_each_skill.replace(s, tmp.get(s));
                        }
                    }

                    /* Computation for user */
                    if ( !user_avg_mse_for_each_skill.containsKey(s) ) {
//                        if ( tmp.get(s) < 0  ) {
//                            user_avg_mse_for_each_skill.put(s,-1.0);
//                        } else {
                        user_avg_mse_for_each_skill.put(s, tmp.get(s));
//                        }
                    } else {
                        if ( user_avg_mse_for_each_skill.get(s) > -0.00000000000000000000000000001 ) {
                            /* There's a value associated with this skill. Keep adding */
                            if ( tmp.get(s) < 0 ) {
                                /* Skip value... */
                            } else {
                                double d = user_avg_mse_for_each_skill.get(s);
                                user_avg_mse_for_each_skill.replace(s, tmp.get(s) + d);
                            }
                        } else {
                            user_avg_mse_for_each_skill.replace(s, tmp.get(s));
                        }
                    }

                    if ( tmp.get(s) > -0.00000000000000000000000000001 ) {
                        /* This skill was used in the level (i.e. ground truth existed for it) */
                        if ( !skill_to_levels_used.containsKey(s) ) {
                            skill_to_levels_used.put(s,1);
                        } else {
                            int t = skill_to_levels_used.get(s);
                            skill_to_levels_used.replace(s,t+1);
                        }
                        if ( !skill_to_num_iterations.containsKey(s) ) {
                            skill_to_num_iterations.put(s,1);
                        } else {
                            int t = skill_to_num_iterations.get(s);
                            skill_to_num_iterations.replace(s,t+1);
                        }

                        if ( !level_to_skill_to_num_users.containsKey(level) ) {
                            LinkedHashMap<String,Integer> tmp2 = new LinkedHashMap<String,Integer>();
                            tmp2.put(s,1);
                            level_to_skill_to_num_users.put(level,tmp2);
                        } else {
                            if ( !level_to_skill_to_num_users.get(level).containsKey(s) ) {
                                LinkedHashMap<String,Integer> tmp2 = level_to_skill_to_num_users.get(level);
                                tmp2.put(s,1);
                                level_to_skill_to_num_users.replace(level,tmp2);
                            } else {
                                LinkedHashMap<String,Integer> tmp2 = level_to_skill_to_num_users.get(level);
                                int t = tmp2.get(s);
                                tmp2.replace(s,t+1);
                                level_to_skill_to_num_users.replace(level,tmp2);
                            }
                        }
                    }
                }

                if ( !level_average_mses_for_each_skill.containsKey(level) ) {
                    level_average_mses_for_each_skill.put(level,tmp);
                } else {
                    LinkedHashMap<String,Double> tmp2 =  level_average_mses_for_each_skill.get(level);
                    for ( String s : tmp2.keySet() ) {
                        if ( tmp2.get(s) > -0.00000000000000000000000000001 ) {
                            if (tmp.get(s) < 0) {
                                /* Ignore */
                            } else {
                                double d = tmp2.get(s);
                                tmp2.replace(s, d + tmp.get(s));
                            }
                        } else {
                            tmp2.replace(s, tmp.get(s));
                        }
                    }
//                    ArrayList<Double> tmp3 =  new ArrayList<Double>();
//                    for ( int i = 0; i < tmp2.size(); i++ ) {
//                        tmp3.add(tmp2.get(i) + skill_lst.get(i));
//                    }
                    level_average_mses_for_each_skill.replace(level,tmp2);
                }


                /* This needs to be based on the levels that were actually used in the computation */
//                if ( level_computable ) {
//                    /* Only want to count users that had data available */
//                    if (!level_to_user_count.containsKey(level)) {
//                        level_to_user_count.put(level, 1);
//                    } else {
//                        int tmp2 = level_to_user_count.get(level);
//                        level_to_user_count.replace(level, tmp2 + 1);
//                    }
//                    num_levels_completed++;
//                    num_iterations++;
//                }

//                average_mse_over_skills += mse;
//                overall_mse += mse;

//                if ( !level_to_num_skills.containsKey(level) ) {
//                    level_to_num_skills.put(level,analyzer.skills.size());
//                }
//
//                if ( !level_to_mse.containsKey(level) ) {
//                    level_to_mse.put(level,mse);
//                } else {
//                    double tmp2 = level_to_mse.get(level);
//                    level_to_mse.replace(level,tmp2 + 1);
//                }
            }

            /* Over all the levels for a given user...get the MSE for all skills */
            ArrayList<String> mse_for_skills_lst = new ArrayList<String>();
            for ( String s : user_avg_mse_for_each_skill.keySet() ) {
                if ( user_avg_mse_for_each_skill.get(s) < 0 ) {
                    mse_for_skills_lst.add("");
                } else {
                    mse_for_skills_lst.add(Double.toString(user_avg_mse_for_each_skill.get(s) / skill_to_levels_used.get(s) ));
                }
            }

            csv_average_mse_users.add(new_username_becuase_excel_think_it_is_a_date + ",level," + String.join(",",mse_for_skills_lst));
            //average_mses_over_skills.put(new_username_becuase_excel_sucks, average_mse_over_skills / num_levels_completed);
        }

        System.out.println("Skill vectors");
        System.out.println(String.join("\n",csv_skill_predictions));
        System.out.println("Ground Truth");
        System.out.println(String.join("\n",csv_ground_truth));
        System.out.println("Squared Error (SE)");
        System.out.println(String.join("\n",csv_mse));
        System.out.println("Average Squared Error over levels per user ( Sum of SE for all levels/# levels )");
        System.out.println(String.join("\n",csv_average_mse_users));
        System.out.println("Rules skill vector");
        System.out.println(String.join("\n",rules_skill_vector));

        System.out.println("Average Squared Error over users per level ( Sum of SE for all users that completed level/# users that completed level )");
        for ( String s : level_average_mses_for_each_skill.keySet() ) {
            LinkedHashMap<String, Double> tmp = level_average_mses_for_each_skill.get(s);
            ArrayList<String> tmp2 = new ArrayList<String>();
            //System.out.println(level_to_skill_to_num_users.get(s));
            for ( String skill : tmp.keySet() ) {
                if ( tmp.get(skill) < 0 ) {
                    tmp2.add("");
                } else {
                    tmp2.add(Double.toString(tmp.get(skill) / level_to_skill_to_num_users.get(s).get(skill)));
                }
            }
            System.out.println("user," + s + "," + String.join(",",tmp2) );
        }

        /* Over all the users and levels...get the MSE for a given skill */
        System.out.println("Average Squared Error over all levels and users ( Sum of SE for all users and levels/(users*levels) )");
        System.out.print("user,level,");
        ArrayList<String> mse_for_skills_lst = new ArrayList<String>();
        for ( String s : overall_avg_mse_for_each_skill.keySet() ) {
            if ( overall_avg_mse_for_each_skill.get(s) < 0 ) {
                mse_for_skills_lst.add("");
            } else {
                mse_for_skills_lst.add( Double.toString(overall_avg_mse_for_each_skill.get(s) / skill_to_num_iterations.get(s) ) );
            }
        }
        System.out.println(String.join(",",mse_for_skills_lst));
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

    public Pair< HashMap<Integer,PersistentData>, ArrayList<Integer> > getPersistentDataForLevel( ArrayList<String> telemetry,
                                                                                                  HashMap<String, ArrayList<String> > data_files) {
        /* From the level...find the next TriggerLoadLevel that contains a non-empty string that doesn't start with "l". Update persistent data */
        HashMap<Integer,PersistentData> all_persistent_data = new HashMap<Integer, PersistentData>();
        ArrayList<Integer> index_vector = new ArrayList<Integer>();

        String [] data = telemetry.get(0).split("\t"); String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        Gson gson = new Gson();

        HashMap<String,Object> goal_struct;
        HashMap<Integer,Integer> id_to_num_delivered = new HashMap<Integer,Integer>();
        HashMap<Integer,Integer> id_to_num_missed = new HashMap<Integer,Integer>();
        HashMap<Integer,Integer> id_to_num_delivered_required = new HashMap<Integer,Integer>();
        HashMap<Integer,String> id_to_condition = new HashMap<Integer,String>();

        HashMap<String, Pair<Integer,Integer> > semaphore_locations = new HashMap<String, Pair<Integer,Integer> >();
        HashMap<String, String > semaphore_tracks = new HashMap<String, String >();

        /* Level to critical section */
        ArrayList< ArrayList<String> > critical_section = new ArrayList< ArrayList<String> >();
        ArrayList< Pair<Integer,Integer> > critical_section_buttons = new ArrayList< Pair<Integer,Integer> >();
        ArrayList< Pair<Integer,Integer> > critcal_section_semaphore = new ArrayList< Pair<Integer,Integer> >();


        /* Time in seconds */
        long s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );
        int incr = 0;
        for ( String tel : telemetry ) {
            data = tel.split("\t");
            String name_ = data[1]; String data_ = data[2];

            if (name_.equals("TriggerLoadLevel")) {
                if (data_.length() != 0) {
                    if (data_.startsWith("l")) {
                        /* Get the critical section for this level */
                        try {
                            BufferedReader br = new BufferedReader(new FileReader( "critical-section-annotations/" + data_));
                            String line = "";
                            int num_lines = 0;
                            int board_width = 0, board_height = 0;
                            while ((line = br.readLine()) != null) {
                                if (line.startsWith("board_width")) {
                                    board_width = Integer.parseInt(line.split("\t")[1]);
                                }

                                if (line.startsWith("board_height")) {
                                    board_height = Integer.parseInt(line.split("\t")[1]);
                                }

                                if ( line.startsWith("CRITICALSECTIONS") ) {
                                    for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                        String tmp = br.readLine();
                                        ArrayList<String> row = new ArrayList<String>();
                                        for ( int m = 0; m < tmp.length(); m++ ) {
                                            if ( String.valueOf(tmp.charAt(m)).equals("B") ) {
                                                critical_section_buttons.add(new Pair<Integer,Integer>(i-(num_lines+1),m));
                                            }
                                            if ( String.valueOf(tmp.charAt(m)).equals("S") ) {
                                                critcal_section_semaphore.add(new Pair<Integer,Integer>(i-(num_lines+1),m));
                                            }
                                            row.add(String.valueOf(tmp.charAt(m)));
                                        }
                                        critical_section.add(row);
                                    }
                                    break;
                                }
                                num_lines++;
                            }
                            br.close();
                        } catch ( Exception e ) {
                            e.printStackTrace();
                        }
                    } else {
                        /* NOTE: This is actually what is sent by the ME for execution */
                        if ( data_files.containsKey(data_) ) {

                            /* Create a feature vector if we have level data */
                            PersistentData persistent_data = new PersistentData();
                            persistent_data.persistent_data.put("start_time", s_);

                            ArrayList<String> component_information = new ArrayList<String>();
                            ArrayList<String> diverter_ids = new ArrayList<String>();
                            ArrayList<String> semaphore_ids = new ArrayList<String>();
                            ArrayList<String> all_semaphore_ids = new ArrayList<String>();

                            ArrayList<String> button_ids = new ArrayList<String>();
                            ArrayList<String> delivery_ids = new ArrayList<String>();
                            ArrayList<String> pickup_ids = new ArrayList<String>();

                            HashMap<String,String> button_to_semaphore = new HashMap<String,String>();
                            HashMap<String, ArrayList<String>> semaphore_to_button = new HashMap<String,ArrayList<String>>();

                            /*  Every time the game sends game state to the ME, check if any of the semaphores were not moved in any insignificant way
                            *   Insignificant is defined as moving within +1 or -1.
                            * */

                            try {
                                BufferedReader br = new BufferedReader(new FileReader(path + "/data/" + data_files.get(data_).get(0)));
                                String line = "";
                                int num_lines = 0;
                                int board_width = 0, board_height = 0;
                                boolean start_parse = false;
                                boolean looking_at_execution = false;
                                while ((line = br.readLine()) != null) {
                                    if (line.startsWith("board_width")) {
                                        board_width = Integer.parseInt(line.split("\t")[1]);
                                        persistent_data.persistent_data.put("width",board_width);
                                    }

                                    if (line.startsWith("board_height")) {
                                        board_height = Integer.parseInt(line.split("\t")[1]);
                                        persistent_data.persistent_data.put("height",board_height);
                                    }

                                    if ( line.startsWith("goal_struct") ) {
                                        goal_struct = gson.fromJson(line.split("\t")[1],HashMap.class);
                                        /* For each required goal, check the number of packages each delivery point requires */
                                        ArrayList< LinkedTreeMap<String,Object>> required = (ArrayList< LinkedTreeMap<String,Object> >)goal_struct.get("required");
                                        for ( LinkedTreeMap<String,Object> cond : required ) {

                                            /* NOTE: We may need to consider other types of goals */
                                            if ( !((String)cond.get("type")).equals("delivery") ) {
                                                continue;
                                            }
                                            int id = ((Double)cond.get("id")).intValue();
                                            int component_value = ((Double)cond.get("value")).intValue();
                                            String condition = (String)cond.get("condition");

                                            /* Set up id -> num_delivered mapping */
                                            id_to_num_delivered.put(id,0);
                                            id_to_num_missed.put(id,0);
                                            id_to_num_delivered_required.put(id,component_value);
                                            id_to_condition.put(id,condition);
                                        }
                                        //persistent_data.persistent_data.replace("goal_struct",goal_struct);
                                    }

                                    if ( line.startsWith("EXECUTION") ) {
                                        looking_at_execution = true;
                                        continue;
                                    }

                                    if ( looking_at_execution && line.split("\t").length > 2 ) {
                                        //System.out.println("Line: " + line);
                                        String id = line.split("\t")[2];
                                        if ( diverter_ids.contains(id) ) {
                                            analyzer.updateRules("Use diverters");
//                                            switch ( rule_update_flag ) {
//                                                case 0:
//                                                    rules.replace("Use diverters", rules.get("Use diverters") + 1.0);
//                                                    break;
//                                                case 1:
//                                                    rules.replace("Use diverters",1.0);
//                                                    break;
//                                            }
                                        }
                                    }

                                    if ( looking_at_execution && (line.split("\t")).length > 5 && (line.split("\t")[5]).contains("final_condition") ) {
                                        String info = line.split("\t")[5];
                                        HashMap<String, Double > final_condition = gson.fromJson(info,HashMap.class);

                                        /* TODO: Check whether these are reliable ways to detect starvation */
                                        if ( (final_condition.get("final_condition").intValue() & GameState.RESULT_PROBLEMATIC_STARVATION) == 0 ) {
                                            /* Starvation detected */
                                            analyzer.updateRules("Prevent starvation");
//                                            switch ( rule_update_flag ) {
//                                                case 0:
//                                                    rules.replace("Prevent starvation", rules.get("Prevent starvation") + 1.0);
//                                                    break;
//                                                case 1:
//                                                    rules.replace("Prevent starvation",1.0);
//                                                    break;
//                                            }
                                        }

                                        if ( (final_condition.get("final_condition").intValue() & GameState.RESULT_PROBLEMATIC_INFINITE_LOOP) == 0 ) {
                                            /* Starvation detected */
                                            analyzer.updateRules("Prevent starvation");
//                                            switch ( rule_update_flag ) {
//                                                case 0:
//                                                    rules.replace("Prevent starvation", rules.get("Prevent starvation") + 1.0);
//                                                    break;
//                                                case 1:
//                                                    rules.replace("Prevent starvation",1.0);
//                                                    break;
//                                            }
                                        }
                                    }

                                    if ( looking_at_execution && line.split("\t")[0].equals("D") ) {
                                        /* Execution denotes a package was delivered */
                                        HashMap<String, Object> tmp = gson.fromJson(line.split("\t")[5],HashMap.class);
                                        /* Structure example: {"missed_items":[],"delivered_items":[2002],"delivered_to":3001} */
                                        /* NOTE: Can also be: {exchange_between_b=1004.0, exchange_between_a=1001.0} */
                                        if ( !tmp.containsKey("delivered_items") ) {
                                            /* This is an exchange point */
                                            if ( tmp.containsKey("exchange_between_b") ) {
                                                int a = ((Double)tmp.get("exchange_between_a")).intValue();
                                                int b = ((Double)tmp.get("exchange_between_b")).intValue();
                                                analyzer.updateRules("Understand exchange points");
//                                                switch ( rule_update_flag ) {
//                                                    case 0:
//                                                        rules.replace("Understand exchange points", rules.get("Understand exchange points") + 1.0);
//                                                        break;
//                                                    case 1:
////                                                        rules.replace("Understand exchange points", rules.get("Understand exchange points") + 1.0);
//                                                        rules.replace("Understand exchange points",1.0);
//                                                        break;
//                                                }
                                            } else {
                                                continue;
                                            }
                                        } else {
                                            ArrayList<Integer> delivered = (ArrayList<Integer>)tmp.get("delivered_items");
                                            ArrayList<Integer> missed = (ArrayList<Integer>) tmp.get("delivered_items");
                                            //System.out.println("Delivered: " + tmp.toString());
                                            int id = ((Double) tmp.get("delivered_to")).intValue();
                                            /* NOTE: I'm a bit skeptical about how to get the missed packages */
                                            id_to_num_missed.replace(id, id_to_num_missed.get(id) + missed.size());
                                            id_to_num_delivered.replace(id, id_to_num_delivered.get(id) + delivered.size());
                                            //goals_completed += tmp.get("goals_completed");
                                        }
                                    }

                                    if (line.startsWith("DIRECTIONS")) {
                                        ArrayList< ArrayList<String> > board = (ArrayList< ArrayList<String> >) persistent_data.persistent_data.get("direction_layout");

                                        for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                        //for (int i = incr + 1; i < incr + board_height + 1; i++) {
                                            String tmp = br.readLine();
                                            ArrayList<String> row = new ArrayList<String>();
                                            for ( int m = 0; m < tmp.length(); m++ ) {
                                                row.add(String.valueOf(tmp.charAt(m)));
                                            }
                                            board.add(row);
                                        }
                                        persistent_data.persistent_data.replace("direction_layout", board);
                                    }

                                    if (line.startsWith("COLORS")) {
                                        ArrayList< ArrayList<String> > color = (ArrayList< ArrayList<String> >) persistent_data.persistent_data.get("color_layout");
                                        for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                        //for (int i = incr + 1; i < incr + board_height + 1; i++) {
                                            String tmp = br.readLine();
                                            ArrayList<String> row = new ArrayList<String>();
                                            for ( int m = 0; m < tmp.length(); m++ ) {
                                                row.add(String.valueOf(tmp.charAt(m)));
                                            }
                                            color.add(row);
                                        }
                                        persistent_data.persistent_data.replace("color_layout", color);
                                    }

                                    if (line.startsWith("COMPONENTS")) {
                                        start_parse = true;
                                    } else {
                                        if (start_parse) {
                                            if (line.length() == 0) {
                                                start_parse = false;
                                            } else {
                                                String[] comp_info = line.split("\t");

                                                /* Component -> x,y coordinate on board */
                                                if ( persistent_data.persistent_data.containsKey("comp_loc_map") ) {
                                                    HashMap<String, ArrayList<Integer> > comp_loc_map =  (HashMap<String, ArrayList<Integer> >)persistent_data.persistent_data.get("comp_loc_map");
                                                    ArrayList<Integer> tmp = new ArrayList<Integer>();
                                                    tmp.add(Integer.parseInt(comp_info[2])); tmp.add(Integer.parseInt(comp_info[3]));
                                                    //System.out.println("Component: " + comp_info[0] + "," + data_files.get(data_).get(0));
                                                    comp_loc_map.put(comp_info[0],tmp);
                                                    persistent_data.persistent_data.replace("comp_loc_map", comp_loc_map);
                                                }

                                                /* Actual information about the component is located at index 6 */
                                                HashMap<String, Object> info = feat_extract.parseComponentInformation(comp_info[6]);

                                                component_information.add(line);
                                                for ( String c_info : component_information ) {
                                                    String [] tmp = c_info.split("\t");
                                                    if ( tmp[1].equals("diverter") ) {
                                                        diverter_ids.add(tmp[0]);
                                                    }

                                                    if ( tmp[1].equals("delivery") ) {
                                                        delivery_ids.add(tmp[0]);
                                                    }

                                                    if ( tmp[1].equals("pickup") ) {
                                                        pickup_ids.add(tmp[0]);
                                                    }

                                                    if ( tmp[1].equals("semaphore") ) {
                                                        all_semaphore_ids.add(tmp[0]);
                                                        if ( !semaphore_locations.containsKey(tmp[0]) ) {
                                                            semaphore_locations.put(tmp[0],new Pair<Integer,Integer>(Integer.parseInt(comp_info[2]),Integer.parseInt(comp_info[3])));
                                                            semaphore_tracks.put(tmp[0],String.valueOf(info.get("color")));
                                                        } else {
                                                            /* Check whether this has moved insignificantly (TODO: I'm not sure if this is right as we haven't defined insignifiant exactly) */
                                                            Pair<Integer,Integer> prev_coord = semaphore_locations.get(tmp[0]);
                                                            Pair<Integer,Integer> cur_coord = new Pair<Integer,Integer>(Integer.parseInt(comp_info[2]),Integer.parseInt(comp_info[3]));
                                                            /* If semaphore has been moved to a different track, remained in position, and moved */
                                                            if ( semaphore_tracks.get(tmp[0]).equals(String.valueOf(info.get("color"))) ) {
                                                                /* Check whether the object has been moved by + or -1 in any direction */
                                                                if ( (prev_coord.p1+1 != cur_coord.p1) && (prev_coord.p1-1 != cur_coord.p1) &&
                                                                    (prev_coord.p2+1 != cur_coord.p2) && (prev_coord.p2-1 != cur_coord.p2)
                                                                        && !(prev_coord.equals(cur_coord)) ) {
                                                                    /* Not moved + or -1 in any direction, are on the same track, and has at least changed. */
                                                                    //System.out.println("Understanding unpredicatable rates");
                                                                    analyzer.updateRules("Understand that arrows move at unpredictable rates");
//                                                                    switch ( rule_update_flag ) {
//                                                                        case 0:
//                                                                            rules.replace("Understand that arrows move at unpredictable rates", rules.get("Understand that arrows move at unpredictable rates") + 1.0);
//                                                                            break;
//                                                                        case 1:
//                                                                            rules.replace("Understand that arrows move at unpredictable rates",1.0);
//                                                                            break;
//                                                                    }
                                                                }
                                                            } else {
                                                                semaphore_tracks.replace(tmp[0],String.valueOf(info.get("color")));
                                                            }
                                                            semaphore_locations.replace(tmp[0],cur_coord);
                                                        }
                                                    }

                                                    if ( tmp[1].equals("signal") ) {
                                                        //System.out.println("data_: " + data_files.get(data_).get(0));
                                                        if ( info.containsKey("link") && ((Double)info.get("link")).intValue() != -1 ) {
                                                            /* NOTE: Can one semaphore be connected to two different locks? */
                                                            button_to_semaphore.put(tmp[0],Integer.toString(((Double) info.get("link")).intValue()));
                                                            if ( semaphore_to_button.containsKey(Integer.toString(((Double) info.get("link")).intValue())) ) {
                                                                ArrayList<String> buttons = semaphore_to_button.get(Integer.toString(((Double) info.get("link")).intValue()));
                                                                buttons.add(tmp[0]);
                                                                semaphore_to_button.replace(Integer.toString(((Double) info.get("link")).intValue()),buttons);
                                                            } else {
                                                                ArrayList<String> buttons = new ArrayList<String>();
                                                                buttons.add(tmp[0]);
                                                                semaphore_to_button.put(Integer.toString(((Double) info.get("link")).intValue()), buttons);
                                                            }
                                                            semaphore_ids.add(Integer.toString(((Double) info.get("link")).intValue()));

                                                            button_ids.add(tmp[0]);
                                                        }
                                                    }
                                                }

                                                if (info.containsKey("color")) {
                                                    //System.out.println("Should be here: " + comp_info[0]);
                                                    if (persistent_data.persistent_data.containsKey("comp_color_map")) {
                                                        HashMap<String, String> color_map = (HashMap<String, String>)persistent_data.persistent_data.get("comp_color_map");
                                                        color_map.put(comp_info[0], Integer.toString(((Double) info.get("color")).intValue()));
                                                        persistent_data.persistent_data.replace("comp_color_map", color_map);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    num_lines++;
                                }
                                br.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            HashMap<String, String> color_map = (HashMap<String, String>)persistent_data.persistent_data.get("comp_color_map");
                            HashMap<String,Integer> num_semaphores_unlinked_per_track = new HashMap<String,Integer>();
                            for ( String sem_id : all_semaphore_ids ) {
                                String sem_id_color = color_map.get(sem_id);
                                if ( !semaphore_ids.contains(sem_id) ) {
                                    if ( num_semaphores_unlinked_per_track.containsKey(sem_id_color) ) {
                                        num_semaphores_unlinked_per_track.replace(sem_id_color,num_semaphores_unlinked_per_track.get(sem_id_color)+1);
                                    } else {
                                        num_semaphores_unlinked_per_track.put(sem_id_color,1);
                                    }
                                }
                            }

                            for ( String track : num_semaphores_unlinked_per_track.keySet() ) {
                                if ( num_semaphores_unlinked_per_track.get(track) < 2 ) {
                                    analyzer.updateRules("Understand that arrows move at unpredictable rates");
//                                    switch ( rule_update_flag ) {
//                                        case 0:
//                                            rules.replace("Understand that arrows move at unpredictable rates", rules.get("Understand that arrows move at unpredictable rates") + 1.0);
//                                            break;
//                                        case 1:
//                                            rules.replace("Understand that arrows move at unpredictable rates",1.0);
//                                            break;
//                                    }
                                }
                            }

                            /* Check "Alternating access with semaphores and locks (ensure mutual exclusion)" rule here ( TODO: Determine why this is giving worse results )
                            *   Maybe we need to consider the color of track...
                            * */
                            for ( String s : button_to_semaphore.keySet() ) {
                                HashMap<String, ArrayList<Integer> > comp_loc_map = (HashMap<String, ArrayList<Integer> >)persistent_data.persistent_data.get("comp_loc_map");
//                                System.out.println("Button coordinate: " + s + "," + button_to_semaphore.get(s));
                                ArrayList<Integer> button_coord = comp_loc_map.get(s);
                                ArrayList<Integer> semaphore_coord = comp_loc_map.get(button_to_semaphore.get(s));

                                if ( semaphore_coord == null ) {
                                    /* Linking to a non-existant semaphore */
                                    continue;
                                }

                                int button_x = button_coord.get(0);
                                int button_y = button_coord.get(1);
                                int semaphore_x = semaphore_coord.get(0);
                                int semaphore_y = semaphore_coord.get(1);

                                Pair<Integer,Integer> button = new Pair<Integer,Integer>(button_x,button_y);
                                Pair<Integer,Integer> semaphore = new Pair<Integer,Integer>(semaphore_x,semaphore_y);

                                if ( critcal_section_semaphore.contains(semaphore) && critical_section_buttons.contains(button) ) {
                                    analyzer.updateRules("Block critical sections");
//                                    switch ( rule_update_flag ) {
//                                        case 0:
//                                            rules.replace("Block critical sections", rules.get("Block critical sections" + 1.0));
//                                            break;
//                                        case 1:
//                                            rules.replace("Block critical sections",1.0);
//                                            break;
//                                    }
                                }

                                /* Get color of each...if the colors are different, don't consider them? */

                                /* Path starts from semaphore..and goes to signal */

                                ArrayList< ArrayList<String> > board = (ArrayList< ArrayList<String> >) persistent_data.persistent_data.get("direction_layout");
                                ArrayList< Pair<Integer,Integer> > open = new ArrayList< Pair<Integer,Integer> >();
                                ArrayList< Integer > num_pairs_in_path_list = new ArrayList< Integer >();
                                open.add(new Pair<Integer,Integer>(semaphore_x,semaphore_y));
                                num_pairs_in_path_list.add(0);

                                ArrayList< Pair<Integer,Integer> > path = new ArrayList< Pair<Integer,Integer> >();
                                path.add(new Pair<Integer,Integer>(semaphore_x,semaphore_y));
                                ArrayList< Pair<Integer,Integer> > closed = new ArrayList< Pair<Integer,Integer> >();

                                int num_pairs_in_path = 0;  /* Number of delivery, pickup pairs in path */

                                /* NOTE: I didn't think of every possibility here, so there's a chance that one scenario isn't detected */
                                while ( !open.isEmpty() ) {
                                    Pair<Integer,Integer> coord = open.remove(open.size()-1);
                                    Integer num_pairs = num_pairs_in_path_list.remove(num_pairs_in_path_list.size()-1);
                                    closed.add(coord);

                                    if ( coord.p1 == button_x && coord.p2 == button_y ) {

                                        /* End of the search */
                                        if ( num_pairs_in_path == 0 ) {
                                            /* Add +1 evidence */
                                            //System.out.println("Detected mutual exclusion");
                                            analyzer.updateRules("Alternating access with semaphores and locks (ensure mutual exclusion)");
//                                            switch ( rule_update_flag ) {
//                                                case 0:
//                                                    rules.replace("Alternating access with semaphores and locks (ensure mutual exclusion)", rules.get("Alternating access with semaphores and locks (ensure mutual exclusion)") + 1.0);
//                                                    break;
//                                                case 1:
//                                                    rules.replace("Alternating access with semaphores and locks (ensure mutual exclusion)",1.0);
//                                                    break;
//                                            }
                                        }
                                        /* If this has been done at least once...then we know they have evidence that they understand mutual exclusion */
                                        break;
                                    }
                                    if ( !board.get(coord.p2).get(coord.p1).equals("") ) {
                                        String direction = board.get(coord.p2).get(coord.p1);

                                        /* It's possible to have 2 pickups and 2 deliveries in the same path? Even if it doesn't execute correctly, the student
                                         * seems to understand mutual exclusion. Even if the delivery doesn't accept the package, as long
                                         * as we see mutual exclusion, that's fine. */

                                        /* Check to see if a delivery point and a pickup are in the path */

                                        boolean change_paths = false;
                                        for ( String d_ids : delivery_ids ) {
                                            ArrayList<Integer> tmp = comp_loc_map.get(d_ids);
                                            /* Check if delivery accepts the detected pickup */
                                            if ( tmp.get(0) == coord.p1 && tmp.get(1) == coord.p2 ) {
                                                if ( num_pairs_in_path > 0 ) {
                                                    num_pairs_in_path--;
                                                    break;
                                                } else {
                                                    /* Pickup was not detected in this path... */
                                                    change_paths = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if ( change_paths ) {
                                            continue;
                                        }

                                        for ( String d_ids : pickup_ids ) {
                                            ArrayList<Integer> tmp = comp_loc_map.get(d_ids);
                                            if (tmp.get(0) == coord.p1 && tmp.get(1) == coord.p2) {
                                                if ( num_pairs_in_path >= 0 ) {
                                                    num_pairs_in_path++;
                                                    break;
                                                } else {
                                                    /* Pickup was detected before delivery...change paths */
                                                    change_paths = true;
                                                    break;
                                                }
                                            }
                                        }

                                        if ( change_paths ) {
                                            continue;
                                        }

                                        boolean coordinates_added = false;
                                        //System.out.println("x,y1: " + coord.p1 + "," + coord.p2 + "," + direction);
                                        switch ( direction ) {
                                            case "<":
                                                /* Only left */
                                                if ( coord.p1-1 >= 0 ) {
                                                    if ( !board.get(coord.p2).get(coord.p1-1).equals("") ) {
                                                        Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1-1,coord.p2);
                                                        if ( !closed.contains(tmp) ) {
                                                            open.add(tmp);
                                                            coordinates_added = true;
                                                            num_pairs_in_path_list.add(num_pairs_in_path);
                                                        }
                                                    }
                                                }
                                                break;
                                            case ">":
                                                /* Only right */
                                                if ( coord.p1+1 < board.get(0).size() ) {
                                                    if ( !board.get(coord.p2).get(coord.p1+1).equals("") ) {
                                                        Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1+1,coord.p2);
                                                        if ( !closed.contains(tmp) ) {
                                                            open.add(tmp);
                                                            coordinates_added = true;
                                                            num_pairs_in_path_list.add(num_pairs_in_path);
                                                        }
                                                    }
                                                }
                                                break;
                                            case "A":
                                                /* Only up */
                                                if ( coord.p2-1 >= 0 ) {
                                                    if ( !board.get(coord.p2-1).get(coord.p1).equals("") ) {
                                                        Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1,coord.p2-1);
                                                        if ( !closed.contains(tmp) ) {
                                                            open.add(tmp);
                                                            coordinates_added = true;
                                                            num_pairs_in_path_list.add(num_pairs_in_path);
                                                        }
                                                    }
                                                }
                                                break;
                                            case "V":
                                                /* Only down */
                                                if ( coord.p2+1 < board.size() ) {
                                                    Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1,coord.p2+1);
                                                    if ( !closed.contains(tmp) ) {
                                                        open.add(tmp);
                                                        coordinates_added = true;
                                                        num_pairs_in_path_list.add(num_pairs_in_path);
                                                    }
                                                }
                                                break;
                                            case "X":
                                                /* All 4 directions */
                                                if ( coord.p1-1 >= 0 ) {
                                                    if ( !board.get(coord.p2).get(coord.p1-1).equals("") ) {
                                                        Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1-1,coord.p2);
                                                        if ( !closed.contains(tmp) ) {
                                                            open.add(tmp);
                                                            coordinates_added = true;
                                                            num_pairs_in_path_list.add(num_pairs_in_path);
                                                        }
                                                    }
                                                }
                                                if ( coord.p1+1 < board.get(0).size() ) {
                                                    if ( !board.get(coord.p2).get(coord.p1+1).equals("") ) {
                                                        Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1+1,coord.p2);
                                                        if ( !closed.contains(tmp) ) {
                                                            open.add(tmp);
                                                            coordinates_added = true;
                                                            num_pairs_in_path_list.add(num_pairs_in_path);
                                                        }
                                                    }
                                                }
                                                if ( coord.p2-1 >= 0 ) {
                                                    if ( !board.get(coord.p2-1).get(coord.p1).equals("") ) {
                                                        Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1,coord.p2-1);
                                                        if ( !closed.contains(tmp) ) {
                                                            open.add(tmp);
                                                            coordinates_added = true;
                                                            num_pairs_in_path_list.add(num_pairs_in_path);
                                                        }
                                                    }
                                                }
                                                if ( coord.p2+1 < board.size() ) {
                                                    Pair<Integer,Integer> tmp = new Pair<Integer,Integer>(coord.p1,coord.p2+1);
                                                    if ( !closed.contains(tmp) ) {
                                                        open.add(tmp);
                                                        coordinates_added = true;
                                                        num_pairs_in_path_list.add(num_pairs_in_path);
                                                    }
                                                }
                                                break;
                                        }
                                        if ( !coordinates_added ) {
                                            /* Reached the end of the path... */
                                            num_pairs_in_path = num_pairs;
                                        }

                                    }
                                }
                            }

                            for ( String button_id : button_to_semaphore.keySet() ) {
                                //HashMap<String, String> color_map = (HashMap<String, String>)persistent_data.persistent_data.get("comp_color_map");
//                                System.out.println("Button ID: " + button_id);
//                                System.out.println(color_map.toString());
                                String button1_color = color_map.get(button_id);
                                /* Check the color of the linked semaphore */
                                String sem2_color = color_map.get(button_to_semaphore.get(button_id));

                                /* Make sure these colors are different */
                                if ( button1_color.equals(sem2_color) ) {
                                    continue;
                                }

                                /* Check if a semaphore exists on lock1_color, and check what lock it's connected to.  */
                                for ( String comp_id : color_map.keySet() ) {
                                    /* If one the same track as lock1, and is a semaphore */
                                    if ( color_map.get(comp_id).equals(button1_color) && semaphore_ids.contains(comp_id) ) {
                                        //String sem1_color = color_map.get(comp_id);
                                        System.out.println(semaphore_to_button.toString() + " " + comp_id);
                                        for ( String button : semaphore_to_button.get(comp_id) ) {
                                            String button2_color = color_map.get(button);
                                            /* Check if lock2's color is the same as sem2's color, and check if lock2 isn't the same color as sem1 */
                                            if ( button2_color.equals(sem2_color) ) {
                                                analyzer.updateRules("Synchronized multiple arrows");
//                                                switch ( rule_update_flag ) {
//                                                    case 0:
//                                                        rules.replace("Synchronized multiple arrows", rules.get("Synchronized multiple arrows") + 1.0);
//                                                        break;
//                                                    case 1:
//                                                        rules.replace("Synchronized multiple arrows",1.0);
//                                                        break;
//                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            index_vector.add(incr);
                            all_persistent_data.put(incr,persistent_data);
                            incr++;
                            continue;
                        }
                    }
                }
            }
            index_vector.add(incr);
        }

        /* NOTE: Is this computed for each file sent over to the ME or all fles? */

        int total_missed = 0;
        boolean delivered_all_packages = true;
        for ( Integer id : id_to_num_delivered_required.keySet() ) {
            String condition = id_to_condition.get(id);
            int value = id_to_num_delivered.get(id);
            int component_value = id_to_num_delivered_required.get(id);
            int missed = id_to_num_missed.get(id);
            total_missed += missed;

            if ("eq".equals(condition) && !(value == component_value)) {
                delivered_all_packages = false;
                break;
            } else if ("lt".equals(condition) && !(value < component_value)) {
                delivered_all_packages = false;
                break;
            } else if ("gt".equals(condition) && !(value > component_value)) {
                delivered_all_packages = false;
                break;
            } else if ("ne".equals(condition) && !(value != component_value)) {
                delivered_all_packages = false;
                break;
            }
        }
        if ( delivered_all_packages ) {
            //System.out.println("You successfully delivered all the packages!");
            analyzer.updateRules("Deliver packages");
//            switch ( rule_update_flag ) {
//                case 0:
//                    rules.replace("Deliver packages", rules.get("Deliver packages") + 1.0);
//                    break;
//                case 1:
//                    rules.replace("Deliver packages",1.0);
//                    break;
//            }
        }

        if ( total_missed == 0 ) {
            //System.out.println("You successfully didn't miss any packages!");
            analyzer.updateRules("Understand specific delivery points");
//            switch ( rule_update_flag ) {
//                case 0:
//                    rules.replace("Understand specific delivery points", rules.get("Understand specific delivery points") + 1.0);
//                    break;
//                case 1:
//                    rules.replace("Understand specific delivery points", 1.0);
////                    rules.replace("Understand specific delivery points", rules.get("Understand specific delivery points") + 1.0);
//                    break;
//            }
        }

        return new Pair< HashMap<Integer,PersistentData>, ArrayList<Integer> >(all_persistent_data, index_vector);
    }

}

//    public void simulate() {
//
//        /* Average mean-squared errors (MSE) for each student over all levels */
//        HashMap<String, Double> average_mses = new HashMap<String, Double>();
//
//        /* Get all the players in the log files */
//        HashMap< String, ArrayList<String> > players = tel_utils.getPlayers();
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
//            getTrainingDataset(train_files);
//
//            try {
//                classifier.buildClassifier(instances);
//                //eval = new Evaluation(instances);
//            } catch ( Exception e ) {
//                e.printStackTrace();
//            }
//
//            /* Generate feature vector */
//            HashMap<String, ArrayList<String> > data_files = tel_utils.getLevelData( path + "/data/" );
//
//            File f = new File(path + "/log/" + players.get(player).get(0));
//            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());
//
//            double t1 = 0;
//            double t2 = interval;
//
//            HashMap<String, Object > persistent_data = new HashMap<String,Object>();
//            /* Initialize persistent data */
//
//            persistent_data.put("cur_track", -1);
//            persistent_data.put("cur_mouse_comp", "");
//            persistent_data.put("cur_mouse_time", 0.0);
//            persistent_data.put("linking", false);
//            persistent_data.put("filename","");
//
//            persistent_data.put("direction_layout",new ArrayList<String>());
//            persistent_data.put("color_layout",new ArrayList<String>());
//            persistent_data.put("comp_link_map",new HashMap<String, String >());
//            persistent_data.put("comp_color_map",new HashMap<String, String >());
//
//            /* Find the last possible time */
//            String last_log = telemetry.get(telemetry.size()-1);
//            double final_time_ = Double.parseDouble(last_log.split("\t")[3]);
//            String username = getUser(telemetry);
//            cur_level = "";
//
//            double average_mse = 0.0;
//            int num_levels_completed = 0;
//
//            while ( t1 < final_time_ ) {
//                ArrayList<String> tmp = tel_utils.getDataInIntervals(telemetry, t1, t2);
////                String print_statement = String.format("Printing telemetry data at interval %f to %f for %s",t1,t2,f.getName());
////                System.out.println(print_statement);
////                System.out.println("---------------------------------------");
////                for ( String s : tmp.p1 ) {
////                    System.out.println' (s);
////                }
////                System.out.println("---------------------------------------");
//                HashMap<String, Double> feature_vector = feat_extract.extractFeatureVector(path, tmp, persistent_data, data_files);
//                String level = (String)persistent_data.get("filename");
//
//                if ( !cur_level.equals(level) && level.startsWith("l") ) {
//                    // For now, only consider non-PCG levels...
//
//                    if ( cur_level.length() != 0 ) {
//                        /* Evaulate */
//                        average_mse += analyzer.evaluate(cur_level, username);
//                        num_levels_completed ++;
//                    }
//
//                    cur_level = level;
//
//                    analyzer.reset();
//                    System.out.println("Starting a new level " + cur_level + " for user " + username);
//
//                    analyzer.readSkillList(cur_level);
//                }
//
//                ArrayList<String> slices = getSlices(username,t1,t2);
//                ArrayList<String> labels = new ArrayList<String>();
//                for ( String slice : slices ) {
//                    String [] split_slice = slice.split(",");
//                    labels.add(split_slice[2]);
//                }
//
//                Set<String> unique_values = new HashSet<String>(labels);
//                ArrayList<String> common = new ArrayList<String>();
//
//                t1 += interval / 2.0;
//                t2 = t1 + interval;
//
//                int max_freq = 0;
//                for ( String s : unique_values ) {
//                    int freq = Collections.frequency(labels,s);
//                    if ( freq >  max_freq ) {
//                        max_freq = freq;
//                        common.clear();
//                        common.add(s);
//                    } else {
//                        if ( freq == max_freq ) {
//                            /* Check the ranking */
//                            common.add(s);
//                        }
//                    }
//                }
//
//                String label = "";
//                if ( common.size() == 0 ) {
//                    continue;
//                } else if ( common.size() == 1 ) {
//                    label = common.get(0);
//                } else {
//                    if ( common.contains("C") ) {
//                        label = "C";
//                    } else if (common.contains("B")) {
//                        label = "B";
//                    } else if (common.contains("A")) {
//                        label = "A";
//                    }
//                }
//
//                double classification = classifyInstance(feature_vector,label);
//                //System.out.println("Classified as: " + instances.classAttribute().value((int)classification));
//
//                analyzer.updateSkills(instances.classAttribute().value((int)classification));
//                //System.out.println("Updated Skills. " + analyzer.toString());
//            }
//
//            /* We still need to compute the MSE for the last level */
//            average_mse += analyzer.evaluate(cur_level, username);
//            average_mses.put(username,average_mse / num_levels_completed);
//
//            /* TODO: Get this to work for testing accuracy of machine learning algorithm */
////            try {
////
////                Evaluation eval = new Evaluation(instances);
////                //System.out.println(instances.size());
////                eval.evaluateModel(classifier,all_test_instances); /* Why is this not working correctly and not reading the instances in in the testing dataset? */
////                System.out.println("Results: ");
////                System.out.println("===========================");
////                String results = String.format("Num instances: %f\nNum correct/incorrect: %f/%f\nAccuracy: %f",eval.numInstances(),eval.correct(),eval.incorrect(),
////                       eval.weightedFMeasure());
////                System.out.println(results);
////                System.out.println(eval.toMatrixString());
//////                ArrayList<String> tmp = new ArrayList<String>();
//////                for (int i = 0; i < confusion_matrix.length; i++ ) {
//////                    ArrayList<String> row = new ArrayList<String>();
//////                    for ( double d : confusion_matrix[i] ) {
//////                        row.add(Double.toString(d));
//////                    }
//////                    tmp.add(String.join(",",row));
//////                }
//////                System.out.println("Confusion Matrix\n" + String.join("\n",tmp));
////                System.out.println("==========================");
////            } catch ( Exception e ) {
////                e.printStackTrace();
////            }
//        }
//
//        for ( String s : average_mses.keySet() ) {
//            System.out.println("Average MSE over levels for student " + s + ": " + average_mses.get(s));
//        }
//
//    }

//    private void getTrainingDataset(ArrayList<String> train_files) {
//        /* Get all the players in the log files */
//        instances = new Instances("Training_dataset",attributes,10);
//        instances.setClassIndex(attributes.size()-1);
//
//        HashMap<String, ArrayList<String> > data_files = tel_utils.getLevelData( path + "/data/" );
//
//        /* For now, open a log file, and extract slices for a given time interval */
//        int i = 0;
//        File directory = new File(path + "/log/");
//        File [] dir_list = directory.listFiles();
//        for ( File f : dir_list ) {
//            if ( !train_files.contains(f.getName()) ) {
//                continue;
//            }
//            ArrayList<String> telemetry = tel_utils.getTelemetryData(f.getAbsolutePath());
//
//            HashMap<String, ArrayList<String> > telemetry_by_level = tel_utils.getTelemetryByLevel(telemetry);
//
//            double t1 = 0;
//            double t2 = interval;
//
//            for ( String level : telemetry_by_level.keySet() ) {
//                ArrayList<String> tel_by_level = telemetry_by_level.get(level);
//            }
//
//             HashMap<Integer,PersistentData> all_persistent_data = getPersistentDataForLevel( telemetry, ArrayList<Integer> bit_vector,
//                    HashMap<String, ArrayList<String> > data_files ) {
//
//
////            HashMap<String, Object > persistent_data = new HashMap<String,Object>();
////            /* Initialize persistent data */
////
////            persistent_data.put("cur_track", -1);
////
////            /* These don't require looking at the level */
////            persistent_data.put("cur_mouse_comp", "");
////            persistent_data.put("cur_mouse_time", 0.0);
////            persistent_data.put("linking", false);
////            persistent_data.put("filename","");
////
////            persistent_data.put("direction_layout",new ArrayList<String>());
////            persistent_data.put("color_layout",new ArrayList<String>());
////            persistent_data.put("comp_link_map",new HashMap<String, String >());
////            persistent_data.put("comp_color_map",new HashMap<String, String >());
//
//            /* Find the last possible time */
//            String last_log = telemetry.get(telemetry.size()-1);
//            double final_time_ = Double.parseDouble(last_log.split("\t")[3]);
//            String username = getUser(telemetry);
//
//            while ( t1 < final_time_ ) {
//                ArrayList<String> tmp = tel_utils.getDataInIntervals(telemetry, t1, t2);
//
//                HashMap<String,Double> feature_vector = feat_extract.extractFeatureVector(path, tmp, persistent_data, data_files);
//
//                ArrayList<String> slices = getSlices(username,t1,t2);
//                ArrayList<String> labels = new ArrayList<String>();
//                for ( String slice : slices ) {
//                    String [] split_slice = slice.split(",");
//                    labels.add(split_slice[2]);
//                }
//
//                Set<String> unique_values = new HashSet<String>(labels);
//                ArrayList<String> common = new ArrayList<String>();
//
//                t1 += interval/2.0;
//                t2 = t1 + interval;
//
//                int max_freq = 0;
//                for ( String s : unique_values ) {
//                    int freq = Collections.frequency(labels,s);
//                    if ( freq >  max_freq ) {
//                        max_freq = freq;
//                        common.clear();
//                        common.add(s);
//                    } else {
//                        if ( freq == max_freq ) {
//                            /* Check the ranking */
//                            common.add(s);
//                        }
//                    }
//                }
//
//                String label = "";
//                if ( common.size() == 0 ) {
//                    continue;
//                } else if ( common.size() == 1 ) {
//                    label = common.get(0);
//                } else {
//                    if ( common.contains("C") ) {
//                        label = "C";
//                    } else if (common.contains("B")) {
//                        label = "B";
//                    } else if (common.contains("A")) {
//                        label = "A";
//                    }
//                }
////                System.out.println("----------------------------------");
////                int i = 0;
////                for ( String s : feat_extract.features ) {
////                    System.out.print((i == feature_vector.size()-1) ? feature_vector.get(s) + "\n" : feature_vector.get(s) + ",");
////                    if( feature_vector.get(s) < 0.0 ) {
////                        System.err.println(s + " is negative. This is inappropriate behavior. Exiting");
////                        System.exit(-1);
////                    }
////                    i++;
////                }
////                System.out.println("----------------------------------");
//                Instance new_instance = new DenseInstance(feature_vector.size()+1);
//                for ( Attribute a : attributes ) {
//                    if ( a.name().equals("annotations") ) {
//                        new_instance.setValue(a, label);
//                    } else {
//                        new_instance.setValue(a, feature_vector.get(a.name()));
//                    }
//                }
//                instances.add(new_instance);
//            }
//        }
//    }
