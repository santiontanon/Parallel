package playermodeling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by pavankantharaju on 3/5/18.
 */
public class SkillAnalysis {

    private final int DEBUG = 0;
    private final String data_file_dir = "data_files/";
    //private final String dataset = data_file_dir + "8_dash_skills_ordered.csv";
    private final String dataset = data_file_dir + "35_dash_skills_clean.csv";

    public LinkedHashMap< String,Pair<Integer,Double> > skill_vector;
    public LinkedHashMap< String, LinkedHashMap< String, Double > > ground_truth;

    /* Level-specific skills */
    public ArrayList<String> skills;

    private ArrayList<String> official_skill_names;

    /* Rule data structures */
    private int update_flag;
    public LinkedHashMap<String,Double> rules;

    public SkillAnalysis(int u_flag) {
        skill_vector = new LinkedHashMap< String, Pair<Integer,Double> >();
        ground_truth = new  LinkedHashMap< String, LinkedHashMap< String, Double > >();
        update_flag = u_flag;

        skills = new ArrayList<String>();

        /* Read in players and their skill level */
        official_skill_names = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(data_file_dir + "Official-Skills-List.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                official_skill_names.add(line.trim());
            }

            br = new BufferedReader(new FileReader(dataset));
            //BufferedReader br = new BufferedReader(new FileReader(data_file_dir + "35_dash_skills_clean.csv"));
            line = br.readLine();
            String [] skills = line.split(",");
            for ( int i = 2; i < skills.length; i++ ) {
                if ( !official_skill_names.contains(skills[i].trim()) ) {
                    System.out.println(String.format("Skill %s not correctly worded or doesn't exist in file %s",skills[i].trim(),dataset));
                    System.exit(1);
                }
                skill_vector.put(skills[i].trim(), new Pair< Integer, Double>(0,-1.0) );
            }
            while ((line = br.readLine()) != null) {
                String [] line_split = line.split(",");
                LinkedHashMap< String, Double > tmp = new LinkedHashMap<String, Double>();
                for ( int i = 2; i < skills.length; i++ ) {
                    double val = 0.0;
                    if ( line_split[i].equals("X") || line_split[i].equals("Y") ) {
                        val = -1;
                    } else {
                        val = Double.parseDouble(line_split[i].trim());
                    }
                    tmp.put(skills[i].trim(),val);
                }
                ground_truth.put( line_split[0].trim() + "," + line_split[1].trim(), tmp );
            }
            br.close();

            rules = new LinkedHashMap<String,Double>();
            for ( String s : skill_vector.keySet() ) {
                rules.put(s,0.0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean readSkillList(String path, String level_name) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "/" +level_name));
            String line = "";
            while ((line = br.readLine()) != null) {
                if ( !official_skill_names.contains(line.trim()) ) {
                    System.out.println(String.format("Skill %s not correctly worded or doesn't exist in file %s",line.trim(),path + "/" + level_name));
                    System.exit(1);
                }
                skills.add(line.trim());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void resetSkillsPerLevel() {
        if ( DEBUG > 0 ) {
            System.out.println("Resetting list of skills for each level");
        }
        skills.clear();
    }

    public void resetSkillVector() {
        if ( DEBUG > 0 ) {
            System.out.println("Resetting skill vector");
        }
        for( String s : skill_vector.keySet() ) {
            Pair<Integer, Double> tmp = skill_vector.get(s);
            tmp.p1 = 0;
            tmp.p2 = -1.0;
            skill_vector.replace(s,tmp);
        }
    }

    public void updateRules(String skill) {
        if ( DEBUG > 0 ) {
            System.out.println("Adding evidence for skill: " + skill);
        }
        switch ( update_flag ) {
            case 0:
                rules.replace(skill,rules.get(skill) + 1.0);
                break;
            case 1:
                rules.replace(skill,1.0);
                break;
        }
    }

    public void resetRuleEvidence() {
        if ( DEBUG > 0 ) {
            System.out.println("Resetting evidence for each rule");
        }
        for ( String s : rules.keySet() ) {
            rules.replace(s,0.0);
        }
    }

    public void updateSkillsBasedOnClassification(String classification) {
        if ( DEBUG > 0 ) {
            System.out.println("Updating skill vector with classification: " + classification);
        }
        double val = 0.0;
        if (classification.equals("C")) {
            val = 1.0;
        } else if (classification.equals("B")) {
            val = 0.5;
        } else if (classification.equals("A")) {
            val = 0.0;
        } else {
            val = 0.0;
        }

        for ( String s : skills ) {
            if ( !skill_vector.containsKey(s) ) {
                if ( DEBUG > 0 ) {
                    System.out.println("Skill not found (or doesn't have ground truth). Skipping..." + s);
                }
                continue;
            }

            Pair<Integer, Double> tmp = skill_vector.get(s);
            if ( tmp.p2 < 0 ) {
                tmp.p2 = val;
            } else {
                tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + val )/(tmp.p1+1);
            }
            tmp.p1++;

            skill_vector.replace(s,tmp);
        }
    }


    public void updateSkillsBasedOnRules( int updates_flag ) {
        for ( String s : skill_vector.keySet() ) {
            if ( rules.get(s) > 0.0 ) {
                Pair<Integer, Double> tmp = skill_vector.get(s);
                switch ( updates_flag ) {
                    case 0:
                        tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + rules.get(s) )/ ( tmp.p1+rules.get(s) );
                        tmp.p1 += rules.get(s).intValue();
                        break;
                    case 1:
                        tmp.p2 = rules.get(s);
                        tmp.p1++;
                        break;
                }
                skill_vector.replace(s,tmp);
            }
        }
    }

