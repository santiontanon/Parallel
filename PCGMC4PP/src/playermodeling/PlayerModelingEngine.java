package playermodeling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.core.*;

import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayerModelingEngine extends AbstractPlayerModeler {

    private static final Logger logger = LogManager.getLogger(PlayerModelingEngine.class);

    public String skillVectorFilepath;
    public String criticalSectionRootPath;
    public String playerModelingDirectory;
    public String user;
    public String level;

    public SkillAnalyzer skillAnalyzer;
    public TelemetryAnalyzer telemetryAnalyzer;

    /* NOTE: These are passed into the Server Interface for bookkeeping of the generated skill vectors */
    public String logStartTimeStamp;
    public String logEndTimeStamp;

    public PlayerModelingEngine() {
        super(new BayesNet(), 10, 1);
        user = "";
        level = "";
        skillVectorFilepath = "";
        criticalSectionRootPath = "";
        playerModelingDirectory = PLAYER_MODELING_DATA_DIR;

        trainingDataset = new Instances("Training_dataset", attributes, 10);
        trainingDataset.setClassIndex(attributes.size() - 1);

        skillAnalyzer = new SkillAnalyzer(skillVectorFilepath, playerModelingDirectory);
        MEExecutionAnalyzer.setColorMap();
        telemetryAnalyzer = new TelemetryAnalyzer();
    }

    public PlayerModelingEngine(Classifier cls, double interval_, int u_technique_flag, String skillVectorFilepath_, String playerModelingDirectory_,
                                String level_, String user_) {
        super(cls, interval_, u_technique_flag);
        user = user_;
        level = level_;
        skillVectorFilepath = skillVectorFilepath_;
        playerModelingDirectory = playerModelingDirectory_ + File.separator + PLAYER_MODELING_DATA_DIR;
        criticalSectionRootPath =  playerModelingDirectory + "critical_sections" + File.separator;

        logger.info("Setting player modeling directory path to: " + playerModelingDirectory);
        logger.info("Setting critical section path to: " + criticalSectionRootPath);

        trainingDataset = new Instances("Training_dataset", attributes, 10);
        trainingDataset.setClassIndex(attributes.size() - 1);

        skillAnalyzer = new SkillAnalyzer(skillVectorFilepath, playerModelingDirectory);
        MEExecutionAnalyzer.setColorMap();
        telemetryAnalyzer = new TelemetryAnalyzer();
    }

    @Override
    public void execute() {}

    public void executePM(String telemetryFilename, String meExecutionFilenames) {
        /*
            telemetryFilename - Telemetry file after submitting level data
            meExecutionFilename - Model Engine Execution file
         */

        logger.info("--------------------- Executing Player Modeling! ---------------------");

        ArrayList<String> telemetryData = telemetryAnalyzer.readTelemetryFile(telemetryFilename);
        LevelData levelData = MEExecutionAnalyzer.analyzeMEExecution(meExecutionFilenames, criticalSectionRootPath, skillAnalyzer);

        String firstLineInLog = telemetryData.get(0);
        double startTime = Double.parseDouble(firstLineInLog.split("\t")[3]);
        logStartTimeStamp = firstLineInLog.split("\t")[0];

        String date_ = firstLineInLog.split("\t")[0];
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);
        levelData.data.put("start_time", ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 ));

        String lastLineInLog = telemetryData.get(telemetryData.size() - 1);
        double endTime = Double.parseDouble(lastLineInLog.split("\t")[3]);
        logEndTimeStamp = lastLineInLog.split("\t")[0];

        double t1 = startTime;
        double t2 = startTime + interval;

        logger.info("Start time: " + startTime + ", end time: " + endTime);
        logger.info("Reading in skills for level...");

        if (!skillAnalyzer.readSkillsForLevel(levelData)) {
            logger.warn(String.format("Level %s does not have any skills associated with it. Player Modeling Complete!", level));
            return;
        }
        logger.info("Updating skill vector....");

        while (t1 < endTime) {

            logger.trace(String.format("Start of time interval: %f, End of time interval: %f", t1, t2));

            ArrayList<String> telemetryDataInInterval = telemetryAnalyzer.getTelemetryInInterval(telemetryData, t1, t2);
            HashMap<String, Double> featureVector = FeatureExtractor.buildFeatureVector(telemetryDataInInterval, levelData, skillAnalyzer);

            t1 += interval / 2.0;
            t2 = t1 + interval;

            /* If feature vector is empty, then either there's no telemetry in the interval OR we have insufficient data to create the feature vector */
            if (featureVector.size() == 0) {
                continue;
            }

            logger.trace("Updating skill vector using classification from machine learning!");

            switch (skillVectorUpdateTechniqueFlag) {
                case 0:
                    /* Both Machine Learning and Rules */
                    double classification = classifyInstance(featureVector);
                    skillAnalyzer.updateSkillVectorUsingMachineLearning(trainingDataset.classAttribute().value((int) classification));
                case 1:
                    /* Only machine learning */
                    classification = classifyInstance(featureVector);
                    skillAnalyzer.updateSkillVectorUsingMachineLearning(trainingDataset.classAttribute().value((int) classification));
                    break;
                case 2:
                    /* Only rule evidence */
                    break;
            }
        }

        logger.trace("Updating skill vector using evidence from rules!");

        /* Update from ruleEvidence after completing the level */
        switch (skillVectorUpdateTechniqueFlag) {
            case 0:
                /* Both Machine Learning and Rules */
                skillAnalyzer.updateSkillVectorUsingRules();
            case 1:
                break;
            case 2:
                /* Only rule evidence */
                skillAnalyzer.updateSkillVectorUsingRules();
                break;
        }
        logger.info("Completed update of skill vector.");

        skillAnalyzer.writeSkillVectorToFile(skillVectorFilepath);
        logger.info("--------------------- Player Modeling Complete! ---------------------");
    }
}
