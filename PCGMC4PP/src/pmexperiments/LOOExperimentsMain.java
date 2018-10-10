package pmexperiments;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.BayesNet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class LOOExperimentsMain {

    public static void main(String [] args) throws Exception {
        /* NOTE: LOO stands for Leave One Out */

        /* FLAGS */
        /*
            0 (RM) : Machine Learning and Rules
            1 (M) : Machine Learning Only
            2 (R) : Rules Only
         */

        int[] skill_vector_update_technique_flags = {0, 1, 2};
        String[] update_names = {"RM", "M", "R"};
        int[] time_intervals = {10, 20, 30};

        String directory_name = "loo-results/";
        new File(directory_name).mkdir();

        String[] machine_learning_classes = {
                "weka.classifiers.trees.J48",
                "weka.classifiers.trees.RandomForest",
                "weka.classifiers.meta.Bagging",
                "weka.classifiers.meta.AdaBoostM1",
                "weka.classifiers.bayes.NaiveBayes",
                "weka.classifiers.bayes.BayesNet",
                "weka.classifiers.functions.MultilayerPerceptron"
        };
        String [] machine_learning_algorithms = new String[machine_learning_classes.length];
        for ( int i = 0; i < machine_learning_classes.length; i++ ) {
            String [] split_class_string = machine_learning_classes[i].split("\\.");
            machine_learning_algorithms[i] = split_class_string[split_class_string.length-1];
        }

        String training_data_relative_path = "../35-dash-dataset";

        for (int time : time_intervals) {
            for (int utf : skill_vector_update_technique_flags) {
                if (utf == 0 || utf == 1) {
                    for (int i = 0; i < machine_learning_algorithms.length; i++) {
                        Class c1 = Class.forName(machine_learning_classes[i]);
                        Constructor con1 = c1.getConstructor();
                        Classifier classifier = (Classifier) con1.newInstance();
                        String fname = directory_name + String.format("%s.%s.%d.csv",update_names[utf],machine_learning_algorithms[i],time);
                        PrintStream old = System.out;

                        System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fname)), true));

                        SimulationLOO sim = new SimulationLOO(classifier, time, utf, training_data_relative_path);
                        sim.execute();

                        System.setOut(old);
                    }
                } else {
                    String fname = directory_name + String.format("%s.%d.csv",update_names[utf],time);
                    PrintStream old = System.out;

                    System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(fname)), true));

                    SimulationLOO sim = new SimulationLOO(new BayesNet(), time, utf, training_data_relative_path);
                    sim.execute();

                    System.setOut(old);
                }
            }
        }
    }
}
