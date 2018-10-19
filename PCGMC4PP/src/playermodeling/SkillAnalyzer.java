package playermodeling;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by pavankantharaju on 3/5/18.
 */
public class SkillAnalyzer {

    protected ArrayList<String> skills;
    public ArrayList<String> skills_specific_to_level;
    public LinkedHashMap< String, Pair<Integer, Double> > skill_vector;
    public LinkedHashMap<String, Double> rule_evidence;
    public boolean debug;

    public SkillAnalyzer(String skillVectorFilename, String playerModelingDirectory) {
        skill_vector = new LinkedHashMap<>();
        skills_specific_to_level = new ArrayList<>();
        skills = new ArrayList<>();
        debug = false;

        try {
            BufferedReader br = new BufferedReader(new FileReader(playerModelingDirectory + "skills.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String skill = line.trim();
                skills.add(skill);
                skill_vector.put(skill, new Pair< Integer, Double >(0,-1.0));
            }
            br.close();
            rule_evidence = new LinkedHashMap<String, Double>();
            for ( String s : skill_vector.keySet() ) {
                rule_evidence.put(s,0.0);
            }
            br = new BufferedReader(new FileReader(skillVectorFilename));
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                String skillValuePair = line.trim();
                String [] skillValuePairSplit = skillValuePair.split(",");
                String skill = skillValuePairSplit[0];
                double skillValue = Double.parseDouble(skillValuePairSplit[1]);
                int evidenceValue = Integer.parseInt(skillValuePairSplit[2]);
                if ( evidenceValue == 0 ) {
                    skill_vector.put(skill, new Pair< Integer, Double >(0, skillValue));
                } else {
                    skill_vector.put(skill, new Pair< Integer, Double >(evidenceValue, skillValue));
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SkillAnalyzer(String skillVectorFilename, String playerModelingDirectory, boolean debug_) {
        skill_vector = new LinkedHashMap<>();
        skills_specific_to_level = new ArrayList<>();
        skills = new ArrayList<>();
        debug = debug_;

        try {
            BufferedReader br = new BufferedReader(new FileReader(playerModelingDirectory + "skills.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String skill = line.trim();
                skills.add(skill);
                skill_vector.put(skill, new Pair< Integer, Double >(0,-1.0));
            }
            br.close();
            rule_evidence = new LinkedHashMap<String, Double>();
            for ( String s : skill_vector.keySet() ) {
                rule_evidence.put(s,0.0);
            }
            br = new BufferedReader(new FileReader(skillVectorFilename));
            while ((line = br.readLine()) != null) {
                if (line.length() == 0) {
                    continue;
                }
                String skillValuePair = line.trim();
                String [] skillValuePairSplit = skillValuePair.split(",");
                String skill = skillValuePairSplit[0];
                double skillValue = Double.parseDouble(skillValuePairSplit[1]);
                int evidenceValue = Integer.parseInt(skillValuePairSplit[2]);
                if ( evidenceValue == 0 ) {
                    skill_vector.put(skill, new Pair< Integer, Double >(0, skillValue));
                } else {
                    skill_vector.put(skill, new Pair< Integer, Double >(evidenceValue, skillValue));
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SkillAnalyzer() {
        skill_vector = new LinkedHashMap< String, Pair<Integer,Double> >();
        skills_specific_to_level = new ArrayList<String>();
        skills = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(PlayerModeler.PLAYER_MODELING_DATA_DIR + "skills.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String skill = line.trim();
                skills.add(skill);
                skill_vector.put(skill, new Pair< Integer, Double >(0,-1.0));
            }
            br.close();
            rule_evidence = new LinkedHashMap<String,Double>();
            for ( String s : skill_vector.keySet() ) {
                rule_evidence.put(s,0.0);
            }
        } catch (IOException e) {
            System.err.println("There is an error with reading in " + PlayerModeler.PLAYER_MODELING_DATA_DIR + "skills.txt");
            e.printStackTrace();
        }
    }

    /* TODO: Future work: Write a function to read in a skill vector from a file */

    public void resetSkillsPerLevel() {
        if (debug) {
            System.out.println("Resetting list of skills for each level");
        }
        skills_specific_to_level.clear();
    }

    public void resetSkillVector() {
        if (debug) {
            System.out.println("Resetting skill vector");
        }
        for( String s : skill_vector.keySet() ) {
            Pair<Integer, Double> tmp = skill_vector.get(s);
            tmp.p1 = 0;
            tmp.p2 = -1.0;
            skill_vector.replace(s,tmp);
        }
    }

    public void resetRuleEvidence() {
        if (debug) {
            System.out.println("Resetting evidence for each rule");
        }
        for ( String s : rule_evidence.keySet() ) {
            rule_evidence.replace(s,0.0);
        }
    }

    public boolean readSkillsForLevel(String path, String level_name) {
        if (debug) {
            System.out.println(String.format("Getting skills for level %s", level_name));
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "/" + level_name));
            String line;
            while ((line = br.readLine()) != null) {
                if ( !skills.contains(line.trim()) ) {
                    System.out.println(String.format("Skill %s not correctly worded or doesn't exist in file %s",line.trim(), path + "/" + level_name));
                    System.exit(1);
                }
                skills_specific_to_level.add(line.trim());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean readSkillsForLevel(PersistentData persistentData) {
        if (debug) {
            System.out.println(String.format("Getting skills for PCG level."));
        }
        ArrayList<String> skillsPerLevel = (ArrayList<String>)persistentData.persistent_data.get("skills_per_level");
        if ( skillsPerLevel.size() == 0 ) {
            return false;
        }
        for ( String s : skillsPerLevel ) {
            skills_specific_to_level.add(s);
        }
        return true;
    }

    public void updateRuleEvidence(String skill) {
        if (debug) {
            System.out.println("Adding evidence for skill: " + skill);
        }
        rule_evidence.replace(skill, rule_evidence.get(skill) + 1.0);
    }

    public void updateSkillVectorUsingMachineLearning(String classification) {
        if (debug) {
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

        for ( String s : skills_specific_to_level ) {
            if ( !skill_vector.containsKey(s) ) {
                if ( PlayerModelingEngine.debug ) {
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

    public void updateSkillVectorUsingRules() {
        for ( String s : skills_specific_to_level ) {
            if ( !skill_vector.containsKey(s) ) {
                if (PlayerModelingEngine.debug ) {
                    System.out.println("Skill not found (or doesn't have ground truth). Skipping..." + s);
                }
                continue;
            }
            if ( rule_evidence.get(s) > 0.0 ) {
                Pair<Integer, Double> tmp = skill_vector.get(s);
                tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + rule_evidence.get(s) )/ ( tmp.p1+ rule_evidence.get(s) );
                tmp.p1 += rule_evidence.get(s).intValue();
                skill_vector.replace(s,tmp);
            }
        }
    }

    /* ------------------------------------------ Print Functions ------------------------------------------ */
    public String printSkillVector() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skill_vector.keySet() ) {
            String frmt;
            if ( skill_vector.get(s).p2 < 0 ) {
                frmt = String.format("%f",0.5);
            } else {
                frmt = String.format("%f",skill_vector.get(s).p2);
            }
            tmp.add(frmt);
        }
        return  String.join(",",tmp);
    }

    public String printRuleEvidence() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : rule_evidence.keySet() ) {
            String frmt = String.format("%f",rule_evidence.get(s));
            tmp.add(frmt);
        }
        return String.join(",",tmp);
    }

    public void writeSkillVectorToFile(String filename) {
        /* Write skill vector as CSV (skill name, value) */
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            for ( String s : skill_vector.keySet() ) {
                String format;
                if ( skill_vector.get(s).p2 < 0 ) {
                    format = String.format("%s,%f,%d", s, 0.5, skill_vector.get(s).p1);
                } else {
                    format = String.format("%s,%f,%d", s, skill_vector.get(s).p2, skill_vector.get(s).p1);
                }
                writer.println(format);
            }
            writer.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
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