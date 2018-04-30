package playermodeling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * Created by pavankantharaju on 3/5/18.
 */
public class SkillAnalysis {

    public LinkedHashMap< String,Pair<Integer,Double> > skill_map;
    public LinkedHashMap< String, LinkedHashMap< String, Double > > actual_skill_map;
    public LinkedHashMap<String,Boolean> is_finalized;   /* NOTE: Only used for updating skill vector using rules */

    /* Level-specific skills */
    public ArrayList<String> skills;


    /* Rule data structures */
    private int update_flag;
    public LinkedHashMap<String,Double> rules;

    public SkillAnalysis(int u_flag) {
        skill_map = new LinkedHashMap< String, Pair<Integer,Double> >();
        actual_skill_map = new  LinkedHashMap< String, LinkedHashMap< String, Double > >();
        is_finalized = new LinkedHashMap<String,Boolean>();

        skills = new ArrayList<String>();
        /* Read in players and their skill level */
        try {
            BufferedReader br = new BufferedReader(new FileReader("player_skills_4.csv"));
            String line = br.readLine();
            String [] skills = line.split(",");
            for ( int i = 1; i < skills.length; i++ ) {
                skill_map.put(skills[i].trim(), new Pair< Integer, Double>(0,-1.0) );
                is_finalized.put(skills[i].trim(),false);
            }
            while ((line = br.readLine()) != null) {
                String [] line_split = line.split(",");
                LinkedHashMap< String, Double > tmp = new LinkedHashMap<String, Double>();
                for ( int i = 1; i < skills.length; i++ ) {
                    double val = 0.0;
                    if ( line_split[i].equals("X") || line_split[i].equals("Y") ) {
                        val = -1;
                    } else {
                        val = Double.parseDouble(line_split[i].trim());
                    }
                    tmp.put(skills[i].trim(),val);
                }
                actual_skill_map.put( line_split[0].trim() , tmp );
            }
            br.close();

            rules = new LinkedHashMap<String,Double>();
            for ( String s : skill_map.keySet() ) {
                rules.put(s,-1.0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean readSkillList(String level_name) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("skills/"+level_name));
            String line = "";
            while ((line = br.readLine()) != null) {
                skills.add(line.trim());
            }
            return true;
        } catch (IOException e) {
            //System.out.println("Level " + level_name + " doesn't exist...");
            return false;
        }
    }

    public void softReset() {
        skills.clear();
    }

    public void hardReset() {
        for( String s : skill_map.keySet() ) {
            Pair<Integer, Double> tmp = skill_map.get(s);
            tmp.p1 = 0;
            tmp.p2 = -1.0;
            skill_map.replace(s,tmp);
        }
    }

    public void updateRules(String skill) {
        switch ( update_flag ) {
            case 0:
                rules.replace(skill,rules.get(skill) + 1.0);
                break;
            case 1:
                rules.replace(skill,1.0);
                break;
        }
    }

    public void resetRules() {
        for ( String s : rules.keySet() ) {
            rules.replace(s,0.0);
        }
    }

    public void updateSkillsBasedOnClassification(String classification) {
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

            if ( is_finalized.get(s) ) {
                /* Don't touch this value */
                continue;
            }

            Pair<Integer, Double> tmp = skill_map.get(s);
            if ( tmp.p2 < 0 ) {
                tmp.p2 = val;
            } else {
                tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + val )/(tmp.p1+1);
            }
            tmp.p1++;

            skill_map.replace(s,tmp);
        }
    }


    public void updateSkillsBasedOnRules( int updates_flag ) {
        for ( String s : skill_map.keySet() ) {
            if ( rules.get(s) > 0.0 ) {
                Pair<Integer, Double> tmp = skill_map.get(s);
                switch ( updates_flag ) {
                    case 0:
                        tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + rules.get(s) )/ ( tmp.p1+rules.get(s) );
                        tmp.p1 += rules.get(s).intValue();
                        break;
                    case 1:
                        tmp.p2 = rules.get(s);
                        tmp.p1++;
                        is_finalized.replace(s,true);
                        break;
                }
                skill_map.replace(s,tmp);
            }
        }
    }

