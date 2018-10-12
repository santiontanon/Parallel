package pmexperiments;

/**
 * Created by pavankantharaju on 3/1/18.
 */

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import playermodeling.*;
import weka.classifiers.Classifier;

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

public class SimulationAccuracy8Dash extends PlayerModeler {

    /* Used for data analysis */
    public static String CRITICAL_SECTION_PATH = PLAYER_MODELING_DATA_DIR + "critical_section_annotations_8_dash/";
    public static final String SLICE_FILE = PLAYER_MODELING_DATA_DIR + "slices.tsv";
    public static final String SKILL_LIST_PATH  = PLAYER_MODELING_DATA_DIR + "skill_list_per_level_8_dash/";

    private TelemetryUtils35Dash telemetry_utils_35_dash;
    private TelemetryUtils8Dash telemetry_utils_8_dash;
    private SkillAnalyzerExperiment8Dash skill_analyzer_8_dash;
    private FeatureExtraction feat_extract;

    private String training_data_relative_path;
    private String testing_data_relative_path;

    public HashMap<String,String> colors;

    public SimulationAccuracy8Dash(Classifier cls, double interval_, int u_technique_flag, String train_path, String test_path) {
        super(cls, interval_, u_technique_flag);

        training_data_relative_path = train_path;
        testing_data_relative_path = test_path;

        skill_analyzer_8_dash = new SkillAnalyzerExperiment8Dash("8_dash_skills_ordered.csv");
        telemetry_utils_35_dash = new TelemetryUtils35Dash();
        telemetry_utils_8_dash = new TelemetryUtils8Dash();
        feat_extract = new FeatureExtraction(skill_analyzer_8_dash);

        colors = new HashMap<String,String>();

        String [] color_strings = { " ", "!", "\"", "#", "$", "%", "&", "\'", "(", ")", "/", "." };
        for ( int i = 0; i < color_strings.length; i++) {
            colors.put(color_strings[i],Integer.toString(i));
        }
    }

    public ArrayList<String> getSlices(String username, double t1, double t2) {
        ArrayList<String> slices = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(SLICE_FILE));
            String line = "";
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

