package playermodeling;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.core.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerModelingEngine extends PlayerModeler {

    public static final String CRITICAL_SECTION_PATH = PLAYER_MODELING_DATA_DIR + "critical_sections/";
    public static String SKILL_VECTOR_FILE = "currentParameters.txt";

    public String skillVectorFilepath;
    public String user;
    public String level;

    public SkillAnalyzer skillAnalyzer;
    public TelemetryUtils telemetryUtils;
    public FeatureExtraction featureExtraction;
    public MEExecutionAnalyzer meExecutionAnalyzer;

    public PlayerModelingEngine() {
        super(new BayesNet(), 10, 1);
        skillVectorFilepath = SKILL_VECTOR_FILE;
        training_dataset = new Instances("Training_dataset", attributes, 10);
        training_dataset.setClassIndex(attributes.size() - 1);
        user = "";
        level = "";
        skillAnalyzer = new SkillAnalyzer(skillVectorFilepath);
        telemetryUtils = new TelemetryUtils();
        featureExtraction = new FeatureExtraction(skillAnalyzer);
        meExecutionAnalyzer = new MEExecutionAnalyzer(skillAnalyzer, CRITICAL_SECTION_PATH);
    }

    public PlayerModelingEngine(Classifier cls, double interval_, int u_technique_flag, String skillVectorFilepath_, String level_, String user_) {
        super(cls, interval_, u_technique_flag);
        training_dataset = new Instances("Training_dataset", attributes, 10);
        training_dataset.setClassIndex(attributes.size() - 1);
        user = user_;
        level = level_;
        skillVectorFilepath = skillVectorFilepath_ + "/" + SKILL_VECTOR_FILE;
        skillAnalyzer = new SkillAnalyzer(skillVectorFilepath);
        telemetryUtils = new TelemetryUtils();
        featureExtraction = new FeatureExtraction(skillAnalyzer);
        meExecutionAnalyzer = new MEExecutionAnalyzer(skillAnalyzer, CRITICAL_SECTION_PATH);
    }

    @Override
    public void execute() {}

    public void executePM(String telemetryFilename, String meExecutionFilenames) {
        /*
            telemetryFilename - Telemetry file after submitting level data
            meExecutionFilename - Model Engine Execution file
         */

        ArrayList<String> telemetryData = telemetryUtils.readTelemetryFile(telemetryFilename);
        PersistentData persistentData = meExecutionAnalyzer.analyzeMEExecution(meExecutionFilenames);

        String firstLineInLog = telemetryData.get(0);
        double startTime = Double.parseDouble(firstLineInLog.split("\t")[3]);

        String date_ = firstLineInLog.split("\t")[0];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);
        persistentData.persistent_data.put("start_time", ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 ));

        String lastLineInLog = telemetryData.get(telemetryData.size() - 1);
        double endTime = Double.parseDouble(lastLineInLog.split("\t")[3]);

        double t1 = startTime;
        double t2 = startTime + interval;

        if (DEBUG > 0) {
            System.out.println("Start time: " + startTime + ", end time: " + endTime);
        }

        if (!skillAnalyzer.readSkillsForLevel(PLAYER_MODELING_DATA_DIR + "/level_skills/", level)) {
            return;
        }
        while (t1 < endTime) {
            ArrayList<String> telemetryDataInInterval = telemetryUtils.getTelemetryInInterval(telemetryData, t1, t2);
            HashMap<String, Double> featureVector = featureExtraction.extractFeatureVectorPM(telemetryDataInInterval, persistentData);

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
                    skillAnalyzer.updateSkillVectorUsingMachineLearning(training_dataset.classAttribute().value((int) classification));
                case 1:
                    /* Only machine learning */
                    classification = classifyInstance(featureVector);
                    skillAnalyzer.updateSkillVectorUsingMachineLearning(training_dataset.classAttribute().value((int) classification));
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
                skillAnalyzer.updateSkillVectorUsingRules();
            case 1:
                break;
            case 2:
                /* Only rule_evidence */
                skillAnalyzer.updateSkillVectorUsingRules();
                break;
        }
        skillAnalyzer.writeSkillVectorToFile(skillVectorFilepath);
    }
}
