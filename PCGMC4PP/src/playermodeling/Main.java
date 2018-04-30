package playermodeling;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

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

        String saved_data_location = "../35_saved_data";
        String slices_location = "../LogVisualizer/slices.tsv";
        int interval = Integer.parseInt("5");

        /* There are a total of 4 runs that need to be simulated:
        *   1) MR RU1
        *   2) MR UR2
        *   3) R RU1
        *   4) R RU2
        * */

        int [] update_technique_flags = {0,2};
        int [] rule_update_flags = {0,1};

        for ( int utf : update_technique_flags ) {
            for ( int ruf : rule_update_flags ) {
                String update = "";
                switch ( utf ) {
                    case 0:
                        update = "RM";
                        break;
                    case 2:
                        update = "R";
                        break;
                }
                String filename = String.format("Simulation-Results-%s-RU%d-5s.csv",update,ruf+1);
                System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(filename)),true));

                Simulation sim = new Simulation(saved_data_location,slices_location,interval, utf, ruf);
                sim.simulate();
            }
        }
        /* Flags */
        /*
            0 : Machine Learning and Rules
            1 : Machine Learning Only
            2 : Rules Only
         */

//        int update_technique_flag = 2;
//        /*
//            0 : Additive
//            1 : Absolute/Additive
//         */
//        int rule_update_flag = 1;
//
//        Simulation sim = new Simulation(saved_data_location,slices_location,interval, update_technique_flag, rule_update_flag);
//        sim.simulate();
    }

}
