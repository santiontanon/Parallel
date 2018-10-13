package playermodeling;

import org.apache.commons.cli.*;
import pmutils.ServerInterface;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static final String TRAINING_MODEL_FILEPATH = "pmfiles/weka.classifiers.bayes.NaiveBayes.10.model";
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


        String meExecutionFilepath = "";
        String telemetryFilepath = "";
        String parameterFilepath = "";
        String level = "";
        String user = "";

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
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (meExecutionFilepath.equals("") || telemetryFilepath.equals("")) {
            System.out.println("Need to specify arguments for ME Execution and Telemetry Filepath");
            System.exit(-1);
        }

        PlayerModelingEngine pmEngine = new PlayerModelingEngine(cls, interval, skillVectorUpdateTechniqueFlag, parameterFilepath, level, user);
        pmEngine.readTrainingModel(TRAINING_MODEL_FILEPATH);
        pmEngine.executePM(telemetryFilepath, meExecutionFilepath);
        ServerInterface serverInterface = new ServerInterface("testing/");
        serverInterface.saveSkillVectorToServer(level, user);
    }
}
