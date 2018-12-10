package pmexperiments;

import playermodeling.TelemetryUtils;

import java.util.ArrayList;

public class TelemetryUtils35Dash extends TelemetryUtils {

    public TelemetryUtils35Dash() {}

//    public String getUsername(ArrayList<String> data) {
//        /*  NOTE: There are a few users from the study
//            (3 and 5 dash datasets) that were not correctly mentioned in the "SessionUser" event..
//            */
//
//        String session_id = "";
//        for ( String d : data ) {
//            String [] tmp = d.split("\t");
//            if ( tmp[1].equals("SessionID") ) {
//                session_id = tmp[2];
//            }
//
//            if ( tmp[1].equals("SessionUser") ) {
//                if ( tmp[2].equals("NONE")  ) {
//                    /* Check session id */
//                    switch ( session_id ) {
//                        case "CAT":
//                            return "MOUSE";
//                        case "DOG":
//                            return "DRAGON";
//                    }
//                }
//                if ( tmp[2].equals("CHICKEN") ) {
//                    return "MONKEY";
//                }
//                return tmp[2];
//            }
//        }
//        System.err.println("Execution should never be here.");
//        System.exit(-1);
//        return "";
//    }

    public String getUsername(ArrayList<String> data) {
        /*  NOTE: There are a few users from the study
            (3 and 5 dash datasets) that were not correctly mentioned in the "SessionUser" event..
            */

        String session_id = "";
        for ( String d : data ) {
            String [] tmp = d.split("\t");
            if ( tmp[1].equals("SessionID") ) {
                session_id = tmp[2];
            }

            if ( tmp[1].equals("SessionUser") ) {
                if ( tmp[2].equals("NONE")  ) {
                    /* Check session id */
                    switch ( session_id ) {
                        case "1241":
                            return "3-1";
                        case "1238":
                            return "3-4";
                    }
                }
                if ( tmp[2].equals("evan") ) {
                    return "5-1";
                }
                return tmp[2];
            }
        }
        System.err.println("Execution should never be here.");
        System.exit(-1);
        return "";
    }
}
