package playermodeling;


import weka.classifiers.functions.MultilayerPerceptron;

import java.io.*;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static void main(String [] args) throws Exception {
        /* TODO: Set up command line arguments */
//        if ( args.length < 2 ) {
//            System.err.println("Program takes in three arguments: saved_data_location (required), slices_location (required), interval (optional. default=5)");
//            System.exit(0);
//        }

//        String saved_data_location = "";
//        String slices_location = "";
//        int interval = 5;
//        try {
//            String saved_data_location = args[0];
//        String slices_location = args[1];
//        int interval = Integer.parseInt(args[2]);

        String directory_name = "simulation_results_timings/";

        new File(directory_name).mkdir();

        String saved_data_location = "../35_saved_data";
        String testing_path = "../8_saved_data/8_saved_data";
        String slices_location = "../LogVisualizer/slices.tsv";
        int interval = Integer.parseInt("30");

        /* There are a total of 4 runs that need to be simulated:
        *   1) MR RU1
        *   2) MR UR2
        *   3) R RU1
        *   4) R RU2
        * */

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
        int [] timings = { 5, 10, 20, 30 };

        /* Flags */
        /*
            0 : Machine Learning and Rules
            1 : Machine Learning Only
            2 : Rules Only
         */

        int update_technique_flag = 1;
        /*
            0 : Additive
            1 : Absolute/Additive
         */
        int rule_update_flag = 0;


        //ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //PrintStream old = System.out;

        //System.setOut(new PrintStream(baos,true));
        //System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream("log2")), true));
        //Simulation_v2 sim = new Simulation_v2(saved_data_location, testing_path, slices_location,interval, update_technique_flag, rule_update_flag, new MultilayerPerceptron());
        Simulation_v1 sim = new Simulation_v1(saved_data_location,slices_location,interval, update_technique_flag, rule_update_flag, new MultilayerPerceptron());
        sim.simulate();

        //System.setOut(old);
        //String data = baos.toString();

        //System.out.println("Data: " + data);
    }

}
