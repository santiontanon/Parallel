package pmexperiments;

import playermodeling.AbstractPlayerModeler;
import playermodeling.SkillAnalyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by pavankantharaju on 3/5/18.
 */
public class SkillAnalyzerExperiment extends SkillAnalyzer {

    public LinkedHashMap< String, LinkedHashMap< String, Double > > ground_truth;

    public SkillAnalyzerExperiment(String ground_truth_file) {
        super();
        ground_truth = new LinkedHashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(AbstractPlayerModeler.PLAYER_MODELING_DATA_DIR + ground_truth_file));
            String line = br.readLine();
            String[] skill_list = line.split(",");
            while ((line = br.readLine()) != null) {
                String[] line_split = line.split(",");
                LinkedHashMap<String, Double> tmp = new LinkedHashMap<String, Double>();
                for ( String skill : skills ) {
                    tmp.put(skill, -1.0);
                }
                for (int i = 2; i < skill_list.length; i++) {
                    String skill_in_ground_truth = skill_list[i].trim();
                    double val;
                    if (line_split[i].equals("X") || line_split[i].equals("Y")) {
                        val = -1;
                    } else {
                        val = Double.parseDouble(line_split[i].trim());
                    }
                    tmp.replace(skill_in_ground_truth, val);
                }
//                for (int i = 2; i < skill_list.length; i++) {
//                    double val;
//                    if (line_split[i].equals("X") || line_split[i].equals("Y")) {
//                        val = -1;
//                    } else {
//                        val = Double.parseDouble(line_split[i].trim());
//                    }
//                    tmp.put(skill_list[i].trim(), val);
//                }
                ground_truth.put(line_split[0].trim() + "," + line_split[1].trim(), tmp);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ------------------------------------------ Mean-Squared Error Evaluation Methods ------------------------------------------ */

    public LinkedHashMap<String, Double> evaluateSkills(String level, String user) {
        LinkedHashMap< String, Double> squared_error = new LinkedHashMap< String, Double> ();
        for ( String s : skill_vector.keySet() ) {
            squared_error.put(s,-1.0);
        }

        String key = user + "," + level;

        if ( user.equals("5-2") || (skills_specific_to_level.size() == 0) ) {
            /* This user doesn't exist or if a level that wasn't supposed to be played was played. */
            return squared_error;
        }

        for ( String s : skill_vector.keySet() ) {
            if ( !ground_truth.get(key).containsKey(s) ) {
                System.out.println(String.format("Skill %s not in ground truth.", s));
                System.out.println("There is a discrepancy between the ground truth and the skill vector...exiting...");
                System.exit(-1);
            }
            if ( ground_truth.get(key).get(s) < 0 ) {
                /* Ignore */
                squared_error.replace(s,-1.0);
            } else {
                if ( skill_vector.get(s).p2 < 0 )  {
                    squared_error.replace(s, Math.pow(ground_truth.get(key).get(s) - 0.5, 2.0));
                } else {
                    squared_error.replace(s, Math.pow(ground_truth.get(key).get(s) - skill_vector.get(s).p2, 2.0));
                }
            }
        }

        return squared_error;
    }

    /* ------------------------------------------ Print Functions ------------------------------------------ */

    public String getSquaredErrorCSV(String level, String username) {
        LinkedHashMap<String,Double> squared_error = evaluateSkills(level,username);
        ArrayList<String> mses_list = new ArrayList<String>();
        for ( String s : squared_error.keySet() ) {
            if ( squared_error.get(s) < 0 ) {
                mses_list.add("");
            } else {
                mses_list.add(squared_error.get(s).toString());
            }
        }
        return String.join(",",mses_list);
    }

    public String getGroundTruthCSV(String level, String user) {
        ArrayList<String> ground_truth_print = new ArrayList<String>();

        String key = user + "," + level;

        for ( String s : ground_truth.get(key).keySet() ) {
            if ( ground_truth.get(key).get(s) < 0 ) {
                /* Skip this */
                ground_truth_print.add("");
            } else {
                ground_truth_print.add(ground_truth.get(key).get(s).toString());
            }
        }
        return String.join(",",ground_truth_print);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}