    public void createTrainingDataset() {
        if (DEBUG > 0) {
            System.out.println("-------------------Training dataset-------------------");
        }
        training_dataset = new Instances("Training_dataset", attributes, 10);
        training_dataset.setClassIndex(attributes.size() - 1);

        HashMap<String, ArrayList<String>> data_files = telemetry_utils_35_dash.getLevelData(training_data_relative_path + "/data/");

        File directory = new File(training_data_relative_path + "/log/");
        File[] dir_list = directory.listFiles();
        for (File f : dir_list) {

            ArrayList<String> telemetry = telemetry_utils_35_dash.readTelemetryFile(f.getAbsolutePath());
            String username = telemetry_utils_35_dash.getUsername(telemetry);

            if (username.equals("5-2")) {
                continue;
            }

            HashMap<String, ArrayList<String>> telemetry_by_level = telemetry_utils_35_dash.splitTelemetryByLevels(telemetry);

            for (String level : telemetry_by_level.keySet()) {
                ArrayList<String> tel_by_level = telemetry_by_level.get(level);

                Pair<HashMap<Integer, PersistentData>, ArrayList<Integer>> persistent_data_info = getPersistentDataForLevel(tel_by_level, data_files, training_data_relative_path);

                String first_log = tel_by_level.get(0);
                double start_time_ = Double.parseDouble(first_log.split("\t")[3]);

                String last_log = tel_by_level.get(tel_by_level.size() - 1);
                double final_time_ = Double.parseDouble(last_log.split("\t")[3]);

                double t1 = start_time_;
                double t2 = start_time_ + interval;

                while (t1 < final_time_) {
                    Pair<ArrayList<String>, ArrayList<Integer>> tel_in_interval = telemetry_utils_35_dash.getTelemetryInInterval(tel_by_level, t1, t2, persistent_data_info.p2);

                    HashMap<String, Double> feature_vector = feat_extract.extractFeatureVector(training_data_relative_path, tel_in_interval.p1, tel_in_interval.p2, persistent_data_info.p1, data_files);

                    ArrayList<String> slices = getSlices(username, t1, t2);
                    ArrayList<String> labels = new ArrayList<String>();
                    for (String slice : slices) {
                        String[] split_slice = slice.split(",");
                        if (split_slice[2].equals("B/C") || split_slice[2].equals("A/B")) {
                            /* Just ignore this */
                            continue;
                        }
                        labels.add(split_slice[2]);
                    }

                    Set<String> unique_values = new HashSet<String>(labels);
                    ArrayList<String> common = new ArrayList<String>();

                    t1 += interval / 2.0;
                    t2 = t1 + interval;

                    if (feature_vector.size() == 0) {
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

                    if (DEBUG > 0) {
                        System.out.println("----------------------------------");
                        int i = 0;
                        for (String s : FeatureExtraction.features) {
                            System.out.print((i == feature_vector.size() - 1) ? feature_vector.get(s) + "\n" : feature_vector.get(s) + ",");
                            if (feature_vector.get(s) < 0.0) {
                                System.err.println(s + " is negative. This is inappropriate behavior. Exiting");
                                System.exit(-1);
                            }
                            i++;
                        }
                        System.out.println("----------------------------------");
                    }

                    Instance new_instance = new DenseInstance(feature_vector.size() + 1);
                    for (Attribute a : attributes) {
                        if (a.name().equals("annotations")) {
                            new_instance.setValue(a, label);
                        } else {
                            new_instance.setValue(a, feature_vector.get(a.name()));
                        }
                    }
                    training_dataset.add(new_instance);
                }
            }
        }
    }

    @Override
    public void execute() {

        LinkedHashMap<String, LinkedHashMap<String, Double> > total_square_error_per_week = new LinkedHashMap<>();
        LinkedHashMap<String, LinkedHashMap<String, Integer>> week_to_skill_to_num_users = new LinkedHashMap<String, LinkedHashMap<String, Integer>>();

        /* For printing data */
        ArrayList<String> skill_vector_as_csv = new ArrayList<String>();
        ArrayList<String> ground_truth_as_csv = new ArrayList<String>();
        ArrayList<String> squared_error_per_week_as_csv = new ArrayList<String>();
        ArrayList<String> rule_evidence_as_csv = new ArrayList<String>();

        /* Student -> Week -> list of telemetry files */
        HashMap< String, LinkedHashMap< String, ArrayList<String> > > players = telemetry_utils_8_dash.getPlayers8Dash(testing_data_relative_path + "/log");

        createTrainingDataset();
        trainModel();

        /* NOTE: It is possible that we may not have ground truth for all players. Ignore these players */
        LinkedHashMap< String, LinkedHashMap< String, Double > > ground_truth = skill_analyzer_8_dash.ground_truth;
        ArrayList<String> players_with_ground_truth = new ArrayList<String>();
        for ( String s : ground_truth.keySet() ) {
            String [] tmp = s.split(",");
            String player = tmp[0].replace(".", "-");
            if ( !players_with_ground_truth.contains(player) ) {
                players_with_ground_truth.add(player);
            }
        }

        if ( DEBUG > 0 ) {
            System.out.println("Players to files played in a week");
            for (String player : players.keySet()) {
                for (String week : players.get(player).keySet()) {
                    System.out.println(player + "," + week + "," + String.join(",", players.get(player).get(week)));
                }
            }
        }

        LinkedHashMap<String, ArrayList<String>> data_files = telemetry_utils_8_dash.getLevelData(testing_data_relative_path + "/data/");

        ArrayList<String> testing_instance_labels = new ArrayList<String>();

        /* User, Level, Skill list */
        System.out.print("User,Level,");
        System.out.println(String.join(",", skill_analyzer_8_dash.skill_vector.keySet()));

        for (String player : players.keySet()) {

            if ( !players_with_ground_truth.contains(player) ) {
                continue;
            }

            skill_analyzer_8_dash.resetSkillVector();

            String reformatted_username = player.replace("-", ".");

            for ( String week : players.get(player).keySet() ) {
                for (String fname : players.get(player).get(week)) {
                    File f = new File(testing_data_relative_path + "/log/" + fname);
                    ArrayList<String> telemetry = telemetry_utils_8_dash.readTelemetryFile(f.getAbsolutePath());

                    LinkedHashMap<String, ArrayList<String>> telemetry_by_level = telemetry_utils_8_dash.splitTelemetryByLevels(telemetry);

                    for (String level : telemetry_by_level.keySet()) {

                        skill_analyzer_8_dash.resetSkillsPerLevel();
                        skill_analyzer_8_dash.resetRuleEvidence();

                        if (!skill_analyzer_8_dash.readSkillsForLevel(SKILL_LIST_PATH, level)) {
                            /* If level was not supposed to be played, then move to the next level */
                            continue;
                        }
                        ArrayList<String> tel_by_level = telemetry_by_level.get(level);

                        /* Time to explain the Integer arraylist:
                         *      In a given level, students can run the model checker as many times as they want. We have to separate executions in each level.
                         *      This arraylist maps what execution each telemetry entry is part of.
                         * */

                        Pair<HashMap<Integer, PersistentData>, ArrayList<Integer>> persistent_data_info = getPersistentDataForLevel(tel_by_level, data_files, testing_data_relative_path);

                        String first_log = tel_by_level.get(0);
                        double start_time_ = Double.parseDouble(first_log.split("\t")[3]);

                        String last_log = tel_by_level.get(tel_by_level.size() - 1);
                        double final_time_ = Double.parseDouble(last_log.split("\t")[3]);

                        double t1 = start_time_;
                        double t2 = start_time_ + interval;

                        if (DEBUG > 0) {
                            System.out.println("Level: " + level + ", start time: " + start_time_ + ", end time: " + final_time_);
                        }
                        while (t1 < final_time_) {
                            Pair<ArrayList<String>, ArrayList<Integer>> tel_in_interval = telemetry_utils_8_dash.getTelemetryInInterval(tel_by_level, t1, t2, persistent_data_info.p2);

                            HashMap<String, Double> feature_vector = feat_extract.extractFeatureVector_v2(testing_data_relative_path, tel_in_interval.p1, tel_in_interval.p2, persistent_data_info.p1, data_files);

                            t1 += interval / 2.0;
                            t2 = t1 + interval;

                            /* If feature vector is empty, then either there's no telemetry in the interval OR we have insufficient data to create the feature vector */
                            if (feature_vector.size() == 0) {
                                continue;
                            }

                            switch (skill_vector_update_technique_flag) {
                                case 0:
                                    /* Both Machine Learning and Rules */
                                    double classification = classifyInstance(feature_vector);
                                    testing_instance_labels.add(training_dataset.classAttribute().value((int) classification));
                                    skill_analyzer_8_dash.updateSkillVectorUsingMachineLearning(training_dataset.classAttribute().value((int) classification));
                                case 1:
                                    /* Only machine learning */
                                    classification = classifyInstance(feature_vector);
                                    testing_instance_labels.add(training_dataset.classAttribute().value((int) classification));
                                    skill_analyzer_8_dash.updateSkillVectorUsingMachineLearning(training_dataset.classAttribute().value((int) classification));
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
                                skill_analyzer_8_dash.updateSkillVectorUsingRules();
                            case 1:
                                break;
                            case 2:
                                /* Only rule_evidence */
                                skill_analyzer_8_dash.updateSkillVectorUsingRules();
                                break;
                        }
                    }
                }

                /* Data structure should contain all skills that have ground truth */
                LinkedHashMap<String, Double> squared_error = skill_analyzer_8_dash.evaluateSkills(week, reformatted_username);

                if ( !week_to_skill_to_num_users.containsKey(week) ) {
                    LinkedHashMap<String, Integer> skill_to_num_users = new LinkedHashMap<>();
                    for ( String s : squared_error.keySet() ) {
                        if ( squared_error.get(s) < 0 ) {
                            /* SE doesn't exist for this skill in this week for this specific user.  */
                            skill_to_num_users.put(s,0);
                        } else {
                            skill_to_num_users.put(s,1);
                        }
                    }
                    week_to_skill_to_num_users.put(week,skill_to_num_users);
                } else {
                    LinkedHashMap<String, Integer> skill_to_num_users = week_to_skill_to_num_users.get(week);
                    for ( String s : squared_error.keySet() ) {
                        if ( squared_error.get(s) > -0.00000000000000000000000000001 ) {
                            skill_to_num_users.put(s, skill_to_num_users.get(s) + 1);
                        }
                    }
                    week_to_skill_to_num_users.put(week,skill_to_num_users);
                }

                if ( !total_square_error_per_week.containsKey(week) ) {
                    total_square_error_per_week.put(week,squared_error);
                } else {
                    LinkedHashMap<String, Double > square_error_for_week = total_square_error_per_week.get(week);
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

                skill_vector_as_csv.add(reformatted_username + "," + week + "," + skill_analyzer_8_dash.printSkillVector());
                ground_truth_as_csv.add(reformatted_username + "," + week + "," + skill_analyzer_8_dash.getGroundTruthCSV(week, reformatted_username));
                rule_evidence_as_csv.add(reformatted_username + "," + week + "," + skill_analyzer_8_dash.printRuleEvidence());
                squared_error_per_week_as_csv.add(reformatted_username + "," + week + "," + skill_analyzer_8_dash.getSquaredErrorCSV(week, reformatted_username));
            }
        }

        System.out.println("Skill vectors");
        System.out.println(String.join("\n", skill_vector_as_csv));
        System.out.println("Rules skill vector");
        System.out.println(String.join("\n", rule_evidence_as_csv));
        System.out.println("Ground Truth");
        System.out.println(String.join("\n", ground_truth_as_csv));
        System.out.println("Squared Error (SE)");
        System.out.println(String.join("\n", squared_error_per_week_as_csv));

        System.out.println("Mean Squared Error over all users per week ( For each skill, sum of squared error for all users in the week / number of users in that week )");
        for (String week : total_square_error_per_week.keySet()) {
            LinkedHashMap<String, Double> total_square_error = total_square_error_per_week.get(week);
            ArrayList<String> print_strings = new ArrayList<String>();
            for (String skill : total_square_error.keySet()) {
                if (total_square_error.get(skill) < 0) {
                    print_strings.add("");
                } else {
                    if (week_to_skill_to_num_users.get(week).get(skill) == 0) {
                        /* This should actually never happen ( means this skill was never used by anyone ) */
                        System.out.println("Skill was not used by anyone: " + skill);
                        System.exit(-1);
                    } else {
                        print_strings.add(Double.toString(total_square_error.get(skill) / week_to_skill_to_num_users.get(week).get(skill)));
                    }
                }
            }
            System.out.println("user," + week + "," + String.join(",", print_strings));
        }
    }

    public Pair<HashMap<Integer, PersistentData>, ArrayList<Integer>> getPersistentDataForLevel(ArrayList<String> telemetry,
                                                                                                HashMap<String, ArrayList<String>> data_files, String path) {
        /* From the level...find the next TriggerLoadLevel that contains a non-empty string that doesn't start with "l". Update persistent data */
        HashMap<Integer, PersistentData> all_persistent_data = new HashMap<Integer, PersistentData>();
        ArrayList<Integer> index_vector = new ArrayList<Integer>();

        String[] data = telemetry.get(0).split("\t");
        String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_, formatter);

        Gson gson = new Gson();

        /* Rule: Understand that arrows move at unpredictable rates */
        HashMap<String, Pair<Integer, Integer>> prev_semaphore_locations = null; //new HashMap<String, Pair<Integer, Integer>>();
        HashMap<String, String> prev_semaphore_tracks = null;
        //new HashMap<String, String>();

        /*  Rules: "Blocking critical section"
            NOTE: These never change in a level
        */
        boolean critical_section_data_populated = false;
        LinkedHashMap< String,LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > > > critical_section_data = null;

        boolean testing_before_submitting = false;

        //int num_tests = 0;
        /* Time in seconds */
        long s_ = (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000);
        int incr = 0;
        for (String tel : telemetry) {
            data = tel.split("\t");
            String name_ = data[1];
            String data_ = data[2];

            if (name_.equals("SubmitCurrentLevelPlay") ) {
                testing_before_submitting = true;
            }

            if ( name_.equals("SubmitCurrentLevelME") ) {
                if ( testing_before_submitting ) {
                    skill_analyzer_8_dash.updateRuleEvidence("Testing before submitting");
                    testing_before_submitting = false;
                }
            }

            if (name_.equals("TriggerLoadLevel")) {
                if (data_.length() != 0) {
                    if (data_.startsWith("l")) {
                        /* Get the critical section for this level */
                        try {
                            //num_tests = 0;
                            if ( new File(CRITICAL_SECTION_PATH + "/" + data_ + ".txt").exists() && !critical_section_data_populated ) {
                                critical_section_data_populated = true;
                                critical_section_data = new LinkedHashMap< String, LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > > >();

                                BufferedReader br = new BufferedReader(new FileReader(CRITICAL_SECTION_PATH + "/" + data_ + ".txt"));
                                String line; int num_lines = 0;
                                ArrayList<ArrayList<String>> critical_section = new ArrayList<ArrayList<String>>();

                                /* Pair each semaphore to all buttons */
                                ArrayList<ArrayList<String>> direction_board = new ArrayList<ArrayList<String>>();

                                int board_width = 0, board_height = 0;
                                while ((line = br.readLine()) != null) {
                                    if (line.startsWith("board_width")) {
                                        board_width = Integer.parseInt(line.split("\t")[1]);
                                    }

                                    if (line.startsWith("board_height")) {
                                        board_height = Integer.parseInt(line.split("\t")[1]);
                                    }

                                    if (line.startsWith("DIRECTIONS")) {
                                        for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                            String tmp = br.readLine();
                                            ArrayList<String> row = new ArrayList<String>();
                                            for (int m = 0; m < tmp.length(); m++) {
                                                row.add(String.valueOf(tmp.charAt(m)));
                                            }
                                            direction_board.add(row);
                                        }
                                    }

                                    if (line.startsWith("CRITICALSECTIONS")) {
                                        for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                            String tmp = br.readLine();
                                            ArrayList<String> row = new ArrayList<String>();
                                            for (int m = 0; m < tmp.length(); m++) {
                                                row.add(String.valueOf(tmp.charAt(m)));
                                            }
                                            critical_section.add(row);
                                        }
                                        break;
                                    }
                                    num_lines++;
                                }
                                for ( int i = 0; i < critical_section.size(); i++ ) {
                                    for ( int j = 0; j < critical_section.get(0).size(); j++ ) {
                                        if ( DEBUG > 0 ) { System.out.println(data_+  "," + i + "," + j + "," + critical_section.get(i).get(j)); }
                                        if ( Character.isUpperCase(critical_section.get(i).get(j).charAt(0)) ) {
                                            /* Delivery Point */
                                            if ( !critical_section_data.containsKey(critical_section.get(i).get(j).toLowerCase()) ) {
                                                LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > > coords =
                                                        new LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > >();
                                                coords.put("semaphore",new ArrayList< Pair<Integer,Integer> >());
                                                coords.put("button",new ArrayList< Pair<Integer,Integer> >());
                                                critical_section_data.put(critical_section.get(i).get(j).toLowerCase(),coords);
                                            }
                                            LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > > tmp = critical_section_data.get(critical_section.get(i).get(j).toLowerCase());
                                            ArrayList< Pair<Integer,Integer> > coords = tmp.get("button");
                                            if ( i-1 >= 0 && direction_board.get(i - 1).get(j).equals("A") ) {
                                                    coords.add(new Pair<Integer, Integer>( j, i - 1));
                                            }

                                            if ( i+1 < direction_board.size() && direction_board.get(i + 1).get(j).equals("V") ) {
                                                coords.add(new Pair<Integer, Integer>(j, i + 1));
                                            }

                                            if ( j-1 >= 0 && direction_board.get(i).get(j - 1).equals("<") ) {
                                                coords.add(new Pair<Integer, Integer>(j - 1,i));
                                            }

                                            if ( j+1 < direction_board.get(0).size() && direction_board.get(i).get(j + 1).equals(">") ) {
                                                coords.add(new Pair<Integer, Integer>(j + 1,i));
                                            }
                                            tmp.replace("button",coords);
                                            critical_section_data.replace(critical_section.get(i).get(j).toLowerCase(),tmp);
                                        }

                                        if ( Character.isLowerCase(critical_section.get(i).get(j).charAt(0)) ) {
                                            /* Pickup Point */
                                            if ( !critical_section_data.containsKey(critical_section.get(i).get(j).toLowerCase()) ) {
                                                LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > > coords =
                                                        new LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > >();
                                                coords.put("semaphore",new ArrayList< Pair<Integer,Integer> >());
                                                coords.put("button",new ArrayList< Pair<Integer,Integer> >());
                                                critical_section_data.put(critical_section.get(i).get(j).toLowerCase(),coords);
                                            }
                                            LinkedHashMap< String, ArrayList< Pair<Integer,Integer> > > tmp = critical_section_data.get(critical_section.get(i).get(j).toLowerCase());
                                            ArrayList< Pair<Integer,Integer> > coords = tmp.get("semaphore");

                                            if ( i-1 >= 0 && direction_board.get(i - 1).get(j).equals("V")) {
                                                coords.add(new Pair<Integer, Integer>( j, i - 1));
                                            }

                                            if ( i+1 < direction_board.size() && direction_board.get(i + 1).get(j).equals("A")) {
                                                coords.add(new Pair<Integer, Integer>(j, i + 1));
                                            }

                                            if ( j-1 >= 0 && direction_board.get(i).get(j-1).equals(">")) {
                                                coords.add(new Pair<Integer, Integer>(j-1, i));
                                            }

                                            if ( j+1 < direction_board.get(0).size() && direction_board.get(i).get(j+1).equals("<")) {
                                                coords.add(new Pair<Integer, Integer>(j+1, i));
                                            }
                                            tmp.replace("semaphore",coords);
                                            critical_section_data.replace(critical_section.get(i).get(j).toLowerCase(),tmp);
                                        }
                                    }
                                }
                                if ( DEBUG > 0 ) {
                                    System.out.println("Level: " + data_);
                                    for ( String section : critical_section_data.keySet() ) {
                                        for ( String sem_or_button : critical_section_data.get(section).keySet() ) {
                                            for ( Pair<Integer,Integer> coord : critical_section_data.get(section).get(sem_or_button) ) {
                                                System.out.println(section + "," + sem_or_button + " coordinate: " + coord.p1 + "," + coord.p2);
                                            }
                                        }
                                    }
                                }
                                br.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            /* Something horrible happened...exit. NOTE: Is this good Java error handling? */
                            System.exit(-1);
                        }
                    } else {
                        /* NOTE: This is actually what is sent by the ME for execution */
                        if (data_files.containsKey(data_)) {

                            /* Rules: "Deliver Packages" and "Understand specific delivery points" */
                            HashMap<String, Object> goal_struct;
                            HashMap<Integer, Integer> id_to_num_delivered = new HashMap<Integer, Integer>();
                            HashMap<Integer, Integer> id_to_num_missed = new HashMap<Integer, Integer>();
                            HashMap<Integer, Integer> id_to_num_delivered_required = new HashMap<Integer, Integer>();
                            HashMap<Integer, String> id_to_condition = new HashMap<Integer, String>();

//                            num_tests++;
//                            if ( num_tests >= 2 && !testing_before_submitting_added ) {
//                                /* NOTE: This is actually not correct. There is currently no way to differentiate between testing and submitting.
//                                *  Thus, I made an assumption. If you submit to the ME two or more times, you're testing before submitting.
//                                * */
//                                skill_analyzer_8_dash.updateRuleEvidence("Testing before submitting");
//                                testing_before_submitting_added = true;
//                            }

                            /* Create a feature vector if we have level data */
                            PersistentData persistent_data = new PersistentData();
                            persistent_data.persistent_data.put("start_time", s_);

                            /* Specific component information */
                            ArrayList<String> diverter_ids = new ArrayList<String>();
                            ArrayList<String> all_semaphore_ids = new ArrayList<String>();

                            ArrayList<String> button_ids = new ArrayList<String>();
                            ArrayList<String> delivery_ids = new ArrayList<String>();
                            ArrayList<String> pickup_ids = new ArrayList<String>();

                            /* Rule: "Be able to link semaphores to buttons" */
                            HashMap<String, String> button_to_component = new HashMap<String, String>();
                            HashMap<String, ArrayList<String>> component_to_button = new HashMap<String, ArrayList<String>>();

                            /* Rule: Understand that arrows move at unpredictable rates */
                            HashMap<String, Pair<Integer, Integer>> current_semaphore_locations = new HashMap<String, Pair<Integer, Integer>>();
                            HashMap<String, String> current_semaphore_tracks = new HashMap<String, String>();

                            /* TODO (either future work or before AIIDE deadline): Run experiments with the rule for "Use Diverters" */
                            /*  Every time the game sends game state to the ME, check if any of the semaphores were not moved in any insignificant way
                             *   Insignificant is defined as moving within +1 or -1.
                             * */
//                            ModelEngine me = new ModelEngine(path + "/data/" + data_files.get(data_).get(0), skill_analyzer_8_dash);
//                            Map<String, Object> new_run = me.startModelEngine();
                            /* Get the final condition from the rerun */
                            //int final_condition = Integer.parseInt(String.valueOf(new_run.get("final_condition")));

                            /* TODO: Check whether these are reliable ways to detect starvation */
//                            if ( (final_condition & GameState.RESULT_PROBLEMATIC_STARVATION) == 0 &&
//                                (final_condition & GameState.RESULT_PROBLEMATIC_LOOPY_HASH) == 0) {
//                                    /* Starvation detected */
//                                    skill_analyzer_8_dash.updateRuleEvidence("Prevent starvation");
//                            }
                           //String level = "";

                            try {
                                BufferedReader br = new BufferedReader(new FileReader(path + "/data/" + data_files.get(data_).get(0)));
                                String line; int num_lines = 0;
                                int board_width, board_height = 0;
                                boolean start_parse = false;
                                boolean looking_at_execution = false;
                                while ((line = br.readLine()) != null) {

//                                    if ( line.startsWith("level_id") ) {
//                                        level = line.split("\t")[1];
//                                    }

                                    if (line.startsWith("board_width")) {
                                        board_width = Integer.parseInt(line.split("\t")[1]);
                                        persistent_data.persistent_data.put("width", board_width);
                                    }

                                    if (line.startsWith("board_height")) {
                                        board_height = Integer.parseInt(line.split("\t")[1]);
                                        persistent_data.persistent_data.put("height", board_height);
                                    }

                                    if (line.startsWith("goal_struct")) {
                                        /* Rule: "Understand specific delivery points" and "Deliver packages" */

                                        goal_struct = gson.fromJson(line.split("\t")[1], HashMap.class);
                                        /* For each required goal, check the number of packages each delivery point requires */
                                        ArrayList<LinkedTreeMap<String, Object>> required = (ArrayList<LinkedTreeMap<String, Object>>) goal_struct.get("required");
                                        for (LinkedTreeMap<String, Object> cond : required) {

                                            /* NOTE: We may need to consider other types of goals */
                                            if (!((String) cond.get("type")).equals("delivery")) {
                                                /* Anything but delivery component */
                                                continue;
                                            }

                                            int id = ((Double) cond.get("id")).intValue();
                                            int component_value = ((Double) cond.get("value")).intValue();
                                            String condition = (String) cond.get("condition");

                                            /* Set up id -> num_delivered mapping */
                                            id_to_num_delivered.put(id, 0);
                                            id_to_num_missed.put(id, 0);
                                            id_to_num_delivered_required.put(id, component_value);
                                            id_to_condition.put(id, condition);
                                        }
                                    }

                                    if (line.startsWith("EXECUTION")) {
                                        looking_at_execution = true;
                                        continue;
                                    }

//                                    if (looking_at_execution && (line.split("\t")).length > 5 && (line.split("\t")[5]).contains("final_condition")) {
//                                        //String info = line.split("\t")[5];
//                                        //HashMap<String, Double> final_condition = gson.fromJson(info, HashMap.class);
//                                        /* TODO: Check whether these are reliable ways to detect starvation */
//                                        if ( (final_condition.get("final_condition").intValue() & GameState.RESULT_PROBLEMATIC_STARVATION) == 0 &&
//                                                (final_condition.get("final_condition").intValue() & GameState.RESULT_PROBLEMATIC_LOOPY_HASH) == 0) {
//                                            /* Starvation detected */
//                                            skill_analyzer_8_dash.updateRuleEvidence("Prevent starvation");
//                                        }
//
//                                        if ((final_condition.get("final_condition").intValue() & GameState.RESULT_PROBLEMATIC_INFINITE_LOOP) == 0) {
//                                            /* Starvation detected */
//                                            skill_analyzer_8_dash.updateRuleEvidence("Prevent starvation");
//                                        }
//                                    }

                                    if (looking_at_execution && line.split("\t")[0].equals("D")) {
                                        /* Execution denotes a package was delivered */
                                        HashMap<String, Object> delivery_execution = gson.fromJson(line.split("\t")[5], HashMap.class);
                                        /* Structure example:
                                            {"missed_items":[],"delivered_items":[2002],"delivered_to":3001}
                                            {exchange_between_b=1004.0, exchange_between_a=1001.0}
                                        */
                                        if (!delivery_execution.containsKey("delivered_items")) {
                                            /* This is an exchange point */
                                            if ( delivery_execution.containsKey("exchange_between_b") ) {
                                                /* Check if both have packages. If both do, then exchange. NOTE: Is it possible to exchange with a thread that doesn't have a package? */
                                                if ( delivery_execution.get("exchange_between_a") != null && delivery_execution.get("exchange_between_b") != null ) {
                                                    skill_analyzer_8_dash.updateRuleEvidence("Understand exchange points");
                                                }
                                                //int a = ((Double) tmp.get("exchange_between_a")).intValue();
                                                //int b = ((Double) tmp.get("exchange_between_b")).intValue();
                                                //skill_analyzer_8_dash.updateRuleEvidence("Understand exchange points");
                                            } else {
                                                continue;
                                            }
                                        } else {
                                            /* This is a delivery point */
                                            /* NOTE: Is this the correct way to get missed packages? */
                                            /* NOTE: If a delivery id is missing from this data structure, that means it wasn't required... */
                                            ArrayList<Integer> delivered = (ArrayList<Integer>) delivery_execution.get("delivered_items");
                                            ArrayList<Integer> missed = (ArrayList<Integer>) delivery_execution.get("missed_items");
                                            if ( DEBUG > 0 ) { System.out.println("Delivery execution: " + delivery_execution.toString()); }
                                            int id = ((Double) delivery_execution.get("delivered_to")).intValue();
                                            if ( id_to_num_missed.containsKey(id) ) {
                                                id_to_num_missed.replace(id, id_to_num_missed.get(id) + missed.size());
                                                id_to_num_delivered.replace(id, id_to_num_delivered.get(id) + delivered.size());
                                            }
                                        }
                                    }

                                    if (line.startsWith("DIRECTIONS")) {
                                        ArrayList<ArrayList<String>> board = (ArrayList<ArrayList<String>>) persistent_data.persistent_data.get("direction_layout");

                                        for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                            String tmp = br.readLine();
                                            ArrayList<String> row = new ArrayList<String>();
                                            for (int m = 0; m < tmp.length(); m++) {
                                                row.add(String.valueOf(tmp.charAt(m)));
                                            }
                                            board.add(row);
                                        }
                                        persistent_data.persistent_data.replace("direction_layout", board);
                                    }

                                    if (line.startsWith("COLORS")) {
                                        ArrayList<ArrayList<String>> color = (ArrayList<ArrayList<String>>) persistent_data.persistent_data.get("color_layout");
                                        for (int i = num_lines + 1; i < num_lines + board_height + 1; i++) {
                                            String tmp = br.readLine();
                                            ArrayList<String> row = new ArrayList<String>();
                                            for (int m = 0; m < tmp.length(); m++) {
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
                                                if (persistent_data.persistent_data.containsKey("comp_loc_map")) {
                                                    HashMap<String, ArrayList<Integer>> comp_loc_map = (HashMap<String, ArrayList<Integer>>) persistent_data.persistent_data.get("comp_loc_map");
                                                    ArrayList<Integer> tmp = new ArrayList<Integer>();
                                                    tmp.add(Integer.parseInt(comp_info[2]));
                                                    tmp.add(Integer.parseInt(comp_info[3]));
                                                    //System.out.println("Component: " + comp_info[0] + "," + data_files.get(data_).get(0));
                                                    comp_loc_map.put(comp_info[0], tmp);
                                                    persistent_data.persistent_data.replace("comp_loc_map", comp_loc_map);
                                                }

                                                /* Actual information about the component is located at index 6 */
                                                HashMap<String, Object> info = feat_extract.parseComponentInformation(comp_info[6]);

                                                if (info.containsKey("color")) {
                                                    if (persistent_data.persistent_data.containsKey("comp_color_map")) {
                                                        HashMap<String, String> color_map = (HashMap<String, String>) persistent_data.persistent_data.get("comp_color_map");
                                                        int x = Integer.parseInt(comp_info[2]); int y = Integer.parseInt(comp_info[3]);
                                                        ArrayList<ArrayList<String>> color_board = (ArrayList<ArrayList<String>>) persistent_data.persistent_data.get("color_layout");
                                                        String actual_color = colors.get(color_board.get(y).get(x));
                                                        if ( actual_color == null ) {
                                                            System.out.println("Add this color to the list: " + color_board.get(y).get(x));
                                                            System.exit(-1);
                                                        }
                                                        color_map.put(comp_info[0], actual_color);
                                                        persistent_data.persistent_data.replace("comp_color_map", color_map);
                                                    }
                                                }

                                                switch (comp_info[1]) {
                                                    case "diverter":
                                                        diverter_ids.add(comp_info[0]);
                                                        break;
                                                    case "delivery":
                                                        delivery_ids.add(comp_info[0]);
                                                        break;
                                                    case "pickup":
                                                        pickup_ids.add(comp_info[0]);
                                                        break;
                                                    case "semaphore":
                                                        if ( comp_info[4].equals("P") ) {
                                                            /* Placed by Player */
                                                            skill_analyzer_8_dash.updateRuleEvidence("Place objects on the track");
                                                        }

                                                        all_semaphore_ids.add(comp_info[0]);
                                                        if (!current_semaphore_locations.containsKey(comp_info[0])) {
                                                            int x = Integer.parseInt(comp_info[2]); int y = Integer.parseInt(comp_info[3]);
                                                            current_semaphore_locations.put(comp_info[0], new Pair<Integer, Integer>(x, y));
                                                            ArrayList<ArrayList<String>> color_board = (ArrayList<ArrayList<String>>) persistent_data.persistent_data.get("color_layout");
                                                            String actual_color = colors.get(color_board.get(y).get(x));
                                                            current_semaphore_tracks.put(comp_info[0], actual_color);
                                                            if ( DEBUG > 0 ) {
                                                                System.out.println(String.format("Track of semaphore %s, (x,y): (%d,%d)",actual_color,x,y));
                                                            }
                                                            //current_semaphore_tracks.put(comp_info[0], String.valueOf(info.get("color")));
                                                        } else {
                                                            int x = Integer.parseInt(comp_info[2]); int y = Integer.parseInt(comp_info[3]);
                                                            current_semaphore_locations.replace(comp_info[0], new Pair<Integer, Integer>(x, y));
                                                            ArrayList<ArrayList<String>> color_board = (ArrayList<ArrayList<String>>) persistent_data.persistent_data.get("color_layout");
                                                            String actual_color = colors.get(color_board.get(y).get(x));
                                                            current_semaphore_tracks.replace(comp_info[0], actual_color);
                                                            if ( DEBUG > 0 ) {
                                                                System.out.println(String.format("Track of semaphore %s, (x,y): (%d,%d)",actual_color,x,y));
                                                            }
                                                        }
                                                        break;
                                                    case "signal":
                                                        if ( comp_info[4].equals("P") ) {
                                                            /* Placed by Player */
                                                            skill_analyzer_8_dash.updateRuleEvidence("Place objects on the track");
                                                        }

                                                        if (info.containsKey("link") && ((Double) info.get("link")).intValue() != -1) {
                                                            String linked_item_id = Integer.toString(((Double) info.get("link")).intValue());
                                                            button_to_component.put(comp_info[0], linked_item_id);
                                                            if (component_to_button.containsKey(linked_item_id)) {
                                                                ArrayList<String> buttons = component_to_button.get(linked_item_id);
                                                                buttons.add(comp_info[0]);
                                                                component_to_button.replace(linked_item_id, buttons);
                                                            } else {
                                                                ArrayList<String> buttons = new ArrayList<String>();
                                                                buttons.add(comp_info[0]);
                                                                component_to_button.put(linked_item_id, buttons);
                                                            }
                                                            button_ids.add(comp_info[0]);
                                                        }
                                                        break;
                                                    default:
                                                        break;
                                                }
                                            }
                                        }
                                    }
                                    num_lines++;
                                }
                                br.close();
                            } catch ( Exception e ) {
                                System.out.println(String.format("Error dealing with file %s",data_files.get(data_).get(0)));
                                e.printStackTrace();
                            }

                            /* Rule: "Understand that arrows move at unpredictable rates"  */
                            if ( prev_semaphore_locations == null ) {
                                prev_semaphore_locations = new HashMap<String, Pair<Integer, Integer>>();
                                prev_semaphore_tracks = new HashMap<String, String>();
                                for ( String sem_id : current_semaphore_tracks.keySet() ) {
                                    prev_semaphore_tracks.put(sem_id, current_semaphore_tracks.get(sem_id));
                                }
                                for ( String sem_id : current_semaphore_locations.keySet() ) {
                                    prev_semaphore_locations.put(sem_id, current_semaphore_locations.get(sem_id));
                                }
                            } else {
                                for (String sem_id : current_semaphore_tracks.keySet()) {
                                    String current_track_color = current_semaphore_tracks.get(sem_id);
                                    if ( !prev_semaphore_tracks.containsKey(sem_id) ) {
                                        /* Semaphore was deleted */
                                        continue;
                                    }
                                    String prev_track_color = prev_semaphore_tracks.get(sem_id);

                                    if (prev_track_color.equals(current_track_color)) {
                                        /* Check whether this has moved insignificantly (TODO: I'm not sure if this is right as we haven't defined insignifiant exactly) */
                                        Pair<Integer, Integer> prev_coord = prev_semaphore_locations.get(sem_id);
                                        Pair<Integer, Integer> cur_coord = current_semaphore_locations.get(sem_id);

                                        int manhattan_distance = Math.abs(prev_coord.p1-cur_coord.p1) + Math.abs(prev_coord.p2-cur_coord.p2);
                                        if ( !(prev_coord.equals(cur_coord)) && (manhattan_distance > 1) ) {
                                            /* Changed "significantly" */
                                            skill_analyzer_8_dash.updateRuleEvidence("Understand that arrows move at unpredictable rates");
                                        }
                                        /* Check whether the object has been moved by + or -1 in any direction */
//                                        if ( (prev_coord.p1 + 1 != cur_coord.p1) && (prev_coord.p1 - 1 != cur_coord.p1) &&
//                                                (prev_coord.p2 + 1 != cur_coord.p2) && (prev_coord.p2 - 1 != cur_coord.p2)
//                                                && !(prev_coord.equals(cur_coord)) ) {
//                                            /* Not moved + or -1 in any direction, are on the same track, and has at least changed. */
//                                            skill_analyzer_8_dash.updateRuleEvidence("Understand that arrows move at unpredictable rates");
//                                        }
                                    }
                                }
                                prev_semaphore_tracks.clear();
                                prev_semaphore_locations.clear();
                                for ( String sem_id : current_semaphore_tracks.keySet() ) {
                                    prev_semaphore_tracks.replace(sem_id, current_semaphore_tracks.get(sem_id));
                                }
                                for ( String sem_id : current_semaphore_locations.keySet() ) {
                                    prev_semaphore_locations.replace(sem_id, current_semaphore_locations.get(sem_id));
                                }
                            }

                            HashMap<String, String> button_to_semaphore = new HashMap<String, String>();
                            HashMap<String, ArrayList<String>> semaphore_to_button = new HashMap<String, ArrayList<String>>();

                            /* Rule: "Be able to link semaphores to buttons" and "Understand that arrows move at unpredictable rates" */
                            for ( String sem_id : component_to_button.keySet() ) {
                                if ( all_semaphore_ids.contains(sem_id) ) {
                                    /* Element is linked and is actually a semaphore */
                                    skill_analyzer_8_dash.updateRuleEvidence("Be able to link semaphores to buttons");
                                    for ( String button_id : component_to_button.get(sem_id) ) {
                                        button_to_semaphore.put(button_id,sem_id);
                                    }
                                    semaphore_to_button.put(sem_id,component_to_button.get(sem_id));
                                }
                            }

                            ArrayList<String> unlinked_semaphore_ids = new ArrayList<String>();
                            for ( String sem_id : all_semaphore_ids ) {
                                if ( !semaphore_to_button.containsKey(sem_id) ) {
                                    /* This semaphore is not linked */
                                    unlinked_semaphore_ids.add(sem_id);
                                }
                            }

                            HashMap<String, String> color_map = (HashMap<String, String>) persistent_data.persistent_data.get("comp_color_map");
                            HashMap<String, Integer> num_semaphores_unlinked_per_track = new HashMap<String, Integer>();
                            for (String sem_id : unlinked_semaphore_ids) {
                                String sem_id_color = color_map.get(sem_id);
                                if (num_semaphores_unlinked_per_track.containsKey(sem_id_color)) {
                                    num_semaphores_unlinked_per_track.replace(sem_id_color, num_semaphores_unlinked_per_track.get(sem_id_color) + 1);
                                } else {
                                    num_semaphores_unlinked_per_track.put(sem_id_color, 1);
                                }
                            }

                            for (String track : num_semaphores_unlinked_per_track.keySet()) {
                                if (num_semaphores_unlinked_per_track.get(track) < 2) {
                                    skill_analyzer_8_dash.updateRuleEvidence("Understand that arrows move at unpredictable rates");
                                }
                            }

                            /* Check "Alternating access with semaphores and locks (ensure mutual exclusion)" rule here
                             * TODO future work: As of now, critical section and ensure mutual exclusion are the same rule-wise. Update the rule.
                             * Maybe we need to consider the color of track...
                             * */

                            if ( critical_section_data != null ) {
                                for (String section : critical_section_data.keySet()) {
                                    ArrayList<Pair<Integer, Integer>> sem_coords = critical_section_data.get(section).get("semaphore");
                                    ArrayList<Pair<Integer, Integer>> button_coords = critical_section_data.get(section).get("button");

                                    ArrayList<Pair<Integer, Integer>> potential_sem_copy = new ArrayList<Pair<Integer, Integer>>();
                                    for (Pair<Integer, Integer> c : sem_coords) {
                                        potential_sem_copy.add(c.clone());
                                    }
                                    ArrayList<Pair<Integer, Integer>> potential_button_copy = new ArrayList<Pair<Integer, Integer>>();
                                    for (Pair<Integer, Integer> c : button_coords) {
                                        potential_button_copy.add(c.clone());
                                    }

                                    if (potential_button_copy.size() == potential_sem_copy.size()) {
                                        for (String s : button_to_semaphore.keySet()) {
                                            HashMap<String, ArrayList<Integer>> comp_loc_map = (HashMap<String, ArrayList<Integer>>) persistent_data.persistent_data.get("comp_loc_map");
                                            ArrayList<Integer> button_coord = comp_loc_map.get(s);
                                            ArrayList<Integer> semaphore_coord = comp_loc_map.get(button_to_semaphore.get(s));

                                            if (semaphore_coord == null) {
                                                /* Linking to a non-existant semaphore */
                                                continue;
                                            }

                                            Pair<Integer, Integer> button = new Pair<Integer, Integer>(button_coord.get(0), button_coord.get(1));
                                            Pair<Integer, Integer> semaphore = new Pair<Integer, Integer>(semaphore_coord.get(0), semaphore_coord.get(1));

                                            if (potential_button_copy.contains(button) && potential_sem_copy.contains(semaphore)) {
                                                potential_button_copy.remove(button);
                                                potential_sem_copy.remove(semaphore);
                                            }
                                        }
                                        /* If these are not empty, this could mean:
                                            1) Haven't blocked the critical section (buttons and semaphores may not be linked or there are missing semaphores and buttons)
                                            2) Blocked more than the critical section
                                        */
                                        if (potential_button_copy.isEmpty() && potential_sem_copy.isEmpty()) {
                                            skill_analyzer_8_dash.updateRuleEvidence("Block critical sections");
                                        }
                                    } else {
                                        if (potential_button_copy.size() > potential_sem_copy.size()) {
                                            HashMap<String, ArrayList<Integer>> comp_loc_map = (HashMap<String, ArrayList<Integer>>) persistent_data.persistent_data.get("comp_loc_map");
                                            for ( String sem : semaphore_to_button.keySet() ) {
                                                ArrayList<Integer> semaphore_coord = comp_loc_map.get(sem);
                                                if ( semaphore_coord == null ) {
                                                    continue;
                                                }
                                                Pair<Integer, Integer> semaphore = new Pair<Integer, Integer>(semaphore_coord.get(0), semaphore_coord.get(1));
                                                if ( potential_sem_copy.contains(semaphore) ) {
                                                    /* Now, search through all buttons connected to it and make sure all are in the critical section */
                                                    boolean all_buttons_accounted = true;
                                                    for ( String but : semaphore_to_button.get(sem) ) {
                                                        ArrayList<Integer> button_coord = comp_loc_map.get(but);
                                                        Pair<Integer, Integer> button = new Pair<Integer, Integer>(button_coord.get(0), button_coord.get(1));
                                                        if ( potential_button_copy.contains(button) ) {
                                                            potential_button_copy.remove(button);
                                                        } else {
                                                            /* This is a problem because a thread could unlock the critical section from outside */
                                                            all_buttons_accounted = false;
                                                            break;
                                                        }
                                                    }
                                                    if ( all_buttons_accounted ) {
                                                        potential_sem_copy.remove(semaphore);
                                                    }
                                                }
                                            }
                                            if ( potential_button_copy.isEmpty() && potential_sem_copy.isEmpty() ) {
                                                skill_analyzer_8_dash.updateRuleEvidence("Block critical sections");
                                            }

//                                            for (String s : button_to_semaphore.keySet()) {
//                                                HashMap<String, ArrayList<Integer>> comp_loc_map = (HashMap<String, ArrayList<Integer>>) persistent_data.persistent_data.get("comp_loc_map");
//                                                ArrayList<Integer> button_coord = comp_loc_map.get(s);
//                                                ArrayList<Integer> semaphore_coord = comp_loc_map.get(button_to_semaphore.get(s));
//
//                                                if (semaphore_coord == null) {
//                                                    /* Linking to a non-existant semaphore */
//                                                    continue;
//                                                }
//
//                                                Pair<Integer, Integer> button = new Pair<Integer, Integer>(button_coord.get(0), button_coord.get(1));
//                                                Pair<Integer, Integer> semaphore = new Pair<Integer, Integer>(semaphore_coord.get(0), semaphore_coord.get(1));
//
//                                                if (potential_button_copy.contains(button) && potential_sem_copy.contains(semaphore)) {
//                                                    potential_button_copy.remove(button);
//                                                }
//                                            }
//                                            if (potential_button_copy.isEmpty()) {
//                                                skill_analyzer_8_dash.updateRuleEvidence("Block critical sections");
//                                            }
                                        }
                                    }
                                }
                            }

                            for (String button_id : button_to_semaphore.keySet()) {
                                String button1_color = color_map.get(button_id);
                                /* Check the color of the linked semaphore */
                                String sem2_color = color_map.get(button_to_semaphore.get(button_id));
                                if ( DEBUG > 0 ) {
                                    System.out.println("Button 1 color: " + button1_color + "," + " Semaphore 2 color: " + sem2_color);
                                }

                                /* Make sure these colors are different */
                                if (button1_color.equals(sem2_color)) {
                                    continue;
                                }

                                /* Check if a semaphore exists on lock1_color, and check what lock it's connected to.  */
                                for (String comp_id : color_map.keySet()) {
                                    /* If one the same track as lock1, and is a semaphore */
//                                    System.out.println("Color map: " + color_map);
//                                    System.out.println("file: " + data_files.get(data_).get(0));
//                                    System.out.println("data_files: " + data_files.get(data_));
                                    if (color_map.get(comp_id).equals(button1_color) && all_semaphore_ids.contains(comp_id) && !unlinked_semaphore_ids.contains(comp_id) ) {
                                        //String sem1_color = color_map.get(comp_id);
                                        //System.out.println(semaphore_to_button.toString() + " " + comp_id);
                                        /* TODO: What if there is more than one button connected to the semaphore? */
                                        for (String button : semaphore_to_button.get(comp_id)) {
                                            String button2_color = color_map.get(button);
                                            /* Check if lock2's color is the same as sem2's color, and check if lock2 isn't the same color as sem1 */
                                            if (button2_color.equals(sem2_color)) {
                                                skill_analyzer_8_dash.updateRuleEvidence("Synchronize multiple arrows");
                                            }
                                        }
                                    }
                                }
                            }

                            /* NOTE: Is this computed for each file sent over to the ME or for all executions? */
                            int total_missed = 0;
                            boolean delivered_all_packages = true;
                            for (Integer id : id_to_num_delivered_required.keySet()) {
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
                            if (delivered_all_packages) {
                                //System.out.println("You successfully delivered all the packages!");
                                skill_analyzer_8_dash.updateRuleEvidence("Deliver packages");
                            }

                            if (total_missed == 0) {
                                //System.out.println("You successfully didn't miss any packages!");
                                skill_analyzer_8_dash.updateRuleEvidence("Understand specific delivery points");
                            }

                            index_vector.add(incr);
                            all_persistent_data.put(incr, persistent_data);
                            incr++;
                            continue;
                        } else {
                            if ( DEBUG > 0 ) {
                                System.out.println("We do not an execution file for this..: " + data_);
                            }
                        }
                    }
                }
            }
            index_vector.add(incr);
        }

        return new Pair<HashMap<Integer, PersistentData>, ArrayList<Integer>>(all_persistent_data, index_vector);
    }
}