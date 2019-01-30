package playermodeling;

import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.ServerInterface;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static final String TRAINING_MODEL_FILEPATH = "pmfiles/classifier-model.model";
    public static final double interval = 5;
    public static final int skillVectorUpdateTechniqueFlag = 0;
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static final Classifier cls = new NaiveBayes();

    public static void main(String [] args) throws Exception {
        Options cliOptions = new Options();
        cliOptions.addOption("mepath",true,"Model Engine Execution Filepath");
        cliOptions.addOption("pmdir",true,"Player Modeling Files Directory");
        cliOptions.addOption("telemetrypath",true,"Telemetry Filepath");
        cliOptions.addOption("parameterpath",true,"Parameter Filepath");
        cliOptions.addOption("level", true, "Level of Game");
        cliOptions.addOption("user", true, "Player");
        cliOptions.addOption("hostname",true,"Hostname of server");
        cliOptions.addOption("port",true,"Port number of server");

        String meExecutionFilepath = "";
        String telemetryFilepath = "";
        String parameterFilepath = "";
        String pmdir = "";
        String level = "";
        String user = "";
        String hostname = "129.25.141.236";
        int port = 8787;
        boolean debug = false;

        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( cliOptions, args );
            if ( line.hasOption("mepath") ) {
                meExecutionFilepath = line.getOptionValue("mepath");
            }
            if ( line.hasOption("pmdir") ) {
                pmdir = line.getOptionValue("pmdir");
            }
            if ( line.hasOption("telemetrypath") ) {
                telemetryFilepath = line.getOptionValue("telemetrypath");
            }
            if ( line.hasOption("parameterpath") ) {
                parameterFilepath = line.getOptionValue("parameterpath");
            }
            if ( line.hasOption("level") ) {
                level = line.getOptionValue("level");
            }
            if ( line.hasOption("user") ) {
                user = line.getOptionValue("user");
            }
            if ( line.hasOption("hostname") ) {
                hostname = line.getOptionValue("hostname");
            }
            if ( line.hasOption("port") ) {
                port = Integer.parseInt(line.getOptionValue("port"));
            }
        } catch( ParseException exp ) {
            logger.fatal("Parsing failed. Reason: " + exp.getMessage());
            System.exit(1);
        }

        if (meExecutionFilepath.equals("") || telemetryFilepath.equals("")) {
            logger.fatal("Need to specify arguments for ME Execution and Telemetry Filepath");
            System.exit(1);
        }

        logger.info("------------------- Arguments -------------------");
        logger.info("Username of Player: " + user);
        logger.info("Model Engine Execution Filepath: " + meExecutionFilepath);
        logger.info("Telemetry File Path: " + telemetryFilepath);
        logger.info("Path to parameter file: " + parameterFilepath);
        logger.info("Path to player modeling directory: " + pmdir);
        logger.info("Current level: " + level);
        logger.info("Hostname: " + hostname);
        logger.info("Port: " + port);
        logger.info("Debugging: " + debug);
        logger.info("-------------------------------------------------");

        PlayerModelingEngine pmEngine = new PlayerModelingEngine(cls, interval, skillVectorUpdateTechniqueFlag, parameterFilepath, pmdir, level, user);
        pmEngine.readTrainingModel(pmdir, TRAINING_MODEL_FILEPATH);
        pmEngine.executePM(telemetryFilepath, meExecutionFilepath);
        ServerInterface serverInterface = new ServerInterface(parameterFilepath, hostname, port, pmEngine.logStartTimeStamp, pmEngine.logEndTimeStamp, meExecutionFilepath);
        serverInterface.saveSkillVector(level, user);
    }
}
