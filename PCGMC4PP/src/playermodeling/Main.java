package playermodeling;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;

import java.util.HashMap;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static void main(String [] args) {

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

        Simulation sim = new Simulation(saved_data_location,slices_location,interval);
        sim.simulate();
//        String str = "{\"color\":0,\"direction_condition\":\"West\",\"directions_colors\":[[],[],[],[]],\"passed\":0,\"directions_types\":[[\"Empty\"],[],[\"Unconditional\"],[]],\"direction_default\":\"West\"}";
//        Gson gson = new Gson();
//        HashMap<String,Object> tmp = gson.fromJson(str,HashMap.class);
//        for ( String t : tmp.keySet() ) {
//            System.out.println("T: " + t + "," + tmp.get(t));
//        }
//        str = str.replace("\"","");
//        String brackets = "\\[\\]";
////        str = str.replace("]","\"");
////        //String [] tmp = str.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
//        String regex = String.format(",(?=([^%s]*:\\[.*\\])*[^%s]*$)",brackets,brackets);
//        String [] tmp = str.split(regex);
//        for ( String s : tmp ) {
//            System.out.println(s);
//        }
    }



}
