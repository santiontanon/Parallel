package playermodeling;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pmutils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by pavankantharaju on 3/5/18.
 */
public class SkillAnalyzer {

    protected ArrayList<String> skills;
    public ArrayList<String> skillsSpecificToLevel;
    public LinkedHashMap< String, Pair<Integer, Double>> skillVector;
    public LinkedHashMap<String, Double> ruleEvidence;
    private static final Logger logger = LogManager.getLogger(SkillAnalyzer.class);

    public SkillAnalyzer(String skillVectorFilename, String playerModelingDirectory) {
        skillVector = new LinkedHashMap<>();
        skillsSpecificToLevel = new ArrayList<>();
        skills = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(playerModelingDirectory + "skills.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String skill = line.trim();
                skills.add(skill);
                skillVector.put(skill, new Pair< Integer, Double >(0,-1.0));
            }
            br.close();
            ruleEvidence = new LinkedHashMap<String, Double>();
            for ( String s : skillVector.keySet() ) {
                ruleEvidence.put(s,0.0);
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
                    skillVector.put(skill, new Pair< Integer, Double >(0, skillValue));
                } else {
                    skillVector.put(skill, new Pair< Integer, Double >(evidenceValue, skillValue));
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SkillAnalyzer(String playerModelingDirectory) {
        skillVector = new LinkedHashMap< String, Pair<Integer,Double> >();
        skillsSpecificToLevel = new ArrayList<String>();
        skills = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(playerModelingDirectory + "skills.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String skill = line.trim();
                skills.add(skill);
                skillVector.put(skill, new Pair< Integer, Double >(0,-1.0));
            }
            br.close();
            ruleEvidence = new LinkedHashMap<String,Double>();
            for ( String s : skillVector.keySet() ) {
                ruleEvidence.put(s,0.0);
            }
        } catch (IOException e) {
            logger.fatal("There is an error with reading in " + playerModelingDirectory + "skills.txt");
            logger.catching(Level.FATAL, e);
            System.exit(1);
        }
    }

    public SkillAnalyzer() {
        skillVector = new LinkedHashMap< String, Pair<Integer,Double> >();
        skillsSpecificToLevel = new ArrayList<String>();
        skills = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(AbstractPlayerModeler.PLAYER_MODELING_DATA_DIR + "skills.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String skill = line.trim();
                skills.add(skill);
                skillVector.put(skill, new Pair< Integer, Double >(0,-1.0));
            }
            br.close();
            ruleEvidence = new LinkedHashMap<String,Double>();
            for ( String s : skillVector.keySet() ) {
                ruleEvidence.put(s,0.0);
            }
        } catch (IOException e) {
            logger.fatal("There is an error with reading in " + AbstractPlayerModeler.PLAYER_MODELING_DATA_DIR + "skills.txt");
            logger.catching(Level.FATAL, e);
            System.exit(1);
        }
    }

    /* TODO: Future work: Write a function to read in a skill vector from a file - Why? */

    public void resetSkillsPerLevel() {
        logger.info("Resetting list of skills for each level");
        skillsSpecificToLevel.clear();
    }

    public void resetSkillVector() {
        logger.info("Resetting skill vector");
        for( String s : skillVector.keySet() ) {
            Pair<Integer, Double> tmp = skillVector.get(s);
            tmp.p1 = 0;
            tmp.p2 = -1.0;
            skillVector.replace(s,tmp);
        }
    }

    public void resetRuleEvidence() {
        logger.info("Resetting evidence for each rule");
        for ( String s : ruleEvidence.keySet() ) {
            ruleEvidence.replace(s,0.0);
        }
    }

    public boolean readSkillsForLevel(String path, String levelName) {
        logger.info(String.format("Getting skills for level %s", levelName));

        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "/" + levelName));
            String line;
            while ((line = br.readLine()) != null) {
                if ( !skills.contains(line.trim()) ) {
                    logger.warn(String.format("Skill %s not correctly worded or doesn't exist in file %s",line.trim(), path + "/" + levelName));
                    System.exit(1);
                }
                skillsSpecificToLevel.add(line.trim());
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean readSkillsForLevel(LevelData levelData) {
        logger.info("Reading in skills specific to level");

        ArrayList<String> skillsPerLevel = (ArrayList<String>) levelData.data.get("skills_per_level");
        if ( skillsPerLevel.size() == 0 ) {
            return false;
        }
        for ( String s : skillsPerLevel ) {
            skillsSpecificToLevel.add(s);
        }
        return true;
    }

    public void updateRuleEvidence(String skill) {
        logger.info("Adding evidence for skill: " + skill);
        ruleEvidence.replace(skill, ruleEvidence.get(skill) + 1.0);
    }

    public void updateSkillVectorUsingMachineLearning(String classification) {
        logger.info("Updating skill vector with classification: " + classification);

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

        for ( String s : skillsSpecificToLevel) {
            if ( !skillVector.containsKey(s) ) {
                logger.warn("Skill not found (or doesn't have ground truth). Skipping..." + s);
                continue;
            }

            Pair<Integer, Double> tmp = skillVector.get(s);
            if ( tmp.p2 < 0 ) {
                tmp.p2 = val;
            } else {
                tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + val )/(tmp.p1+1);
            }
            tmp.p1++;

            skillVector.replace(s,tmp);
        }
    }

    public void updateSkillVectorUsingRules() {
        for ( String s : skillsSpecificToLevel ) {
            if ( !skillVector.containsKey(s) ) {
                logger.warn("Skill not found (or doesn't have ground truth). Skipping..." + s);
                continue;
            }
            if ( ruleEvidence.get(s) > 0.0 ) {
                Pair<Integer, Double> tmp = skillVector.get(s);
                tmp.p2 =  ( ( tmp.p2*tmp.p1 ) + ruleEvidence.get(s) )/ ( tmp.p1+ ruleEvidence.get(s) );
                tmp.p1 += ruleEvidence.get(s).intValue();
                skillVector.replace(s,tmp);
            }
        }
    }

    /* ------------------------------------------ Print Functions ------------------------------------------ */
    public String printSkillVector() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skillVector.keySet() ) {
            String frmt;
            if ( skillVector.get(s).p2 < 0 ) {
                frmt = String.format("%f",0.5);
            } else {
                frmt = String.format("%f", skillVector.get(s).p2);
            }
            tmp.add(frmt);
        }
        return  String.join(",",tmp);
    }

    public String printRuleEvidence() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : ruleEvidence.keySet() ) {
            String frmt = String.format("%f", ruleEvidence.get(s));
            tmp.add(frmt);
        }
        return String.join(",",tmp);
    }

    public void writeSkillVectorToFile(String filename) {
        /* Write skill vector as CSV (skill name, value) */
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            for ( String s : skillVector.keySet() ) {
                String format;
                if ( skillVector.get(s).p2 < 0 ) {
                    format = String.format("%s,%f,%d", s, 0.5, skillVector.get(s).p1);
                } else {
                    format = String.format("%s,%f,%d", s, skillVector.get(s).p2, skillVector.get(s).p1);
                }
                writer.println(format);
            }
            writer.flush();
            writer.close();
        } catch ( IOException e ) {
            logger.fatal("Unable to write skill vector to file: " + filename);
            logger.catching(Level.FATAL, e);
            System.exit(1);
        }
    }

    @Override
    public String toString() {
        ArrayList<String> tmp = new ArrayList<String>();
        for ( String s : skillVector.keySet() ) {
            String frmt = String.format("%s (%d,%f)",s, skillVector.get(s).p1, skillVector.get(s).p2);
            tmp.add(frmt);
        }
        return  String.join("\n",tmp);
    }
}