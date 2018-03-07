package playermodeling;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Created by pavankantharaju on 2/26/18.
 */


public class Main {

    public static void main(String [] args) {

        Simulation sim = new Simulation("../35_saved_data","../LogVisualizer/slices.tsv",5);
        sim.simulate();

//        String str = "\"color\":0,\"direction_condition\":\"West\",\"directions_colors\":[[],[],[],[]],\"passed\":0,\"directions_types\":[[\"Empty\"],[],[\"Unconditional\"],[]],\"direction_default\":\"West\"";
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
