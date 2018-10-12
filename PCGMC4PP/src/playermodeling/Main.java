package playermodeling;

import org.apache.commons.cli.*;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static final String TRAINING_MODEL_FILEPATH = "pmfiles/";

    public static void main(String [] args) throws Exception {
        Options cliOptions = new Options();
        cliOptions.addOption("mepath",true,"Model Engine Execution Filepath");
        cliOptions.addOption("telemetrypath,",true,"Telemetry Filepath");
        cliOptions.addOption("parameterpath,",true,"Parameter Filepath");

        String meExecutionFilepath = "";
        String telemetryFilepath = "";
        String parameterFilepath = "";

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
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        if (meExecutionFilepath.equals("") || telemetryFilepath.equals("")) {
            System.out.println("Need to specify arguments for ME Execution and Telemetry Filepath");
            System.exit(-1);
        }

        PlayerModelingEngine pmEngine = new PlayerModelingEngine();
        pmEngine.readTrainingModel(TRAINING_MODEL_FILEPATH);
        pmEngine.executePM(telemetryFilepath, meExecutionFilepath);
    }
}
