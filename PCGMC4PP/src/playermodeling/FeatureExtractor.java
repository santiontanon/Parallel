package playermodeling;

import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * Created by pavankantharaju on 2/26/18.
 */
public class FeatureExtraction {

    public static String[] features = {
            "start_date",
            "start_time",
            "end_date",
            "tutorial_time",
            "total_time",
            "me_tests",
            "me_submissions",
            "me_replays",
            "tooltips",
            "connection_visibility",
            "flow_visibility",
            "flow_tooltip",
            "popup_success",
            "popup_error",
            "popup_error_starvation",
            "popup_error_deadend",
            "popup_error_delivery",
            "popup_error_badgoals",
            "tooltip_Place_Semaphore",
            "tooltip_PlaceButton",
            "tooltip_Trash",
            "dragged",
            "trashed",
            "hover",
            "dragged_button",
            "trashed_button",
            "dragged_semaphore",
            "trashed_semaphore",
            "dragged_signal",
            "trashed_signal",
            "hover_button",
            "hover_semaphore",
            "hover_signal",
            "hover_pickup",
            "hover_delivery",
            "hover_conditional",
            "hover_diverter",
            "hover_exchange",
            "r_me_per_minute",
            "r_me_tests_per_minute",
            "r_me_submissions_per_minute",
            "r_me_replays_per_minute",
            "r_me_tests",
            "r_me_submissions",
            "r_me_replays",

            "r_num_tracks_used_per_minute",
            "r_num_track_no_changes_per_minute",
            "r_num_track_changes_per_minute",
            "r_num_mouse_on_comp_per_minute",
            "r_dragged_components_per_minute",
            "r_hover_components_per_minute",

            /* "r_reposition_dist_per_minute", */
            "t_d_component_dragged_me_avg",
            "t_d_component_dragged_me_tests_avg",
            "t_d_component_dragged_me_submissions_avg",
            "t_d_component_dragged_me_min",                 // This is apparently helpful...why...
            "t_d_component_dragged_me_tests_min",
            "t_d_component_dragged_me_submissions_min",

            "num_tracks_used",                              // Number of tracks used at the time period
            "num_track_no_changes",
            "num_track_changes",
            "num_mouse_on_comp",                            // Number of tracks used at the time period

            "total_time_on_component",                      // Total amount of time on components
            "avg_time_on_component",                        // Computed as: total time on component / num_mouse_on_comp

           // "total_components",                             // Total number of components over all USED track ( doing something on it...not just mousing/hovering )
           // "tracks_used_components_per_track_avg",         // Total number of components over all used track / tracks used
            "avg_dragged_components_dist",
            "total_dragged_components_dist"
            /* "total_reposition_dist"   Total reposition euclidean distance */
    };

    private SkillAnalyzer analyzer;

    public String[] additional_features = {
            "_timestamp_last_dragged_component",
            "_timestamp_last_dragged_component",
            "_timestamp_last_connected_component",
            "_track_color_last_placed_component",
            "_t_d_component_dragged_me_lst",
            "_tracks_used_lst",
    };

    public FeatureExtraction(SkillAnalyzer a ) {
        analyzer = a;
    }

    public HashMap<String,Object> parseComponentInformation( String comp_info ) {
        Gson gson = new Gson();
        HashMap<String,Object> info = gson.fromJson(comp_info,HashMap.class);
        return info;
    }