    /* ------------------------------------------ Mean-Squared Error Evaluation Methods ------------------------------------------ */

    public LinkedHashMap<String, Double> evaluateSkills(String level, String user) {
        /* Used to evaluate the performance of player modeling  */
        /* Compute Squared Error (SE) for each skill in the ground truth */
        /* TODO for future work: Compute KL-Divergence, Total variation distance, and Hellinger distance - ? */

        LinkedHashMap< String, Double> squared_error = new LinkedHashMap< String, Double> ();

        int lvl_number = Integer.parseInt(level.substring(level.lastIndexOf("l")+1,level.length()));

        //String key = user + " " + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);
        String key = user + "," + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);

        if ( user.equals("5-2") || (skills.size() == 0) ) {
            /* This user doesn't exist, and if a level that wasn't supposed to be played was played. */
            return squared_error;
        }

        for ( String s : skill_vector.keySet() ) {
            //System.out.println("Key,skill: " + s + "," + actual_skill_map.get(key).get(s));
            if ( !ground_truth.get(key).containsKey(s) ) {
                if ( DEBUG > 0 ) {
                    System.out.println(String.format("Skill %s not in ground truth. Ignoring...", s));
                }
                continue;
            }
            if ( ground_truth.get(key).get(s) < 0 ) {
                /* Ignore */
                squared_error.put(s,-1.0);
            } else {
                if ( skill_vector.get(s).p2 < 0 )  {
                    squared_error.put(s, Math.pow(ground_truth.get(key).get(s) - 0.5, 2.0));
                } else {
                    squared_error.put(s, Math.pow(ground_truth.get(key).get(s) - skill_vector.get(s).p2, 2.0));
                }
            }
        }

        return squared_error;
    }

    public LinkedHashMap<String, Double> evaluateSkillsPerWeek(String week, String user) {
        /* Used to evaluate the performance of player modeling  */
        /* Compute Squared Error (SE) for each skill in the ground truth */
        /* TODO for future work: Compute KL-Divergence, Total variation distance, and Hellinger distance - ? */

        LinkedHashMap< String, Double> squared_error = new LinkedHashMap< String, Double> ();

        String key = user + "," + week;

        if ( skills.size() == 0 ) {
            /* This user doesn't exist, and if a level that wasn't supposed to be played was played. */
            return squared_error;
        }

        for ( String s : skill_vector.keySet() ) {
            if ( !ground_truth.get(key).containsKey(s) ) {
                if ( DEBUG > 0 ) {
                    System.out.println(String.format("Skill %s not in ground truth. Ignoring...",s));
                }
                continue;
            }
            if ( ground_truth.get(key).get(s) < 0 ) {
                /* Ignore */
                squared_error.put(s,-1.0);
            } else {
                if ( skill_vector.get(s).p2 < 0 )  {
                    squared_error.put(s, Math.pow(ground_truth.get(key).get(s) - 0.5, 2.0));
                } else {
                    squared_error.put(s, Math.pow(ground_truth.get(key).get(s) - skill_vector.get(s).p2, 2.0));
                }
            }
        }
        return squared_error;
    }

