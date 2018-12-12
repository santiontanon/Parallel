package playermodeling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pmutils.Pair;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;


/**
 * Created by pavankantharaju on 2/26/18.
 */
public class FeatureExtractor {

    private static final Logger logger = LogManager.getLogger(FeatureExtractor.class);

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

    public static LinkedHashMap< String, Double > buildFeatureVectorLegacy(ArrayList<String> telemetryData,
                                                                     LevelData levelData, SkillAnalyzer analyzer) {
        logger.info("Constructing feature vector!");
        LinkedHashMap< String, Double > featureVector = new LinkedHashMap< String, Double >();

        if ( telemetryData.size() == 0 ) {
            logger.warn("There is no data. Returning empty feature vector!");
            return featureVector;
        }
        /* Initialize feature vector */
        for ( String f : features ) {
            featureVector.put(f,0.0);
        }

        /* Used to store list of values for later computation */
        HashMap< String, ArrayList<Double> > statisticsFeatures = new HashMap< String, ArrayList<Double> >();

        Pair<Double,Double> repositionBeginCoord = new Pair<Double,Double>(), repositionEndCoord = new Pair<Double,Double>();
        Pair<Double,Double> linkBeginCoord = new Pair<Double,Double>(), linkEndCoord = new Pair<Double,Double>();
        Pair<Double,Double> dragStartCoord = new Pair<Double,Double>(), dragEndCoord = new Pair<Double,Double>();

        String [] data = telemetryData.get(0).split("\t");
        String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long startTime = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

        featureVector.replace("start_date", (double)startTime);
        featureVector.replace("start_time", ((double)startTime) - ((Long)levelData.data.get("start_time")).doubleValue());

        HashMap<String, Object > levelData_ = levelData.data;
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
            logger.trace(String.format("Event Name: %s, Date: %s, Time %d, Event Data: %s, (x,y): (%f,%f)", name_, date_, startTime, data_, x_, y_));
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
                    if ( (Boolean)levelData_.get("dragging") ) {
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

                        levelData_.replace("dragging",false);
                    } else {
                        if ( featureVector.containsKey("dragged") ) {
                            n = featureVector.get("dragged");
                            featureVector.replace("dragged", n + 1);
                        }
                        if ( featureVector.containsKey("dragged_" + data_) ) {
                            n = featureVector.get("dragged_" + data_);
                            featureVector.replace("dragged_" + data_, n + 1);
                        }

                        if (!featureVector.containsKey("_timestamp_last_dragged_component")) {
                            featureVector.put("_timestamp_last_dragged_component", (double) startTime);
                        } else {
                            featureVector.replace("_timestamp_last_dragged_component", (double) startTime);
                        }

                        dragStartCoord.p1 = x_;
                        dragStartCoord.p2 = y_;
                        levelData_.replace("dragging",true);
                    }
                    break;
                case "endDrag":
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

                    linkBeginCoord.p1 = x_;
                    linkBeginCoord.p2 = y_;
                    break;
                case "LinkTo":
                    levelData_.replace("linking", true);

                    linkEndCoord.p1 = x_;
                    linkEndCoord.p2 = y_;

                    linkBeginCoord.reset();
                    linkEndCoord.reset();
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

    public static LinkedHashMap< String, Double > buildFeatureVector(ArrayList<String> telemetryData,
                                                              LevelData levelData, SkillAnalyzer analyzer) {
        logger.info("Constructing feature vector!");
        LinkedHashMap< String, Double > featureVector = new LinkedHashMap< String, Double >();

        if ( telemetryData.size() == 0 ) {
            logger.warn("There is no data. Returning empty feature vector!");
            return featureVector;
        }
        /* Initialize feature vector */
        for ( String f : features ) {
            featureVector.put(f,0.0);
        }

        /* Used to store list of values for later computation */
        HashMap< String, ArrayList<Double> > statisticsFeatures = new HashMap< String, ArrayList<Double> >();

        Pair<Double,Double> repositionBeginCoord = new Pair<Double,Double>(), repositionEndCoord = new Pair<Double,Double>();
        Pair<Double,Double> linkBeginCoord = new Pair<Double,Double>(), linkEndCoord = new Pair<Double,Double>();
        Pair<Double,Double> dragStartCoord = new Pair<Double,Double>(), dragEndCoord = new Pair<Double,Double>();

        String [] data = telemetryData.get(0).split("\t");
        String date_ = data[0];

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");
        LocalDateTime time = LocalDateTime.parse(date_,formatter);

        /* Time in seconds */
        long startTime = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

        featureVector.replace("start_date", (double)startTime);
        featureVector.replace("start_time", ((double)startTime) - ((Long)levelData.data.get("start_time")).doubleValue());

        HashMap<String, Object > levelData_ = levelData.data;
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
            logger.trace(String.format("Event Name: %s, Date: %s, Time %d, Event Data: %s, (x,y): (%f,%f)", name_, date_, startTime, data_, x_, y_));
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

                    linkBeginCoord.p1 = x_;
                    linkBeginCoord.p2 = y_;
                    break;
                case "LinkTo":
                    levelData_.replace("linking", true);

                    linkEndCoord.p1 = x_;
                    linkEndCoord.p2 = y_;

                    linkBeginCoord.reset();
                    linkEndCoord.reset();
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
