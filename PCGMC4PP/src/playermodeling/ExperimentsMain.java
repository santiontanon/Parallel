package playermodeling;


import com.sun.xml.internal.bind.v2.util.ByteArrayOutputStreamEx;
import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;
import weka.classifiers.meta.AdaBoostM1;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class ExperimentsMain {

    public static void main(String [] args) throws Exception {
        /* FLAGS */
        /*
            0 : Machine Learning and Rules
            1 : Machine Learning Only
            2 : Rules Only
         */
        int[] update_technique_flags = {0, 1, 2};
        String[] update_names = {"RM", "M", "R"};
        /*
            0 : Additive
            1 : Absolute/Additive
         */

        int[] rule_update_flags = {0};

        int[] timings = {10, 20, 30};

        String directory_name = "simulation_results_06132018_8_data/";
        new File(directory_name).mkdir();

        String[] machine_learning_algorithms = {
                "J48",
                "RandForest",
                "Bagging",
                "AdaBoost",
                "NaiveBayes",
                "BayesNet",
                "MultiPercep"
        };

        String[] machine_learning_classes = {
                "weka.classifiers.trees.J48",
                "weka.classifiers.trees.RandomForest",
                "weka.classifiers.meta.Bagging",
                "weka.classifiers.meta.AdaBoostM1",
                "weka.classifiers.bayes.NaiveBayes",
                "weka.classifiers.bayes.BayesNet",
                "weka.classifiers.functions.MultilayerPerceptron"
        };
        String saved_data_location = "../35_saved_data";
        String testing_path = "../8_saved_data/8_saved_data";
        String slices_location = "../LogVisualizer/slices.tsv";

        for (int time : timings) {
            for (int utf : update_technique_flags) {
                if (utf == 0 || utf == 1) {
                    for (int i = 0; i < machine_learning_algorithms.length; i++) {
                        Class c1 = Class.forName(machine_learning_classes[i]);
                        Constructor con1 = c1.getConstructor();
                        Classifier classifier = (Classifier) con1.newInstance();
                        String fname = directory_name + String.format("%s.%s.%d.csv",update_names[utf],machine_learning_algorithms[i],time);
                        PrintStream old = System.out;

                        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fname)), true));

                        //Simulation_v1 sim = new Simulation_v1(saved_data_location, slices_location, time, utf, 0, classifier,false);
                        Simulation_v2 sim = new Simulation_v2(saved_data_location, testing_path, slices_location, time, utf, 0, classifier,false);
                        sim.simulate();

                        System.setOut(old);
                    }
                } else {
                    String fname = directory_name + String.format("%s.%d.csv",update_names[utf],time);
                    PrintStream old = System.out;

                    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fname)), true));

                    //Simulation_v1 sim = new Simulation_v1(saved_data_location, slices_location, time, utf, 0, new BayesNet(),false);
                    Simulation_v2 sim = new Simulation_v2(saved_data_location, testing_path, slices_location, time, utf, 0, new BayesNet(),false);
                    sim.simulate();

                    System.setOut(old);
                }
            }
        }
    }
}