//    public double evaluate(String level, String user) {
//        /* TODO: Determine what this is used for and if it is unnecessary, then remove it */
//        /* Used to evaluate the effectiveness of the simulation */
//        /* Compute Mean Squared Error (MSE) between the two distributions? - DONE */
//        /* TODO for future work: Compute KL-Divergence, Total variation distance, and Hellinger distance - ? */
//
//        int lvl_number = Integer.parseInt(level.substring(level.lastIndexOf("l")+1,level.length()));
//
//        String key = user + " " + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);
//
//        if ( user.equals("5-2") || (skills.size() == 0) ) {
//            /* This user doesn't exist, and if a level that wasn't supposed to be played was played. */
//            return 0.0;
//        }
//        double mse = 0.0;
//        for ( String s : skills ) {
//            if ( ground_truth.get(key).get(s) < 0 ) {
//                /* Ignore */
//                continue;
//            }
//            mse += Math.sqrt( Math.pow( ground_truth.get(key).get(s) - skill_vector.get(s).p2, 2.0) );
//        }
//        return mse / (double)skills.size();
//    }

    /* ------------------------------------------ Print Functions ------------------------------------------ */

    public String getSEAsCSVPerWeek(String week, String username) {
        LinkedHashMap<String,Double> mses = evaluateSkillsPerWeek(week,username);

        ArrayList<String> mses_list = new ArrayList<String>();
        for ( String s : mses.keySet() ) {
            if ( mses.get(s) < 0 ) {
                mses_list.add("");
            } else {
                mses_list.add(mses.get(s).toString());
            }
        }
        return String.join(",",mses_list);
    }

    public String getSEAsCSV(String level, String username) {
        LinkedHashMap<String,Double> mses = evaluateSkills(level,username);
        ArrayList<String> mses_list = new ArrayList<String>();
        for ( String s : mses.keySet() ) {
            if ( mses.get(s) < 0 ) {
                mses_list.add("");
            } else {
                mses_list.add(mses.get(s).toString());
            }
        }
        return String.join(",",mses_list);
    }

    public String getGroundTruthAsCSVWeek(String week, String user) {
        ArrayList<String> ground_truth_print = new ArrayList<String>();

        String key = user + "," + week;

        for ( String s : ground_truth.get(key).keySet() ) {
            //System.out.println("s: " + s);
            if ( ground_truth.get(key).get(s) < 0 ) {
                /* Skip this */
                ground_truth_print.add("");
            } else {
                ground_truth_print.add(ground_truth.get(key).get(s).toString());
            }
        }
        return String.join(",",ground_truth_print);
    }

    public String getGroundTruthAsCSV(String level, String user) {
        ArrayList<String> ground_truth_print = new ArrayList<String>();

        int lvl_number = Integer.parseInt(level.substring(level.lastIndexOf("l")+1,level.length()));
        //String key = user + " " + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);
        String key = user + "," + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);

        for ( String s : ground_truth.get(key).keySet() ) {
            //System.out.println("s: " + s);
            if ( ground_truth.get(key).get(s) < 0 ) {
                /* Skip this */
                ground_truth_print.add("");
            } else {
                ground_truth_print.add(ground_truth.get(key).get(s).toString());
            }
        }
        return String.join(",",ground_truth_print);
    }

    public String getPredictionsAsCSV() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skill_vector.keySet() ) {
            String frmt = "";
            if ( skill_vector.get(s).p2 < 0 ) {
                frmt = String.format("%f",0.5);
            } else {
                frmt = String.format("%f",skill_vector.get(s).p2);
            }
            tmp.add(frmt);
        }
        return  String.join(",",tmp);
    }

    public String getRuleEvidenceAsCSV(LinkedHashMap<String,Double> rules) {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : rules.keySet() ) {
            String frmt = String.format("%f",rules.get(s));
            tmp.add(frmt);
        }
        return String.join(",",tmp);
    }

    @Override
    public String toString() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skill_vector.keySet() ) {
            String frmt = String.format("%s (%d,%f)",s,skill_vector.get(s).p1,skill_vector.get(s).p2);
            tmp.add(frmt);
        }
        return  String.join("\n",tmp);
    }
}