    /* Making a decision that all attributes must be numerical.... */
    public HashMap< String, Double > extractFeatureVector(String path, ArrayList<String> telemetry_at_interval,
                                                          ArrayList<Integer> index_vector,
                                                          HashMap<Integer, PersistentData > all_persistent_data,
                                                          HashMap<String, ArrayList< String > > data_files) {

        /* NOTE: The is_testing argument is required because in the training data, we never had an endDrag event, but in the testing we do */

        HashMap< String, Double > feature_vector = new HashMap< String, Double >();

        /* Used to store list of values for later computation */
        HashMap< String, ArrayList<Double> > list_map = new HashMap< String, ArrayList<Double> >();

        Pair<Double,Double> ir_coord = new Pair<Double,Double>(), fr_coord = new Pair<Double,Double>();
        Pair<Double,Double> il_coord = new Pair<Double,Double>(), fl_coord = new Pair<Double,Double>();
        Pair<Double,Double> id_coord = new Pair<Double,Double>(), fd_coord = new Pair<Double,Double>();

        if ( telemetry_at_interval.size() == 0 ) {
            return feature_vector;
        }
        /* Initialize feature vector */
        for ( String f : features ) {
            feature_vector.put(f,0.0);
        }

        String [] data = telemetry_at_interval.get(0).split("\t");
        String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

        /* Bad things happen in WEKA if we leave the date as is...so we convert it to the number of seconds since the epoch */
        /* NOTE: We could convert this to a numerical version of the date.. */

        if ( !all_persistent_data.containsKey(0) ) {
            /* This level was not either complete OR doesn't have a file in data/ associate with it. (will end up with inaccurate data
                to train on...) */
            return new HashMap< String, Double >();
        }

        feature_vector.replace("start_date", (double)s_);
        feature_vector.replace("start_time", ((double)s_) - ((Long)all_persistent_data.get(0).persistent_data.get("start_time")).doubleValue() );

        int i = 0;
        for ( String telemetry : telemetry_at_interval ) {
            data = telemetry.split("\t");
            date_ = data[0];
            String name_ = data[1];
            String data_ = data[2];

            double time_ = Double.parseDouble(data[3]);
            double x_ = Double.parseDouble(data[4]);
            double y_ = Double.parseDouble(data[5]);

            formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
            time = LocalDateTime.parse(date_, formatter);

            /* Time in seconds */
            s_ = (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000);

            if ( !all_persistent_data.containsKey(index_vector.get(i)) ) {
                /* This level was not either complete OR doesn't have a file in data/ associate with it. (will end up with inaccurate data
                to train on...) */

                /* Return empty feature vector...don't want inaccurate information */
                return new HashMap< String, Double >();
            }

            HashMap<String, Object > persistent_data = all_persistent_data.get(index_vector.get(i)).persistent_data;
            s_ = s_ - (Long)persistent_data.get("start_time");

            switch (name_) {
                case "TriggerLoadLevel":
                    /* Get level data */
                    if (data_.length() != 0) {
                        if (data_.startsWith("l")) {
                        } else {
                        }
                    } else {
                        if ( feature_vector.containsKey("me_replays") ) {
                            double nreplays = feature_vector.get("me_replays");
                            feature_vector.replace("me_replays", nreplays + 1);
                        }
                    }
                    break;
                case "endTutorial":
                    if ( feature_vector.containsKey("tutorial_time") ) {
                        feature_vector.replace("tutorial_time", (s_ - feature_vector.get("start_time")) / 60.0);
                    }
                    break;
                case "SubmitCurrentLevelPlay":
                    /* This is testing */
                    //persistent_data.replace("test_before_submit",true);
                    if ( feature_vector.containsKey("me_tests") ) {
                        double val = feature_vector.get("me_tests");
                        feature_vector.replace("me_tests", val + 1);
                    }
                    if ( feature_vector.containsKey("_timestamp_last_dragged_component") ) {
                        double t = s_ - feature_vector.get("_timestamp_last_dragged_component");

                        if (list_map.get("_t_d_component_dragged_me_lst_tests") == null) {
                            ArrayList<Double> tmp = new ArrayList<Double>();
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_tests", tmp);
                        } else {
                            ArrayList<Double> tmp = list_map.get("_t_d_component_dragged_me_lst_tests");
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_tests",tmp);
                        }
                    }
                    break;
                case "SubmitCurrentLevelME":
                    /* This is submit */
//                    if ( (Boolean)persistent_data.get("test_before_submit") ) {
//                        analyzer.updateRuleEvidence("Testing before submitting");
//                        persistent_data.replace("test_before_submit",false);
//                    }

                    if ( feature_vector.containsKey("me_submissions") ) {
                        double val = feature_vector.get("me_submissions");
                        feature_vector.replace("me_submissions", val + 1);
                    }
                    if (feature_vector.containsKey("_timestamp_last_dragged_component")) {
                        double t = s_ - feature_vector.get("_timestamp_last_dragged_component");
                        if (list_map.get("_t_d_component_dragged_me_lst_submissions") == null) {
                            ArrayList<Double> tmp = new ArrayList<Double>();
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_submissions", tmp);
                        } else {
                            ArrayList<Double> tmp = list_map.get("_t_d_component_dragged_me_lst_submissions");
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_submissions",tmp);
                        }
                    }
                    break;
                case "tooltip":
                    if ( feature_vector.containsKey("tooltips") ) {
                        double n = feature_vector.get("tooltips");
                        feature_vector.replace("tooltips", n + 1);
                    }

                    if ( feature_vector.containsKey("tooltip_" + data_) ) {
                        double n = feature_vector.get("tooltip_" + data_);
                        feature_vector.replace("tooltip_" + data_, n + 1);
                    }
                    break;
                case "startDrag":
                    if ( (Boolean)persistent_data.get("dragging") ) {
                        /* Training dataset didn't have endDrag due to bug (only had startDrag startDrag pair). This is fixed in the testing dataset */
                        analyzer.updateRuleEvidence("Drag objects");
                        //updateRuleEvidence("Drag objects",rule_evidence);

                        fd_coord.p1 = x_;
                        fd_coord.p2 = y_;
                        double dist = 0.0;

                        // Euclidean distance
                        if (id_coord.p1 != null && id_coord.p2 != null) {
                            dist = Math.sqrt(Math.pow((fd_coord.p1 - id_coord.p1), 2.0) + Math.pow((fd_coord.p2 - id_coord.p2), 2.0));
                        } else {
                            dist = 0.0;
                        }

                        id_coord.reset();
                        fd_coord.reset();

//                        if ( (Integer)persistent_data.get("num_dragged") > 2 ) {
//                            analyzer.updateRuleEvidence("Place objects on track");
//                            //updateRuleEvidence("Place objects on the track",rule_evidence);
//                        }

                        persistent_data.replace("num_dragged",(Integer)persistent_data.get("num_dragged") + 1);

                        if ( feature_vector.containsKey("total_dragged_components_dist") ) {
                            double n = feature_vector.get("total_dragged_components_dist");
                            feature_vector.replace("total_dragged_components_dist", n + dist);
                        }

                        persistent_data.replace("dragging",false);

                    } else {
                        if ( feature_vector.containsKey("dragged") ) {
                            double n = feature_vector.get("dragged");
                            feature_vector.replace("dragged", n + 1);
                        }
                        if ( feature_vector.containsKey("dragged_" + data_) ) {
                            double n = feature_vector.get("dragged_" + data_);
                            feature_vector.replace("dragged_" + data_, n + 1);
                        }

                        if (!feature_vector.containsKey("_timestamp_last_dragged_component")) {
                            feature_vector.put("_timestamp_last_dragged_component", (double) s_);
                        } else {
                            feature_vector.replace("_timestamp_last_dragged_component", (double) s_);
                        }

                        id_coord.p1 = x_;
                        id_coord.p2 = y_;
                        persistent_data.replace("dragging",true);
                    }
                    break;
                case "endDrag":
                    break;
                case "Destroying":
                    if ( feature_vector.containsKey("trashed") ) {
                        double n = feature_vector.get("trashed");
                        feature_vector.replace("trashed",n+1);
                    }

                    if ( feature_vector.containsKey("trashed_" + data_) ) {
                        double n = feature_vector.get("trashed_" + data_);
                        feature_vector.replace("trashed_" + data_, n + 1);
                    }
                    analyzer.updateRuleEvidence("Remove unnecessary elements");
                    //updateRuleEvidence("Remove unnecessary elements",rule_evidence);
                    break;
                case "OnHoverBehavior":
                    // This implies that we are on a track...but we hover on an object though..?
                    if ( feature_vector.containsKey("hover") ) {
                        double n = feature_vector.get("hover");
                        feature_vector.replace("hover",n+1);
                    }

                    if ( feature_vector.containsKey("hover_" + data_) ) {
                        double n = feature_vector.get("hover_" + data_);
                        feature_vector.replace("hover_" + data_, n + 1);
                    }

                    analyzer.updateRuleEvidence("Hover over objects to see what they do");
                    //updateRuleEvidence("Hover over objects to see what they do",rule_evidence);

                    break;
                case "BeginReposition":
                    ir_coord.p1 = x_;
                    ir_coord.p2 = y_;
                    break;
                case "EndReposition":
                    if ( ((String)persistent_data.get("cur_mouse_comp")).length() == 0 ) {
                        break;
                    }

                    /* NOTE: While this is okay for training, if this were to actually be deployed into Parallel, we wouldn't have access to a mapping between comoponents
                     * and the color of the track...
                     * */

                    String comp_id =  ((String)persistent_data.get("cur_mouse_comp")).split("/")[1];
                    HashMap<String,String> comp_color_map = (HashMap<String,String>) persistent_data.get("comp_color_map");

                    if ( !comp_color_map.containsKey(comp_id) ) {
                        break;
                    }

                    if ( feature_vector.containsKey("num_tracks_used") ) {
                        double n = feature_vector.get("num_tracks_used");
                        feature_vector.replace("num_tracks_used", n + 1);
                    }

                    fr_coord.p1 = x_;
                    fr_coord.p2 = y_;

                    ir_coord.reset();
                    fr_coord.reset();

                    break;
                case "BeginLink":
                    if ( ((String)persistent_data.get("cur_mouse_comp")).length() == 0 ) {
                        break;
                    }

                    /* NOTE: While this is okay for training, if this were to actually be deployed into Parallel, we wouldn't have access to a mapping between components
                     * and the color of the track...
                     * */
                    comp_id =  ((String)persistent_data.get("cur_mouse_comp")).split("/")[1];
                    comp_color_map = (HashMap<String,String>) persistent_data.get("comp_color_map");


                    if ( !comp_color_map.containsKey(comp_id) ) {
                        break;
                    }

                    if ( feature_vector.containsKey("num_tracks_used") ) {
                        double n = feature_vector.get("num_tracks_used");
                        feature_vector.replace("num_tracks_used", n + 1);
                    }

                    il_coord.p1 = x_;
                    il_coord.p2 = y_;
                    break;
                case "LinkTo":
                    persistent_data.replace("linking",true);

                    fl_coord.p1 = x_;
                    fl_coord.p2 = y_;

                    double dist = 0.0;

                    // Euclidean distance
                    if (il_coord.p1 != null && il_coord.p2 != null) {
                        dist = Math.sqrt(Math.pow((fl_coord.p1 - il_coord.p1), 2.0) + Math.pow((fl_coord.p2 - il_coord.p2), 2.0));
                    } else {
                        dist = 0.0;
                    }

                    il_coord.reset();
                    fl_coord.reset();
                    break;
                case "OnMouseComponent":
                    if ( feature_vector.containsKey("num_mouse_on_comp") ) {
                        double num_mouse_comp = feature_vector.get("num_mouse_on_comp");
                        feature_vector.replace("num_mouse_on_comp", num_mouse_comp + 1);
                    }

                    persistent_data.replace("cur_mouse_comp",data_);
                    persistent_data.replace("cur_mouse_time",time_);

//                    HashMap<String,String> comp_loc_map = (HashMap<String,String>) persistent_data.get("comp_loc_map");
                    comp_id = data_.split("/")[1];
//                    if ( comp_loc_map == null ) {
//                        HashMap<String,String> loc_map = new HashMap<String,String>();
//                        loc_map.put(comp_id,String.format("%f,%f",x_,y_));
//                        persistent_data.put("comp_loc_map",loc_map);
//                    } else {
//                        comp_loc_map.put(comp_id,String.format("%f,%f",x_,y_));
//                        persistent_data.put("comp_loc_map",comp_loc_map);
//                    }

                    comp_color_map = (HashMap< String, String >)persistent_data.get("comp_color_map");
                    if ( comp_color_map == null ) {
                        /* This really should never happen.. */
                        break;
                    }

                    if ( comp_color_map.size() != 0 && comp_color_map.containsKey(comp_id) ) {
//                        System.out.println(persistent_data.toString());
//                        System.out.println("Current track: " + persistent_data.get("cur_track"));
                        int cur_track = Integer.parseInt(String.valueOf(persistent_data.get("cur_track")));
                        if ( cur_track == -1) {
                            persistent_data.replace("cur_track", Integer.parseInt(comp_color_map.get(comp_id)));
                        } else if ( cur_track != Integer.parseInt(comp_color_map.get(comp_id))) {
                            if ( feature_vector.containsKey("num_track_changes") ) {
                                double n = feature_vector.get("num_track_changes");
                                feature_vector.replace("num_track_changes", n + 1);
                            }
                            persistent_data.replace("cur_track", comp_color_map.get(comp_id));
                        } else {
                            if ( feature_vector.containsKey("num_track_no_changes") ) {
                                double n = feature_vector.get("num_track_no_changes");
                                feature_vector.replace("num_track_no_changes", n + 1);
                            }
                        }
                    }

                    if ( (Boolean)persistent_data.get("linking") ) {
                        /* Check the component */

                        /* Check which component we are linking to
                        *       direction switch -> conditional
                        * */
//                        if ( data_.contains("signal")  ) {
//                            analyzer.updateRuleEvidence("Be able to link semaphores to buttons");
//                            //updateRuleEvidence("Be able to link semaphores to buttons",rule_evidence);
//                        }

                        if ( data_.contains("conditional")  ) {
                            analyzer.updateRuleEvidence("Be able to link buttons to direction switches");
                            //updateRuleEvidence("Be able to link buttons to direction switches",rule_evidence);
                        }

                        if ( comp_color_map.size() != 0 ) {
                            if ( comp_color_map.containsKey(comp_id) ) {
                                if ( feature_vector.containsKey("num_tracks_used") ) {
                                    double n = feature_vector.get("num_tracks_used");
                                    feature_vector.replace("num_tracks_used", n + 1);
                                }
                            }
                        }
                    }
                    persistent_data.replace("linking",false);
                    break;
                case "OutMouseComponent":
                    //System.out.println(telemetry);
                    if ( time_ - (Double)persistent_data.get("cur_mouse_time") < 0 ) {
                        /* This actually happens when the cur_mouse_time is taken from a previous time interval, and time_ starts before cur_mouse_time ) */
                        break; /* Don't use this time_ */
                    }
                    if ( feature_vector.containsKey("total_time_on_component") ) {
                        double total_time_on_component = feature_vector.get("total_time_on_component");
                        feature_vector.replace("total_time_on_component", total_time_on_component + (time_ - (Double) persistent_data.get("cur_mouse_time")));
                    }
                    break;
                case "ToggleConnectionVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    if ( feature_vector.containsKey("connection_visibility") ) {
                        double n = feature_vector.get("connection_visibility");
                        feature_vector.replace("connection_visibility", n + 1);
                    }
                    break;
                case "ToggleFlowVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    if ( feature_vector.containsKey("flow_visibility") ) {
                        double n = feature_vector.get("flow_visibility");
                        feature_vector.replace("flow_visibility", n + 1);
                    }

                    analyzer.updateRuleEvidence("Hover over side arrows to see different colored tracks");
                    //updateRuleEvidence("Hover over side arrows to see different colored tracks",rule_evidence);

                    break;
                case "LockFlowVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    if ( feature_vector.containsKey("flow_tooltip") ) {
                        double n = feature_vector.get("flow_tooltip");
                        feature_vector.replace("flow_tooltip", n + 1);
                    }
                    break;
                case "hint":
                    analyzer.updateRuleEvidence("Use help bar");
                    //updateRuleEvidence("Use help bar",rule_evidence);
                    break;
                case "TriggerGoalPopUp":
                    if ( data_.contains("Successfully") ) {
                        if ( feature_vector.containsKey("popup_success") ) {
                            double n = feature_vector.get("popup_success");
                            feature_vector.replace("popup_success", n + 1);
                        }
                    } else if ( data_.contains("starvation") ) {
                        if ( feature_vector.containsKey("popup_error_starvation") ) {
                            double n = feature_vector.get("popup_error_starvation");
                            feature_vector.replace("popup_error_starvation", n + 1);
                        }
                        if ( feature_vector.containsKey("popup_error") ) {
                            double n = feature_vector.get("popup_error");
                            feature_vector.replace("popup_error", n + 1);
                        }

                    } else if ( data_.contains("dead") ) {
                        if ( feature_vector.containsKey("popup_error_deadend") ) {
                            double n = feature_vector.get("popup_error_deadend");
                            feature_vector.replace("popup_error_deadend", n + 1);
                        }
                        if ( feature_vector.containsKey("popup_error") ) {
                            double n = feature_vector.get("popup_error");
                            feature_vector.replace("popup_error", n + 1);
                        }
                    } else if ( data_.contains("deliveries") ) {
                        if ( feature_vector.containsKey("popup_error_delivery") ) {
                            double n = feature_vector.get("popup_error_delivery");
                            feature_vector.replace("popup_error_delivery", n + 1);
                        }
                        if ( feature_vector.containsKey("popup_error") ) {
                            double n = feature_vector.get("popup_error");
                            feature_vector.replace("popup_error", n + 1);
                        }
                    } else {
                        if ( feature_vector.containsKey("popup_error_badgoals") ) {
                            double n = feature_vector.get("popup_error_badgoals");
                            feature_vector.replace("popup_error_badgoals", n + 1);
                        }
                        if ( feature_vector.containsKey("popup_error") ) {
                            double n = feature_vector.get("popup_error");
                            feature_vector.replace("popup_error", n + 1);
                        }
                    }
                    break;
            }
            i++;
        }
        /* Compute averages */
        String last_line = telemetry_at_interval.get(telemetry_at_interval.size()-1);
        String [] split_last_line = last_line.split("\t");
        date_ = split_last_line[0];
        String name_ = split_last_line[1];
        String data_ = split_last_line[2];
        double time_ = Double.parseDouble(split_last_line[3]);

        formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );
        feature_vector.replace("end_date",(double)s_);
        s_ = s_ - (Long)all_persistent_data.get(0).persistent_data.get("start_time");

        feature_vector.replace("total_time", s_ - feature_vector.get("start_time"));

        if ( feature_vector.get("total_time") < 0 ) {
            System.out.println("This should never happen");
            System.exit(0);
        }

        double me_total = 0.0;
        String [] tsr = { "tests" , "submissions" ,"replays" };
        for ( String s : tsr  ) {
            me_total += feature_vector.get("me_" + s);
        }

        /* NOTE: I'm not sure how good double comparison is here...but we'll use it for now... */
        if ( Double.compare(feature_vector.get("total_time"),0.0) != 0 ) {
            feature_vector.put("r_me_per_minute", me_total/(feature_vector.get("total_time")/60.0));

            feature_vector.replace("r_num_tracks_used_per_minute", feature_vector.get("num_tracks_used") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_num_track_no_changes_per_minute", feature_vector.get("num_track_no_changes") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_num_track_changes_per_minute", feature_vector.get("num_track_changes") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_mouse_on_comp_per_minute",  feature_vector.get("num_mouse_on_comp") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_dragged_components_per_minute", feature_vector.get("dragged") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_hover_components_per_minute", feature_vector.get("hover") / (feature_vector.get("total_time") / 60.0));
        }

        if ( Double.compare(feature_vector.get("num_mouse_on_comp"),0) != 0 ) {
            feature_vector.replace("avg_time_on_component", feature_vector.get("total_time_on_component") / feature_vector.get("num_mouse_on_comp") );
        }

        if ( Double.compare(feature_vector.get("dragged"),0) != 0 ) {
            feature_vector.replace("avg_dragged_components_dist", feature_vector.get("total_dragged_components_dist") / feature_vector.get("dragged") );
        }

        for ( String s : tsr  ) {
            if (Double.compare(feature_vector.get("total_time"), 0.0) != 0) {
                feature_vector.replace("r_me_" + s + "_per_minute", 1.0 * feature_vector.get("me_" + s) / (feature_vector.get("total_time") / 60.0));
            }

            if ( Double.compare(me_total,0.0) != 0 ) {
                feature_vector.replace("r_me_" + s, feature_vector.get("me_" + s) / me_total);
            }
        }

        String [] tse = { "_tests", "_submissions", "" };
        for ( String s : tse ) {
            ArrayList<Double> lst_ = new ArrayList<Double>();

            for ( String j : list_map.keySet() ) {
                if ( j.contains(s) || s.length() == 0 ) {
                    lst_.addAll(list_map.get(s));
                }
            }

            double avg = 0;
            for ( Double d : lst_ ) {
                avg += d;
            }
            avg = ( avg / (double)lst_.size() );

            if ( lst_.size() != 0 ) {
                feature_vector.replace("t_d_component_dragged_me_" + s + "_avg", avg);
                feature_vector.replace("t_d_component_dragged_me_" + s + "_min", Collections.min(lst_));
            }
        }
        return feature_vector;

    }

    public HashMap< String, Double > extractFeatureVector_v2(String path, ArrayList<String> telemetry_at_interval,
                                                          ArrayList<Integer> index_vector,
                                                          HashMap<Integer, PersistentData > all_persistent_data,
                                                          HashMap<String, ArrayList< String > > data_files) {

        HashMap< String, Double > feature_vector = new HashMap< String, Double >();

        /* Used to store list of values for later computation */
        HashMap< String, ArrayList<Double> > list_map = new HashMap< String, ArrayList<Double> >();

        Pair<Double,Double> ir_coord = new Pair<Double,Double>(), fr_coord = new Pair<Double,Double>();
        Pair<Double,Double> il_coord = new Pair<Double,Double>(), fl_coord = new Pair<Double,Double>();
        Pair<Double,Double> id_coord = new Pair<Double,Double>(), fd_coord = new Pair<Double,Double>();

        if ( telemetry_at_interval.size() == 0 ) {
            return feature_vector;
        }
        /* Initialize feature vector */
        for ( String f : features ) {
            feature_vector.put(f,0.0);
        }

        String [] data = telemetry_at_interval.get(0).split("\t");
        String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

        /* Bad things happen in WEKA if we leave the date as is...so we convert it to the number of seconds since the epoch */
        /* NOTE: We could convert this to a numerical version of the date.. */

        if ( !all_persistent_data.containsKey(0) ) {
            /* This level was not either complete OR doesn't have a file in data/ associate with it. (will end up with inaccurate data
                to train on...) */
            return new HashMap< String, Double >();
        }

        feature_vector.replace("start_date", (double)s_);
        feature_vector.replace("start_time", ((double)s_) - ((Long)all_persistent_data.get(0).persistent_data.get("start_time")).doubleValue() );

        int i = 0;
        for ( String telemetry : telemetry_at_interval ) {
            data = telemetry.split("\t");
            date_ = data[0];
            String name_ = data[1];
            String data_ = data[2];

            double time_ = Double.parseDouble(data[3]);
            double x_ = Double.parseDouble(data[4]);
            double y_ = Double.parseDouble(data[5]);

            formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
            time = LocalDateTime.parse(date_, formatter);

            /* Time in seconds */
            s_ = (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000);

            if ( !all_persistent_data.containsKey(index_vector.get(i)) ) {
                /* This level was not either complete OR doesn't have a file in data/ associate with it. (will end up with inaccurate data
                to train on...) */

                /* Return empty feature vector...don't want inaccurate information */
                return new HashMap< String, Double >();
            }

            HashMap<String, Object > persistent_data = all_persistent_data.get(index_vector.get(i)).persistent_data;
            s_ = s_ - (Long)persistent_data.get("start_time");

            switch (name_) {
                case "TriggerLoadLevel":
                    /* Get level data */
                    if (data_.length() != 0) {
                        if (data_.startsWith("l")) {
                        } else {
                        }
                    } else {
                        double nreplays = feature_vector.get("me_replays");
                        feature_vector.replace("me_replays", nreplays + 1);
                    }
                    break;
                case "endTutorial":
                    feature_vector.replace("tutorial_time", (s_ - feature_vector.get("start_time")) / 60.0);
                    break;
                case "SubmitCurrentLevelPlay":
                    /* This is testing */
                    //persistent_data.replace("test_before_submit",true);
                    double val = feature_vector.get("me_tests");
                    feature_vector.replace("me_tests", val + 1);
                    if ( feature_vector.containsKey("_timestamp_last_dragged_component") ) {
                        double t = s_ - feature_vector.get("_timestamp_last_dragged_component");

                        if (list_map.get("_t_d_component_dragged_me_lst_tests") == null) {
                            ArrayList<Double> tmp = new ArrayList<Double>();
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_tests", tmp);
                        } else {
                            ArrayList<Double> tmp = list_map.get("_t_d_component_dragged_me_lst_tests");
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_tests",tmp);
                        }
                    }
                    break;
                case "SubmitCurrentLevelME":
//                    if ( (Boolean)persistent_data.get("test_before_submit") ) {
//                        analyzer.updateRuleEvidence("Testing before submitting");
//                        persistent_data.replace("test_before_submit",false);
//                    }

                    val = feature_vector.get("me_submissions");
                    feature_vector.replace("me_submissions", val + 1);
                    if (feature_vector.containsKey("_timestamp_last_dragged_component")) {
                        double t = s_ - feature_vector.get("_timestamp_last_dragged_component");
                        if (list_map.get("_t_d_component_dragged_me_lst_submissions") == null) {
                            ArrayList<Double> tmp = new ArrayList<Double>();
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_submissions", tmp);
                        } else {
                            ArrayList<Double> tmp = list_map.get("_t_d_component_dragged_me_lst_submissions");
                            tmp.add(t);
                            list_map.replace("_t_d_component_dragged_me_lst_submissions",tmp);
                        }
                    }
                    break;
                case "tooltip":
                    double n = feature_vector.get("tooltips");
                    feature_vector.replace("tooltips", n + 1);

                    //System.out.println("tooltip_" + data_);
                    if ( feature_vector.containsKey("tooltip_" + data_) ) {
                        n = feature_vector.get("tooltip_" + data_);
                        feature_vector.replace("tooltip_" + data_, n + 1);
                    }
                    break;
                case "startDrag":
                    n = feature_vector.get("dragged");
                    feature_vector.replace("dragged", n + 1);

                    n = feature_vector.get("dragged_" + data_);
                    feature_vector.replace("dragged_" + data_, n + 1);

                    if (!feature_vector.containsKey("_timestamp_last_dragged_component")) {
                        feature_vector.put("_timestamp_last_dragged_component", (double) s_);
                    } else {
                        feature_vector.replace("_timestamp_last_dragged_component", (double) s_);
                    }

                    id_coord.p1 = x_;
                    id_coord.p2 = y_;
                    //persistent_data.replace("dragging", true);
                    break;
                case "endDrag":
                    /* Training dataset didn't have endDrag due to bug (only had startDrag startDrag pair). This is fixed in the testing dataset */
                    analyzer.updateRuleEvidence("Drag objects");
                    //updateRuleEvidence("Drag objects",rule_evidence);

                    fd_coord.p1 = x_;
                    fd_coord.p2 = y_;
                    double dist = 0.0;

                    // Euclidean distance
                    if (id_coord.p1 != null && id_coord.p2 != null) {
                        dist = Math.sqrt(Math.pow((fd_coord.p1 - id_coord.p1), 2.0) + Math.pow((fd_coord.p2 - id_coord.p2), 2.0));
                    } else {
                        dist = 0.0;
                    }

                    id_coord.reset();
                    fd_coord.reset();

                    //if ( (Integer)persistent_data.get("num_dragged") > 2 ) {
                    //    analyzer.updateRuleEvidence("Place objects on track");
                        //updateRuleEvidence("Place objects on the track",rule_evidence);
                    //}

                    persistent_data.replace("num_dragged",(Integer)persistent_data.get("num_dragged") + 1);

                    n = feature_vector.get("total_dragged_components_dist");
                    feature_vector.replace("total_dragged_components_dist", n + dist);
                    //persistent_data.replace("dragging",false);
//                    analyzer.updateRuleEvidence("Drag objects");
//                    //updateRuleEvidence("Drag objects",rule_evidence);
//
//                    fd_coord.p1 = x_;
//                    fd_coord.p2 = y_;
//                    double dist = 0.0;
//
//                    // Euclidean distance
//                    if (id_coord.p1 != null && id_coord.p2 != null) {
//                        dist = Math.sqrt(Math.pow((fd_coord.p1 - id_coord.p1), 2.0) + Math.pow((fd_coord.p2 - id_coord.p2), 2.0));
//                    } else {
//                        dist = 0.0;
//                    }
//
//                    id_coord.reset();
//                    fd_coord.reset();
//
//                    if ( (Integer)persistent_data.get("num_dragged") > 2 ) {
//                        analyzer.updateRuleEvidence("Place objects on the track");
//                        //updateRuleEvidence("Place objects on the track",rule_evidence);
//                    }
//
//                    persistent_data.replace("num_dragged",(Integer)persistent_data.get("num_dragged") + 1);
//
//                    n = feature_vector.get("total_dragged_components_dist");
//                    feature_vector.replace("total_dragged_components_dist", n + dist);
//                    persistent_data.replace("dragging",false);

//                    fd_coord.p1 = x_;
//                    fd_coord.p2 = y_;
//                    double dist = 0.0;
//                    // Euclidean distance
//                    if (id_coord.p1 != null && id_coord.p2 != null) {
//                        dist = Math.sqrt(Math.pow((fd_coord.p1 - id_coord.p1), 2.0) + Math.pow((fd_coord.p2 - id_coord.p2), 2.0));
//                        rule_evidence.replace("Drag objects",1.0);
//                    } else {
//                        dist = 0.0;
//                    }
//
//                    id_coord.reset();
//                    fd_coord.reset();
//
//                    n = feature_vector.get("total_dragged_components_dist");
//                    feature_vector.replace("total_dragged_components_dist", n + dist);

                    break;
                case "Destroying":
                    n = feature_vector.get("trashed");
                    feature_vector.replace("trashed",n+1);

                    n = feature_vector.get("trashed_" + data_);
                    feature_vector.replace("trashed_" + data_,n+1);
                    analyzer.updateRuleEvidence("Remove unnecessary elements");
                    //updateRuleEvidence("Remove unnecessary elements",rule_evidence);
                    break;
                case "OnHoverBehavior":
                    // This implies that we are on a track...but we hover on an object though..?
                    n = feature_vector.get("hover");
                    feature_vector.replace("hover",n+1);

                    n = feature_vector.get("hover_" + data_);
                    feature_vector.replace("hover_" + data_,n+1);

                    analyzer.updateRuleEvidence("Hover over objects to see what they do");
                    //updateRuleEvidence("Hover over objects to see what they do",rule_evidence);

                    break;
                case "BeginReposition":
                    ir_coord.p1 = x_;
                    ir_coord.p2 = y_;
                    break;
                case "EndReposition":
                    if ( ((String)persistent_data.get("cur_mouse_comp")).length() == 0 ) {
                        break;
                    }

                    /* NOTE: While this is okay for training, if this were to actually be deployed into Parallel, we wouldn't have access to a mapping between comoponents
                     * and the color of the track...
                     * */

                    String comp_id =  ((String)persistent_data.get("cur_mouse_comp")).split("/")[1];
                    HashMap<String,String> comp_color_map = (HashMap<String,String>) persistent_data.get("comp_color_map");

                    if ( !comp_color_map.containsKey(comp_id) ) {
                        break;
                    }

                    n = feature_vector.get("num_tracks_used");
                    feature_vector.replace("num_tracks_used",n+1);

                    fr_coord.p1 = x_;
                    fr_coord.p2 = y_;

                    ir_coord.reset();
                    fr_coord.reset();

                    break;
                case "BeginLink":
                    if ( ((String)persistent_data.get("cur_mouse_comp")).length() == 0 ) {
                        break;
                    }

                    /* NOTE: While this is okay for training, if this were to actually be deployed into Parallel, we wouldn't have access to a mapping between components
                     * and the color of the track...
                     * */
                    comp_id =  ((String)persistent_data.get("cur_mouse_comp")).split("/")[1];
                    comp_color_map = (HashMap<String,String>) persistent_data.get("comp_color_map");


                    if ( !comp_color_map.containsKey(comp_id) ) {
                        break;
                    }

                    n = feature_vector.get("num_tracks_used");
                    feature_vector.replace("num_tracks_used",n+1);

                    il_coord.p1 = x_;
                    il_coord.p2 = y_;
                    break;
                case "LinkTo":
                    persistent_data.replace("linking",true);

                    fl_coord.p1 = x_;
                    fl_coord.p2 = y_;

                    dist = 0.0;

                    // Euclidean distance
                    if (il_coord.p1 != null && il_coord.p2 != null) {
                        dist = Math.sqrt(Math.pow((fl_coord.p1 - il_coord.p1), 2.0) + Math.pow((fl_coord.p2 - il_coord.p2), 2.0));
                    } else {
                        dist = 0.0;
                    }


                    il_coord.reset();
                    fl_coord.reset();
                    break;
                case "OnMouseComponent":
                    double num_mouse_comp = feature_vector.get("num_mouse_on_comp");
                    feature_vector.replace("num_mouse_on_comp",num_mouse_comp+1);

                    persistent_data.replace("cur_mouse_comp",data_);
                    persistent_data.replace("cur_mouse_time",time_);

                    /* NOTE: While this is okay for training, if this were to actually be deployed into Parallel, we wouldn't have access to a mapping between components
                     * and their location...
                     * */
                    //HashMap<String,String> comp_loc_map = (HashMap<String,String>) persistent_data.get("comp_loc_map");
                    comp_id = data_.split("/")[1];
//                    if ( comp_loc_map == null ) {
//                        HashMap<String,String> loc_map = new HashMap<String,String>();
//                        loc_map.put(comp_id,String.format("%f,%f",x_,y_));
//                        persistent_data.put("comp_loc_map",loc_map);
//                    } else {
//                        comp_loc_map.put(comp_id,String.format("%f,%f",x_,y_));
//                        persistent_data.put("comp_loc_map",comp_loc_map);
//                    }

                    comp_color_map = (HashMap< String, String >)persistent_data.get("comp_color_map");
                    if ( comp_color_map == null ) {
                        /* This really should never happen.. */
                        break;
                    }

//                    if ( comp_color_map.containsKey(comp_id) ) {
//                        System.out.println(comp_color_map.get(comp_id));
//                    }

                    if ( comp_color_map.size() != 0 && comp_color_map.containsKey(comp_id) ) {
//                        System.out.println(persistent_data.toString());
//                        System.out.println("Current track: " + persistent_data.get("cur_track"));
                        int cur_track = Integer.parseInt(String.valueOf(persistent_data.get("cur_track")));
                        if ( cur_track == -1) {
                            persistent_data.replace("cur_track", Integer.parseInt(comp_color_map.get(comp_id)));
                        } else if ( cur_track != Integer.parseInt(comp_color_map.get(comp_id))) {
                            n = feature_vector.get("num_track_changes");
                            feature_vector.replace("num_track_changes", n + 1);
                            persistent_data.replace("cur_track", comp_color_map.get(comp_id));
                        } else {
                            n = feature_vector.get("num_track_no_changes");
                            feature_vector.replace("num_track_no_changes", n + 1);
                        }
                    }

                    if ( (Boolean)persistent_data.get("linking") ) {
                        /* Check the component */

                        /* Check which component we are linking to
                         *       direction switch -> conditional
                         * */
//                        if ( data_.contains("signal")  ) {
//                            analyzer.updateRuleEvidence("Be able to link semaphores to buttons");
//                            //updateRuleEvidence("Be able to link semaphores to buttons",rule_evidence);
//                        }

                        if ( data_.contains("conditional")  ) {
                            analyzer.updateRuleEvidence("Be able to link buttons to direction switches");
                            //updateRuleEvidence("Be able to link buttons to direction switches",rule_evidence);
                        }

                        if ( comp_color_map.size() != 0 ) {
                            if ( comp_color_map.containsKey(comp_id) ) {
                                n = feature_vector.get("num_tracks_used");
                                feature_vector.replace("num_tracks_used", n + 1);
                            }
                        }
                    }
                    persistent_data.replace("linking",false);
                    break;
                case "OutMouseComponent":
                    //System.out.println(telemetry);
                    double total_time_on_component = feature_vector.get("total_time_on_component");
                    if ( time_ - (Double)persistent_data.get("cur_mouse_time") < 0 ) {
                        /* This actually happens when the cur_mouse_time is taken from a previous time interval, and time_ starts before cur_mouse_time ) */
                        break; /* Don't use this time_ */
                    }
                    feature_vector.replace("total_time_on_component",total_time_on_component + (time_ - (Double)persistent_data.get("cur_mouse_time")));
                    break;
                case "ToggleConnectionVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    n = feature_vector.get("connection_visibility");
                    feature_vector.replace("connection_visibility", n + 1);
                    break;
                case "ToggleFlowVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    n = feature_vector.get("flow_visibility");
                    feature_vector.replace("flow_visibility", n + 1);

                    analyzer.updateRuleEvidence("Hover over side arrows to see different colored tracks");

                    break;
                case "LockFlowVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    n = feature_vector.get("flow_tooltip");
                    feature_vector.replace("flow_tooltip", n + 1);
                    break;
                case "hint":
                    analyzer.updateRuleEvidence("Use help bar");
                    //updateRuleEvidence("Use help bar",rule_evidence);
                    break;
                case "TriggerGoalPopUp":
                    if ( data_.contains("Successfully") ) {
                        n = feature_vector.get("popup_success");
                        feature_vector.replace("popup_success", n + 1);
                    } else if ( data_.contains("starvation") ) {
                        n = feature_vector.get("popup_error_starvation");
                        feature_vector.replace("popup_error_starvation", n + 1);

                        n = feature_vector.get("popup_error");
                        feature_vector.replace("popup_error", n + 1);

                    } else if ( data_.contains("dead") ) {

                        n = feature_vector.get("popup_error_deadend");
                        feature_vector.replace("popup_error_deadend", n + 1);

                        n = feature_vector.get("popup_error");
                        feature_vector.replace("popup_error", n + 1);

                    } else if ( data_.contains("deliveries") ) {
                        n = feature_vector.get("popup_error_delivery");
                        feature_vector.replace("popup_error_delivery", n + 1);

                        n = feature_vector.get("popup_error");
                        feature_vector.replace("popup_error", n + 1);

                    } else {
                        n = feature_vector.get("popup_error_badgoals");
                        feature_vector.replace("popup_error_badgoals", n + 1);

                        n = feature_vector.get("popup_error");
                        feature_vector.replace("popup_error", n + 1);
                    }
                    break;
            }
            i++;
        }
        /* Compute averages */
        String last_line = telemetry_at_interval.get(telemetry_at_interval.size()-1);
        String [] split_last_line = last_line.split("\t");
        date_ = split_last_line[0];
        String name_ = split_last_line[1];
        String data_ = split_last_line[2];
        double time_ = Double.parseDouble(split_last_line[3]);

        formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );
        feature_vector.replace("end_date",(double)s_);
        s_ = s_ - (Long)all_persistent_data.get(0).persistent_data.get("start_time");

        feature_vector.replace("total_time", s_ - feature_vector.get("start_time"));

        if ( feature_vector.get("total_time") < 0 ) {
            System.out.println("This should never happen");
            System.exit(0);
        }

        double me_total = 0.0;
        String [] tsr = { "tests" , "submissions" ,"replays" };
        for ( String s : tsr  ) {
            me_total += feature_vector.get("me_" + s);
        }

        /* NOTE: I'm not sure how good double comparison is here...but we'll use it for now... */
        if ( Double.compare(feature_vector.get("total_time"),0.0) != 0 ) {
            feature_vector.put("r_me_per_minute", me_total/(feature_vector.get("total_time")/60.0));

            feature_vector.replace("r_num_tracks_used_per_minute", feature_vector.get("num_tracks_used") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_num_track_no_changes_per_minute", feature_vector.get("num_track_no_changes") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_num_track_changes_per_minute", feature_vector.get("num_track_changes") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_mouse_on_comp_per_minute",  feature_vector.get("num_mouse_on_comp") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_dragged_components_per_minute", feature_vector.get("dragged") / (feature_vector.get("total_time") / 60.0));
            feature_vector.replace("r_hover_components_per_minute", feature_vector.get("hover") / (feature_vector.get("total_time") / 60.0));
        }

        if ( Double.compare(feature_vector.get("num_mouse_on_comp"),0) != 0 ) {
            feature_vector.replace("avg_time_on_component", feature_vector.get("total_time_on_component") / feature_vector.get("num_mouse_on_comp") );
        }

        if ( Double.compare(feature_vector.get("dragged"),0) != 0 ) {
            feature_vector.replace("avg_dragged_components_dist", feature_vector.get("total_dragged_components_dist") / feature_vector.get("dragged") );
        }

        for ( String s : tsr  ) {
            if (Double.compare(feature_vector.get("total_time"), 0.0) != 0) {
                feature_vector.replace("r_me_" + s + "_per_minute", 1.0 * feature_vector.get("me_" + s) / (feature_vector.get("total_time") / 60.0));
            }

            if ( Double.compare(me_total,0.0) != 0 ) {
                feature_vector.replace("r_me_" + s, feature_vector.get("me_" + s) / me_total);
            }
        }

        String [] tse = { "_tests", "_submissions", "" };
        for ( String s : tse ) {
            ArrayList<Double> lst_ = new ArrayList<Double>();

            for ( String j : list_map.keySet() ) {
                if ( j.contains(s) || s.length() == 0 ) {
                    lst_.addAll(list_map.get(s));
                }
            }

            double avg = 0;
            for ( Double d : lst_ ) {
                avg += d;
            }
            avg = ( avg / (double)lst_.size() );

            if ( lst_.size() != 0 ) {
                feature_vector.replace("t_d_component_dragged_me_" + s + "_avg", avg);
                feature_vector.replace("t_d_component_dragged_me_" + s + "_min", Collections.min(lst_));
            }
        }

        return feature_vector;

    }

    public HashMap< String, Double > extractFeatureVectorPM(ArrayList<String> telemetryData,
                                                            PersistentData levelData) {

        HashMap< String, Double > featureVector = new HashMap< String, Double >();

        /* Used to store list of values for later computation */
        HashMap< String, ArrayList<Double> > statisticsFeatures = new HashMap< String, ArrayList<Double> >();

        Pair<Double,Double> repositionBeginCoord = new Pair<Double,Double>(), repositionEndCoord = new Pair<Double,Double>();
        Pair<Double,Double> il_coord = new Pair<Double,Double>(), fl_coord = new Pair<Double,Double>();
        Pair<Double,Double> dragStartCoord = new Pair<Double,Double>(), dragEndCoord = new Pair<Double,Double>();

        if ( telemetryData.size() == 0 ) {
            return featureVector;
        }
        /* Initialize feature vector */
        for ( String f : features ) {
            featureVector.put(f,0.0);
        }

        String [] data = telemetryData.get(0).split("\t");
        String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long startTime = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

        featureVector.replace("start_date", (double)startTime);
        featureVector.replace("start_time", ((double)startTime) - ((Long)levelData.persistent_data.get("start_time")).doubleValue());

        HashMap<String, Object > levelData_ = levelData.persistent_data;
        HashMap<String,String> colorMap = (HashMap<String,String>) levelData_.get("comp_color_map");

        for ( String telemetry : telemetryData ) {
            data = telemetry.split("\t");
            date_ = data[0];
            String name_ = data[1];
            String data_ = data[2];

            double time_ = Double.parseDouble(data[3]);
            double x_ = Double.parseDouble(data[4]);
            double y_ = Double.parseDouble(data[5]);

            formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
            time = LocalDateTime.parse(date_, formatter);

            /* Time in seconds */
            startTime = (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000);

            startTime = startTime - (Long)levelData_.get("start_time");

            switch (name_) {
                case "TriggerLoadLevel":
                    /* Get level data */
                    if (data_.length() != 0) {
                        if (data_.startsWith("l")) {
                        } else {
                        }
                    } else {
                        double nreplays = featureVector.get("me_replays");
                        featureVector.replace("me_replays", nreplays + 1);
                    }
                    break;
                case "endTutorial":
                    featureVector.replace("tutorial_time", (startTime - featureVector.get("start_time")) / 60.0);
                    break;
                case "SubmitCurrentLevelPlay":
                    /* This is testing */
                    double val = featureVector.get("me_tests");
                    featureVector.replace("me_tests", val + 1);
                    if ( featureVector.containsKey("_timestamp_last_dragged_component") ) {
                        double t = startTime - featureVector.get("_timestamp_last_dragged_component");

                        if (statisticsFeatures.get("_t_d_component_dragged_me_lst_tests") == null) {
                            ArrayList<Double> tmp = new ArrayList<Double>();
                            tmp.add(t);
                            statisticsFeatures.replace("_t_d_component_dragged_me_lst_tests", tmp);
                        } else {
                            ArrayList<Double> tmp = statisticsFeatures.get("_t_d_component_dragged_me_lst_tests");
                            tmp.add(t);
                            statisticsFeatures.replace("_t_d_component_dragged_me_lst_tests",tmp);
                        }
                    }
                    break;
                case "SubmitCurrentLevelME":
                    val = featureVector.get("me_submissions");
                    featureVector.replace("me_submissions", val + 1);
                    if (featureVector.containsKey("_timestamp_last_dragged_component")) {
                        double t = startTime - featureVector.get("_timestamp_last_dragged_component");
                        if (statisticsFeatures.get("_t_d_component_dragged_me_lst_submissions") == null) {
                            ArrayList<Double> tmp = new ArrayList<Double>();
                            tmp.add(t);
                            statisticsFeatures.replace("_t_d_component_dragged_me_lst_submissions", tmp);
                        } else {
                            ArrayList<Double> tmp = statisticsFeatures.get("_t_d_component_dragged_me_lst_submissions");
                            tmp.add(t);
                            statisticsFeatures.replace("_t_d_component_dragged_me_lst_submissions",tmp);
                        }
                    }
                    break;
                case "tooltip":
                    double n = featureVector.get("tooltips");
                    featureVector.replace("tooltips", n + 1);

                    if ( featureVector.containsKey("tooltip_" + data_) ) {
                        n = featureVector.get("tooltip_" + data_);
                        featureVector.replace("tooltip_" + data_, n + 1);
                    }
                    break;
                case "startDrag":
                    n = featureVector.get("dragged");
                    featureVector.replace("dragged", n + 1);

                    n = featureVector.get("dragged_" + data_);
                    featureVector.replace("dragged_" + data_, n + 1);

                    if (!featureVector.containsKey("_timestamp_last_dragged_component")) {
                        featureVector.put("_timestamp_last_dragged_component", (double) startTime);
                    } else {
                        featureVector.replace("_timestamp_last_dragged_component", (double) startTime);
                    }

                    dragStartCoord.p1 = x_;
                    dragStartCoord.p2 = y_;
                    break;
                case "endDrag":
                    analyzer.updateRuleEvidence("Drag objects");
                    dragEndCoord.p1 = x_;
                    dragEndCoord.p2 = y_;
                    double dist;

                    // Euclidean distance
                    if (dragStartCoord.p1 != null && dragStartCoord.p2 != null) {
                        dist = Math.sqrt(Math.pow((dragEndCoord.p1 - dragStartCoord.p1), 2.0) + Math.pow((dragEndCoord.p2 - dragStartCoord.p2), 2.0));
                    } else {
                        dist = 0.0;
                    }

                    dragStartCoord.reset();
                    dragEndCoord.reset();

                    levelData_.replace("num_dragged",(Integer)levelData_.get("num_dragged") + 1);

                    n = featureVector.get("total_dragged_components_dist");
                    featureVector.replace("total_dragged_components_dist", n + dist);
                    break;
                case "Destroying":
                    n = featureVector.get("trashed");
                    featureVector.replace("trashed",n+1);

                    n = featureVector.get("trashed_" + data_);
                    featureVector.replace("trashed_" + data_,n+1);
                    analyzer.updateRuleEvidence("Remove unnecessary elements");
                    break;
                case "OnHoverBehavior":
                    // This implies that we are on a track...but we hover on an object though..?
                    n = featureVector.get("hover");
                    featureVector.replace("hover",n+1);

                    n = featureVector.get("hover_" + data_);
                    featureVector.replace("hover_" + data_,n+1);
                    analyzer.updateRuleEvidence("Hover over objects to see what they do");
                    break;
                case "BeginReposition":
                    repositionBeginCoord.p1 = x_;
                    repositionBeginCoord.p2 = y_;
                    break;
                case "EndReposition":
                    if ( ((String)levelData_.get("cur_mouse_comp")).length() == 0 ) {
                        break;
                    }

                    String componentID =  ((String)levelData_.get("cur_mouse_comp")).split("/")[1];
                    if ( colorMap == null ) {
                        /* This really should never happen.. */
                        break;
                    }

                    if ( !colorMap.containsKey(componentID) ) {
                        break;
                    }

                    n = featureVector.get("num_tracks_used");
                    featureVector.replace("num_tracks_used", n+1);

                    repositionEndCoord.p1 = x_;
                    repositionEndCoord.p2 = y_;

                    repositionBeginCoord.reset();
                    repositionEndCoord.reset();

                    break;
                case "BeginLink":
                    if ( ((String)levelData_.get("cur_mouse_comp")).length() == 0 ) {
                        break;
                    }

                    componentID =  ((String)levelData_.get("cur_mouse_comp")).split("/")[1];
                    if ( colorMap == null ) {
                        /* This really should never happen.. */
                        break;
                    }

                    if ( !colorMap.containsKey(componentID) ) {
                        break;
                    }

                    n = featureVector.get("num_tracks_used");
                    featureVector.replace("num_tracks_used",n+1);

                    il_coord.p1 = x_;
                    il_coord.p2 = y_;
                    break;
                case "LinkTo":
                    levelData_.replace("linking", true);

                    fl_coord.p1 = x_;
                    fl_coord.p2 = y_;

                    il_coord.reset();
                    fl_coord.reset();
                    break;
                case "OnMouseComponent":
                    double numTimesMouseOnComponent = featureVector.get("num_mouse_on_comp");
                    featureVector.replace("num_mouse_on_comp", numTimesMouseOnComponent+1);

                    levelData_.replace("cur_mouse_comp", data_);
                    levelData_.replace("cur_mouse_time", time_);

                    componentID = data_.split("/")[1];

                    colorMap = (HashMap< String, String >)levelData_.get("comp_color_map");
                    if ( colorMap == null ) {
                        /* This really should never happen.. */
                        break;
                    }

                    if ( colorMap.size() != 0 && colorMap.containsKey(componentID) ) {
//                        System.out.println(persistent_data.toString());
//                        System.out.println("Current track: " + persistent_data.get("cur_track"));
                        int curTrack = Integer.parseInt(String.valueOf(levelData_.get("cur_track")));
                        if ( curTrack == -1) {
                            levelData_.replace("cur_track", Integer.parseInt(colorMap.get(componentID)));
                        } else if ( curTrack != Integer.parseInt(colorMap.get(componentID))) {
                            n = featureVector.get("num_track_changes");
                            featureVector.replace("num_track_changes", n + 1);
                            levelData_.replace("cur_track", colorMap.get(componentID));
                        } else {
                            n = featureVector.get("num_track_no_changes");
                            featureVector.replace("num_track_no_changes", n + 1);
                        }
                    }

                    if ( (Boolean)levelData_.get("linking") ) {
                        /* Check the component */

                        /* Check which component we are linking to
                         *       direction switch -> conditional
                         * */
                        if ( data_.contains("conditional")  ) {
                            analyzer.updateRuleEvidence("Be able to link buttons to direction switches");
                        }

                        if ( colorMap.size() != 0 ) {
                            if ( colorMap.containsKey(componentID) ) {
                                n = featureVector.get("num_tracks_used");
                                featureVector.replace("num_tracks_used", n + 1);
                            }
                        }
                    }
                    levelData_.replace("linking",false);
                    break;
                case "OutMouseComponent":
                    //System.out.println(telemetry);
                    double totalTimeOnComponent = featureVector.get("total_time_on_component");
                    if ( time_ - (Double)levelData_.get("cur_mouse_time") < 0 ) {
                        /* This actually happens when the cur_mouse_time is taken from a previous time interval, and time_ starts before cur_mouse_time ) */
                        break; /* Don't use this time_ */
                    }
                    featureVector.replace("total_time_on_component",totalTimeOnComponent + (time_ - (Double)levelData_.get("cur_mouse_time")));
                    break;
                case "ToggleConnectionVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    n = featureVector.get("connection_visibility");
                    featureVector.replace("connection_visibility", n + 1);
                    break;
                case "ToggleFlowVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    n = featureVector.get("flow_visibility");
                    featureVector.replace("flow_visibility", n + 1);
                    analyzer.updateRuleEvidence("Hover over side arrows to see different colored tracks");

                    break;
                case "LockFlowVisibility":
                    if ( !data_.equals("True") ) {
                        break;
                    }
                    n = featureVector.get("flow_tooltip");
                    featureVector.replace("flow_tooltip", n + 1);
                    break;
                case "hint":
                    analyzer.updateRuleEvidence("Use help bar");
                    break;
                case "TriggerGoalPopUp":
                    if ( data_.contains("Successfully") ) {
                        n = featureVector.get("popup_success");
                        featureVector.replace("popup_success", n + 1);
                    } else if ( data_.contains("starvation") ) {
                        n = featureVector.get("popup_error_starvation");
                        featureVector.replace("popup_error_starvation", n + 1);

                        n = featureVector.get("popup_error");
                        featureVector.replace("popup_error", n + 1);

                    } else if ( data_.contains("dead") ) {

                        n = featureVector.get("popup_error_deadend");
                        featureVector.replace("popup_error_deadend", n + 1);

                        n = featureVector.get("popup_error");
                        featureVector.replace("popup_error", n + 1);

                    } else if ( data_.contains("deliveries") ) {
                        n = featureVector.get("popup_error_delivery");
                        featureVector.replace("popup_error_delivery", n + 1);

                        n = featureVector.get("popup_error");
                        featureVector.replace("popup_error", n + 1);

                    } else {
                        n = featureVector.get("popup_error_badgoals");
                        featureVector.replace("popup_error_badgoals", n + 1);

                        n = featureVector.get("popup_error");
                        featureVector.replace("popup_error", n + 1);
                    }
                    break;
            }
        }
        /* Compute averages */
        String lastLine = telemetryData.get(telemetryData.size()-1);
        String [] splitLastLine = lastLine.split("\t");
        date_ = splitLastLine[0];
        formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long endTime = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );
        featureVector.replace("end_date", (double)endTime);
        long elapsedTimeFromStartOfRun = endTime - (Long)levelData_.get("start_time");

        featureVector.replace("total_time", elapsedTimeFromStartOfRun - featureVector.get("start_time"));

        if ( featureVector.get("total_time") < 0 ) {
            System.out.println("This should never happen");
            System.exit(0);
        }

        double meTotal = 0.0;
        String [] tsr = { "tests" , "submissions" ,"replays" };
        for ( String s : tsr  ) {
            meTotal += featureVector.get("me_" + s);
        }

        /* NOTE: I'm not sure how good double comparison is here...but we'll use it for now... */
        if ( Double.compare(featureVector.get("total_time"),0.0) != 0 ) {
            featureVector.put("r_me_per_minute", meTotal/(featureVector.get("total_time")/60.0));

            featureVector.replace("r_num_tracks_used_per_minute", featureVector.get("num_tracks_used") / (featureVector.get("total_time") / 60.0));
            featureVector.replace("r_num_track_no_changes_per_minute", featureVector.get("num_track_no_changes") / (featureVector.get("total_time") / 60.0));
            featureVector.replace("r_num_track_changes_per_minute", featureVector.get("num_track_changes") / (featureVector.get("total_time") / 60.0));
            featureVector.replace("r_mouse_on_comp_per_minute",  featureVector.get("num_mouse_on_comp") / (featureVector.get("total_time") / 60.0));
            featureVector.replace("r_dragged_components_per_minute", featureVector.get("dragged") / (featureVector.get("total_time") / 60.0));
            featureVector.replace("r_hover_components_per_minute", featureVector.get("hover") / (featureVector.get("total_time") / 60.0));
        }

        if ( Double.compare(featureVector.get("num_mouse_on_comp"),0) != 0 ) {
            featureVector.replace("avg_time_on_component", featureVector.get("total_time_on_component") / featureVector.get("num_mouse_on_comp") );
        }

        if ( Double.compare(featureVector.get("dragged"),0) != 0 ) {
            featureVector.replace("avg_dragged_components_dist", featureVector.get("total_dragged_components_dist") / featureVector.get("dragged") );
        }

        for ( String s : tsr  ) {
            if (Double.compare(featureVector.get("total_time"), 0.0) != 0) {
                featureVector.replace("r_me_" + s + "_per_minute", 1.0 * featureVector.get("me_" + s) / (featureVector.get("total_time") / 60.0));
            }

            if ( Double.compare(meTotal,0.0) != 0 ) {
                featureVector.replace("r_me_" + s, featureVector.get("me_" + s) / meTotal);
            }
        }

        String [] tse = { "_tests", "_submissions", "" };
        for ( String s : tse ) {
            ArrayList<Double> lst_ = new ArrayList<Double>();

            for ( String j : statisticsFeatures.keySet() ) {
                if ( j.contains(s) || s.length() == 0 ) {
                    lst_.addAll(statisticsFeatures.get(s));
                }
            }

            double avg = 0;
            for ( Double d : lst_ ) {
                avg += d;
            }
            avg = ( avg / (double)lst_.size() );

            if ( lst_.size() != 0 ) {
                featureVector.replace("t_d_component_dragged_me_" + s + "_avg", avg);
                featureVector.replace("t_d_component_dragged_me_" + s + "_min", Collections.min(lst_));
            }
        }
        return featureVector;
    }
}
