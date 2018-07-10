package playermodeling;


//import org.apache.commons.cli.Options;
import weka.classifiers.functions.MultilayerPerceptron;

import java.io.*;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static void main(String [] args) throws Exception {
        /* TODO: Set up command line arguments */
//        Options cli_options = new Options();
//        cli_options.addOption("train_data",true,"Training data location (should contain log, data, and id)");
//        cli_options.addOption("test_data,",true,"Testing data location (should contain log, data, and id)");
//        cli_options.addOption("slice_loc",true,"Location of slices data (None for none)");
//        cli_options.addOption("interval",true,"Time Interval for feature extraction");

//        String saved_data_location = "";
//        String slices_location = "";
//        int interval = 5;
//        try {
//            String saved_data_location = args[0];
//        String slices_location = args[1];
//        int interval = Integer.parseInt(args[2]);

        String saved_data_location = "../35_saved_data";
        String testing_path = "../8_saved_data/8_saved_data";
        String slices_location = "../LogVisualizer/slices.tsv";
        int interval = Integer.parseInt("30");


        String [] machine_learning_algorithms = {
                "J48",
                "Random Forest",
                "Bagging",
                "Ada Boost",
                "Naive Bayes",
                "Bayes Net",
                "Multilayer Perceptron"
        };

        String [] machine_learning_classes = {
                "weka.classifiers.trees.J48",
                "weka.classifiers.trees.RandomForest",
                "weka.classifiers.meta.Bagging",
                "weka.classifiers.meta.AdaBoostM1",
                "weka.classifiers.bayes.NaiveBayes",
                "weka.classifiers.bayes.BayesNet",
                "weka.classifiers.functions.MultilayerPerceptron"
        };

        int [] update_technique_flags = {0,1,2};
        int [] rule_update_flags = {0,1};
        int [] timings = { 10, 20, 30 };

        /* Flags */
        /*
            0 : Machine Learning and Rules
            1 : Machine Learning Only
            2 : Rules Only
         */

        int update_technique_flag = 0;
        /*
            0 : Additive
            1 : Absolute/Additive
         */
        int rule_update_flag = 0;


        //Simulation_v2 sim = new Simulation_v2(saved_data_location, testing_path, slices_location,interval, update_technique_flag, rule_update_flag, new MultilayerPerceptron(), false);
        Simulation_v1 sim = new Simulation_v1(saved_data_location,slices_location,interval, update_technique_flag, rule_update_flag, new MultilayerPerceptron(), false);
        sim.simulate();
    }

}