//    public void updateSkillsBasedOnRules( LinkedHashMap<String,Double> rules, int updates_flag ) {
//        for ( String s : skill_map.keySet() ) {
//            if ( rules.get(s) > 0.0 ) {
//                Pair<Integer, Double> tmp = skill_map.get(s);
//                switch ( updates_flag ) {
//                    case 0:
//                        tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + rules.get(s) )/ ( tmp.p1+rules.get(s) );
//                        tmp.p1 += rules.get(s).intValue();
//                        break;
//                    case 1:
//                        tmp.p2 = rules.get(s);
//                        tmp.p1++;
//                        is_finalized.replace(s,true);
//                        break;
//                }
//                skill_map.replace(s,tmp);
//            }
//        }
//    }

    public LinkedHashMap<String, Double> evaluateSkills(String level, String user) {

        LinkedHashMap< String, Double> mses = new LinkedHashMap< String, Double> ();

        int lvl_number = Integer.parseInt(level.substring(level.lastIndexOf("l")+1,level.length()));

        String key = user + " " + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);

        if ( user.equals("5-2") || (skills.size() == 0) ) {
            /* This user doesn't exist, and if a level that wasn't supposed to be played was played. */
            return mses;
        }

        for ( String s : skill_map.keySet() ) {
            //System.out.println("Key,skill: " + s + "," + actual_skill_map.get(key).get(s));
            if ( actual_skill_map.get(key).get(s) < 0 ) {
                /* Ignore */
                mses.put(s,-1.0);
            } else {
                if ( skill_map.get(s).p2 < 0 )  {
                    mses.put(s, Math.pow(actual_skill_map.get(key).get(s) - 0.5, 2.0));
                } else {
                    mses.put(s, Math.pow(actual_skill_map.get(key).get(s) - skill_map.get(s).p2, 2.0));
                }
            }
        }

        return mses;
    }

    public double evaluate(String level, String user) {
        /* Used to evaluate the effectiveness of the simulation */
        /* Compute Mean Squared Error (MSE) between the two distributions? - DONE */
        /* Compute KL-Divergence, Total variation distance, and Hellinger distance - ? */

        int lvl_number = Integer.parseInt(level.substring(level.lastIndexOf("l")+1,level.length()));

        String key = user + " " + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);

        if ( user.equals("5-2") || (skills.size() == 0) ) {
            /* This user doesn't exist, and if a level that wasn't supposed to be played was played. */
            return 0.0;
        }
        double mse = 0.0;
        for ( String s : skills ) {
            if ( actual_skill_map.get(key).get(s) < 0 ) {
                /* Ignore */
                continue;
            }
            mse += Math.sqrt( Math.pow( actual_skill_map.get(key).get(s) - skill_map.get(s).p2, 2.0) );
        }
        return mse / (double)skills.size();
    }

    public String getMSEAsCSV(String level, String username) {
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

    public String getGroundTruthAsCSV(String level, String user) {
        ArrayList<String> ground_truth = new ArrayList<String>();

        int lvl_number = Integer.parseInt(level.substring(level.lastIndexOf("l")+1,level.length()));
        String key = user + " " + level.substring(0,level.lastIndexOf("l")+1) + Integer.toString(lvl_number);

        for ( String s : actual_skill_map.get(key).keySet() ) {
            //System.out.println("s: " + s);
            if ( actual_skill_map.get(key).get(s) < 0 ) {
                /* Skip this */
                ground_truth.add("");
            } else {
                ground_truth.add(actual_skill_map.get(key).get(s).toString());
            }
        }
        return String.join(",",ground_truth);
    }

    public String getPredictionsAsCSV() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skill_map.keySet() ) {
            String frmt = "";
            if ( skill_map.get(s).p2 < 0 ) {
                frmt = String.format("%f",0.5);
            } else {
                frmt = String.format("%f",skill_map.get(s).p2);
            }
            tmp.add(frmt);
        }
        return  String.join(",",tmp);
    }

    public String getRulesAsCSV(LinkedHashMap<String,Double> rules) {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : rules.keySet() ) {
            String frmt = "";
            if ( is_finalized.get(s) ) {
                frmt = String.format("%f",1.0);
            } else {
                frmt = String.format("%f",rules.get(s));
            }
            tmp.add(frmt);
        }
        return String.join(",",tmp);
    }

    @Override
    public String toString() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skill_map.keySet() ) {
            String frmt = String.format("%s (%d,%f)",s,skill_map.get(s).p1,skill_map.get(s).p2);
            tmp.add(frmt);
        }
        return  String.join("\n",tmp);
    }
}
