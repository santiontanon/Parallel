package playermodeling;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.time.Instant;
import java.math.*;


/**
 * Created by pavankantharaju on 2/26/18.
 */
public class FeatureExtraction {

    public String[] features = {
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

            "total_components",                             // Total number of components over all used track...
            "tracks_used_components_per_track_avg",         // Total number of components over all used track / tracks used
            "avg_dragged_components_dist",
            "total_dragged_components_dist"
            /* "total_reposition_dist"   Total reposition euclidean distance */
    };

    public String[] additional_features = {
            "_timestamp_last_dragged_component",
            "_timestamp_last_dragged_component",
            "_timestamp_last_connected_component",
            "_track_color_last_placed_component",
            "_t_d_component_dragged_me_lst",
            "_tracks_used_lst",
    };

    public FeatureExtraction() {

    }

    private HashMap<String,String> parseComponentInformation( String comp_info ) {
        /* TODO: Check whether this is actually right */
        HashMap< String, String > info = new HashMap< String, String >();

        String pruned = comp_info.substring(1,comp_info.length()-1);
        pruned = pruned.replace("\"","");
        String brackets = "\\[\\]";
        String regex = String.format(",(?=([^%s]*:\\[.*\\])*[^%s]*$)",brackets,brackets);
        String [] tmp = pruned.split(regex);
        //String [] tmp = str.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        //String [] tmp = pruned.split(",(?=([^\"]*:\".*\")*[^\"]*$)");
        //String [] tmp = pruned.split(",");
        for ( int i = 0; i < tmp.length; i++ ) {
            String [] tmp2 = tmp[i].split(":");
            //System.out.println("tmp: " + pruned + "&" + tmp[i]);
            //System.out.println("Saved: " + saved);
            info.put(tmp2[0],tmp2[1]);
        }
//        System.out.println("Component data: " + comp_info);
//        System.out.println("--------------------------------------");
//        for ( String s : info.keySet() ) {
//            System.out.println("key,value: " + s + ":" + info.get(s));
//        }
//        System.out.println("--------------------------------------");
        return info;
    }


