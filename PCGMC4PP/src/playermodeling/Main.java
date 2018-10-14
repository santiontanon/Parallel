package playermodeling;

import org.apache.commons.cli.*;
import pmutils.ServerInterface;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static final String TRAINING_MODEL_FILEPATH = "pmfiles/classifier-model.model";
    public static final double interval = 5;
    public static final int skillVectorUpdateTechniqueFlag = 0;
    public static final Classifier cls = new NaiveBayes();

    public static void main(String [] args) throws Exception {
        Options cliOptions = new Options();
        cliOptions.addOption("mepath",true,"Model Engine Execution Filepath");
        cliOptions.addOption("telemetrypath",true,"Telemetry Filepath");
        cliOptions.addOption("parameterpath",true,"Parameter Filepath");
        cliOptions.addOption("level", true, "Level of Game");
        cliOptions.addOption("user", true, "Player");
        cliOptions.addOption("hostname",true,"Hostname of server");
        cliOptions.addOption("port",true,"Port number of server");
        cliOptions.addOption("debug",false,"Port number of server");

        String meExecutionFilepath = "";
        String telemetryFilepath = "";
        String parameterFilepath = "";
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
            if ( line.hasOption("debug") ) {
                debug = true;
            }
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (meExecutionFilepath.equals("") || telemetryFilepath.equals("")) {
            System.out.println("Need to specify arguments for ME Execution and Telemetry Filepath");
            System.exit(-1);
        }

        if ( debug ) {
            System.out.println("------------------- Arguments -------------------");
            System.out.println("Username of Player: " + user);
            System.out.println("Model Engine Execution Filepath: " + meExecutionFilepath);
            System.out.println("Telemetry File Path: " + telemetryFilepath);
            System.out.println("Path to parameter file: " + parameterFilepath);
            System.out.println("Current level: " + level);
            System.out.println("Hostname: " + hostname);
            System.out.println("Port: " + port);
            System.out.println("Debugging: " + debug);
            System.out.println("-------------------------------------------------");
        }

        PlayerModelingEngine pmEngine = new PlayerModelingEngine(cls, interval, skillVectorUpdateTechniqueFlag, parameterFilepath, level, user);
        pmEngine.readTrainingModel(TRAINING_MODEL_FILEPATH);
        pmEngine.executePM(telemetryFilepath, meExecutionFilepath);
        ServerInterface serverInterface = new ServerInterface(parameterFilepath, hostname, port, debug);
        serverInterface.saveSkillVectorToServer(level, user);
    }
}