    /* Making a decision that all attributes must be numerical.... */
    public HashMap< String, Double > extractFeatureVector(String path, ArrayList<String> telemetry_at_interval, HashMap< String, Object > persistent_data,
                                                          HashMap<String, ArrayList< String > > data_files) {
        HashMap< String, Double > feature_vector = new HashMap< String, Double >();

        HashMap< String, ArrayList<Double> > list_map = new HashMap< String, ArrayList<Double> >();

        Pair<Double,Double> ir_coord = new Pair<Double,Double>();
        Pair<Double,Double> fr_coord = new Pair<Double,Double>();

        Pair<Double,Double> il_coord = new Pair<Double,Double>();
        Pair<Double,Double> fl_coord = new Pair<Double,Double>();

        Pair<Double,Double> id_coord = new Pair<Double,Double>();
        Pair<Double,Double> fd_coord = new Pair<Double,Double>();

        /* Initialize feature vector */
        for ( String f : features ) {
            feature_vector.put(f,0.0);
        }

//        feature_vector['level'] = "slice"
//        feature_vector['tutorial_time'] = 0.0

        if ( telemetry_at_interval.size() == 0 ) {
            return feature_vector;
        }

        String [] data = telemetry_at_interval.get(0).split("\t");
        String date_ = data[0], name_ = data[1], data_ = data[2];
        double time_ = Double.parseDouble(data[3]);
        double x_ = Double.parseDouble(data[4]);
        double y_ = Double.parseDouble(data[5]);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

        if ( !persistent_data.containsKey("start_time") ) {
            persistent_data.put("start_time", s_ );
        }

//        feature_vector['start_date'] = date_
//        feature_vector['start_time'] = float(s_ - persistent_data['start_time'])
//
//        feature_vector['_t_d_component_dragged_me_lst'] = []
//        feature_vector['_timestamp_last_dragged_component'] = 0.0
//
//        start_time = None
//        missing_files = []

        for ( String telemetry : telemetry_at_interval ) {
            data = telemetry.split("\t");
            date_ = data[0];
            name_ = data[1];
            data_ = data[2];

            time_ = Double.parseDouble(data[3]);
            x_ = Double.parseDouble(data[4]);
            y_ = Double.parseDouble(data[5]);

            formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
            time = LocalDateTime.parse(date_, formatter);

            /* Time in seconds */
            s_ = (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000);
            s_ = s_ - (Long)persistent_data.get("start_time");

            switch (name_) {
                case "TriggerLoadLevel":
                    /* Get level data */

                    if (data_.length() != 0) {
                        if (data_.startsWith("l")) {

                            Long start_time = (Long)persistent_data.get("start_time");

                            persistent_data.clear();

                            persistent_data.put("start_time", start_time);
                            persistent_data.put("cur_track", -1);
                            persistent_data.put("cur_mouse_comp", "");
                            persistent_data.put("cur_mouse_time", 0.0);
                            persistent_data.put("linking", false);

                            persistent_data.put("direction_layout",new ArrayList<String>());
                            persistent_data.put("color_layout",new ArrayList<String>());
                            persistent_data.put("comp_link_map",new HashMap<String, String >());
                            persistent_data.put("comp_color_map",new HashMap<String, String >());


                            //level_num = data_.replace('level', '').replace('-', '')
                            //if level_num: data['seq'].append(level_num)
                            //data['levels'] += 1
                        } else {
                            Long start_time = (Long)persistent_data.get("start_time");

                            persistent_data.clear();

                            persistent_data.put("start_time", start_time);
                            persistent_data.put("cur_track", -1);
                            persistent_data.put("cur_mouse_comp", "");
                            persistent_data.put("cur_mouse_time", 0.0);
                            persistent_data.put("linking", false);

                            persistent_data.put("direction_layout",new ArrayList<String>());
                            persistent_data.put("color_layout",new ArrayList<String>());
                            persistent_data.put("comp_link_map",new HashMap<String, String >());
                            persistent_data.put("comp_color_map",new HashMap<String, String >());

                            if (data_files.containsKey(data_)) {
                                try {
                                    BufferedReader br = new BufferedReader(new FileReader(path + "/data/" + data_files.get(data_).get(0)));
                                    String line = "";
                                    int incr = 0;
                                    int board_width = 0, board_height = 0;
                                    boolean start_parse = false;

                                    while ((line = br.readLine()) != null) {
                                        if (line.startsWith("board_width")) {
                                            board_width = Integer.parseInt(line.split("\t")[1]);
                                        }

                                        if (line.startsWith("board_height")) {
                                            board_height = Integer.parseInt(line.split("\t")[1]);
                                        }

                                        if (line.startsWith("DIRECTIONS")) {
                                            ArrayList<String> board = (ArrayList<String>)persistent_data.get("direction_layout");
                                            for (int i = incr + 1; i < incr + board_height + 1; i++) {
                                                board.add(br.readLine());
                                            }
                                            persistent_data.replace("direction_layout", board);
                                        }

                                        if (line.startsWith("COLORS")) {
                                            ArrayList<String> color = (ArrayList<String>)persistent_data.get("color_layout");
                                            for (int i = incr + 1; i < incr + board_height + 1; i++) {
                                                color.add(br.readLine());
                                            }
                                            persistent_data.replace("color_layout", color);
                                        }

                                        if (line.startsWith("COMPONENTS")) {
                                            start_parse = true;
                                        } else {
                                            if (start_parse) {
                                                if (line.length() == 0) {
                                                    start_parse = false;
                                                } else {
                                                    String[] comp_info = line.split("\t");
                                                    /* Actual information about the component is located at index 6 */
                                                    HashMap<String, String> info = parseComponentInformation(comp_info[6]);

                                                    if (info.containsKey("color")) {
                                                        if ( persistent_data.containsKey("comp_color_map") ) {
                                                            HashMap<String, String > color_map = new HashMap<String,String>();
                                                            color_map.put(comp_info[0],info.get("color"));
                                                            persistent_data.replace("comp_color_map",color_map);
                                                        }
                                                        //persistent_data.put("comp_color_map") + comp_info[0], info.get("color"));
                                                    }

                                                    if (info.containsKey("link")) {
                                                        if ( persistent_data.containsKey("comp_link_map") ) {
                                                            HashMap<String, String > color_map = new HashMap<String,String>();
                                                            color_map.put(comp_info[0],info.get("link"));
                                                            persistent_data.replace("comp_link_map",color_map);
                                                        }
                                                        //persistent_data.put("comp_link_map_ " + comp_info[0], info.get("link"));
                                                    }

                                                }
                                            }
                                        }
                                        incr++;
                                    }
                                    br.close();
                                    // data['uploaded'] += 1
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                            } else {
                                /* File's missing */
                                double n = feature_vector.get("me_replays");
                                feature_vector.replace("me_replays", n + 1);
                            }
                        }
                    } else {
                        double nreplays = feature_vector.get("me_replays");
                        feature_vector.replace("me_replays", nreplays + 1);
                    }
                    break;
                case "endTutorial":
                    feature_vector.replace("tutorial_time", (s_ - feature_vector.get("start_time")) / 60.0);
                    //feature_vector['tutorial_time'] = (float(s_) - feature_vector['start_time']) / 60.0
                    break;
                case "SubmitCurrentLevelPlay":
                    double val = feature_vector.get("me_tests");
                    feature_vector.replace("me_tests", val + 1);
                    if (feature_vector.containsKey("_timestamp_last_dragged_component")) {
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

                    n = feature_vector.get("tooltip_" + data_);
                    feature_vector.replace("tooltip_" + data_, n + 1);
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
                    break;
                case "endDrag":
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
                    n = feature_vector.get("total_dragged_components_dist");
                    feature_vector.replace("total_dragged_components_dist", n + dist);

                    break;
                case "Destroying":
                    n = feature_vector.get("trashed");
                    feature_vector.replace("trashed",n+1);

                    n = feature_vector.get("trashed_" + data_);
                    feature_vector.replace("trashed_" + data_,n+1);
                    break;
                case "OnHoverBehavior":
                    // This implies that we are on a track...but we hover on an object though..?
                    n = feature_vector.get("hover");
                    feature_vector.replace("hover",n+1);

                    n = feature_vector.get("hover_" + data_);
                    feature_vector.replace("hover_" + data_,n+1);

                    break;
                case "BeginReposition":
                    ir_coord.p1 = x_;
                    ir_coord.p2 = y_;
                    break;
                case "EndReposition":
                    if ( ((String)persistent_data.get("cur_mouse_comp")).length() == 0 ) {
                        continue;
                    }
                    String comp_id =  ((String)persistent_data.get("cur_mouse_comp")).split("/")[1];
                    HashMap<String,String> tmp = (HashMap<String,String>) persistent_data.get("comp_color_map");

                    if ( !tmp.containsKey(comp_id) ) {
                        continue;
                    }

                    n = feature_vector.get("num_tracks_used");
                    feature_vector.replace("num_tracks_used",n+1);

                    double total_components = feature_vector.get("total_components");

                    String color = tmp.get(comp_id);
                    for ( String c : tmp.keySet() ) {
                        if ( color.equals( tmp.get(c)) ) {
                            total_components++;
                        }
                    }
                    feature_vector.replace("total_components",total_components);

                    fr_coord.p1 = x_;
                    fr_coord.p2 = y_;

                    ir_coord.reset();
                    fr_coord.reset();

                    break;
                case "BeginLink":
                    if ( ((String)persistent_data.get("cur_mouse_comp")).length() == 0 ) {
                        continue;
                    }
                    comp_id =  ((String)persistent_data.get("cur_mouse_comp")).split("/")[1];
                    tmp = (HashMap<String,String>) persistent_data.get("comp_color_map");


                    if ( !tmp.containsKey(comp_id) ) {
                        continue;
                    }

                    n = feature_vector.get("num_tracks_used");
                    feature_vector.replace("num_tracks_used",n+1);

                    total_components = feature_vector.get("total_components");

                    color = tmp.get(comp_id);
                    for ( String c : tmp.keySet() ) {
                        if ( color.equals( tmp.get(c)) ) {
                            total_components++;
                        }
                    }
                    feature_vector.replace("total_components",total_components);

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

                    tmp = (HashMap<String,String>) persistent_data.get("comp_loc_map");
                    comp_id = data_.split("/")[1];
                    if ( tmp == null ) {
                        HashMap<String,String> loc_map = new HashMap<String,String>();
                        loc_map.put(comp_id,String.format("%f,%f",x_,y_));
                        persistent_data.put("comp_loc_map",loc_map);
                    } else {
                        tmp.put(comp_id,String.format("%f,%f",x_,y_));
                        persistent_data.put("comp_loc_map",tmp);
                    }

                    /* TODO: Finish translating this */
                    HashMap< String, String > color_map = (HashMap< String, String >)persistent_data.get("comp_color_map");
                    if ( color_map == null ) {
                        continue;
                    }

                    if ( color_map.size() != 0 && color_map.containsKey(comp_id) ) {
                        if ((Integer) persistent_data.get("cur_track") == -1) {
                            persistent_data.replace("cur_track", Integer.parseInt(color_map.get(comp_id)));
                        } else if ((Integer) persistent_data.get("cur_track") != Integer.parseInt(color_map.get(comp_id))) {
                            n = feature_vector.get("num_track_changes");
                            feature_vector.replace("num_track_changes", n + 1);
                            persistent_data.replace("cur_track", color_map.get(comp_id));
                        } else {
                            n = feature_vector.get("num_track_no_changes");
                            feature_vector.replace("num_track_no_changes", n + 1);
                        }
                    }

                    if ( (Boolean)persistent_data.get("linking") ) {
                            if ( color_map.size() != 0 ) {
                                if ( color_map.containsKey(comp_id) ) {
                                    n = feature_vector.get("num_tracks_used");
                                    feature_vector.replace("num_tracks_used", n + 1);

                                    total_components = feature_vector.get("total_components");

                                    color = tmp.get(comp_id);
                                    for (String c : tmp.keySet()) {
                                        if (color.equals(tmp.get(c))) {
                                            total_components++;
                                        }
                                    }
                                    feature_vector.replace("total_components", total_components);
                                }
                            }
                    }
                    persistent_data.replace("linking",false);
                    break;
                case "OutMouseComponent":
                    double total_time_on_component = feature_vector.get("total_time_on_component");
                    feature_vector.replace("total_time_on_component",total_time_on_component + (time_ - (Double)persistent_data.get("cur_mouse_time")));
                    break;
                case "ToggleConnectionVisibility":
                    if ( !data_.equals("True") ) {
                        continue;
                    }
                    n = feature_vector.get("connection_visibility");
                    feature_vector.replace("connection_visibility", n + 1);
                    break;
                case "ToggleFlowVisibility":
                    if ( !data_.equals("True") ) {
                        continue;
                    }
                    n = feature_vector.get("flow_visibility");
                    feature_vector.replace("flow_visibility", n + 1);
                    break;
                case "LockFlowVisibility":
                    if ( !data_.equals("True") ) {
                        continue;
                    }
                    n = feature_vector.get("flow_tooltip");
                    feature_vector.replace("flow_tooltip", n + 1);
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
        }
        /* Compute averages */
        String last_line = telemetry_at_interval.get(telemetry_at_interval.size()-1);
        String [] split_last_line = last_line.split("\t");
        date_ = split_last_line[0];
        name_ = split_last_line[1];
        data_ = split_last_line[2];
        time_ = Double.parseDouble(data[3]);

        formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );
        feature_vector.replace("end_date",(double)s_);
        s_ = s_ - (Long)persistent_data.get("start_time");

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

        if ( Double.compare(feature_vector.get("total_time"),0.0) != 0 ) {
            feature_vector.put("r_me_per_minute", me_total/(feature_vector.get("total_time")/60.0));

            feature_vector.put("r_num_tracks_used_per_minute", feature_vector.get("num_tracks_used") / (feature_vector.get("total_time") / 60.0));
            feature_vector.put("r_num_track_no_changes_per_minute", feature_vector.get("num_track_no_changes") / (feature_vector.get("total_time") / 60.0));
            feature_vector.put("r_num_track_changes_per_minute", feature_vector.get("num_track_changes") / (feature_vector.get("total_time") / 60.0));
            feature_vector.put("r_mouse_on_comp_per_minute",  feature_vector.get("num_mouse_on_comp") / (feature_vector.get("total_time") / 60.0));
            feature_vector.put("r_dragged_components_per_minute", feature_vector.get("dragged") / (feature_vector.get("total_time") / 60.0));
            feature_vector.put("r_hover_components_per_minute", feature_vector.get("hover") / (feature_vector.get("total_time") / 60.0));
        }

        if ( Double.compare(feature_vector.get("num_mouse_on_comp"),0) != 0 ) {
            feature_vector.put("avg_time_on_component", feature_vector.get("total_time_on_component") / feature_vector.get("num_mouse_on_comp") );
        }

        if ( Double.compare(feature_vector.get("dragged"),0) != 0 ) {
            feature_vector.put("avg_dragged_components_dist", feature_vector.get("total_dragged_components_dist") / feature_vector.get("dragged") );
        }
        if ( Double.compare(feature_vector.get("num_tracks_used"),0) != 0 ) {
            feature_vector.put("tracks_used_components_per_track_avg", feature_vector.get("total_components") / feature_vector.get("num_tracks_used") );
        }

        for ( String s : tsr  ) {
            if (Double.compare(feature_vector.get("total_time"), 0.0) != 0) {
                feature_vector.put("r_me_" + s + "_per_minute", 1.0 * feature_vector.get("me_" + s) / (feature_vector.get("total_time") / 60.0));
            }

            if ( Double.compare(me_total,0.0) != 0 ) {
                feature_vector.put("r_me_" + s, feature_vector.get("me_" + s) / me_total);
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
                feature_vector.put("t_d_component_dragged_me" + s + "_avg", avg);
                feature_vector.put("t_d_component_dragged_me" + s + "_min", Collections.min(lst_));
            }
        }

        return feature_vector;

    }
}
