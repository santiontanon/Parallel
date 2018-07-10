package playermodeling;

/**
 * Created by pavankantharaju on 2/26/18.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TelemetryUtils {

    public static int DEBUG = 0;
    public final String data_file_dir = "data_files/";

    private String log_dir;
    private LinkedHashMap<String, Pair<String,String> > week_to_time_interval_str;
    private LinkedHashMap<String, ArrayList< String > > week_to_levels;

    public TelemetryUtils(String path) {
        log_dir = path + "/log/";

        /* NOTE: For now, this is hard-coded ( 8 dash data is structured differently ). I will remove this once we get more data */
        week_to_levels = new LinkedHashMap<String, ArrayList<String> >();
        week_to_levels.put("Week 4", new ArrayList<String>());
        week_to_levels.put("Week 3", new ArrayList<String>());
        week_to_levels.put("Week 2", new ArrayList<String>());
        week_to_levels.put("Week 1", new ArrayList<String>());

        try {
            BufferedReader br = new BufferedReader(new FileReader(data_file_dir + "week_to_levels.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] tmp = line.split(",");
                ArrayList<String> levels = week_to_levels.get(tmp[0]);
                for (int i = 1; i < tmp.length; i++) {
                    levels.add(tmp[i].trim());
                }
                //System.out.println(String.join(",",levels));
                week_to_levels.replace(tmp[0], levels);
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        /*
            Week 1: April 10- April 17 ( 04-10-18 - 04-17-18 )
            Week 2: April 19- April 27 ( 04-19-18 - 04-27-18 )
            Week 3: April 26- May 1 ( 04-26-18 - 05-01-18 )
            Week 4: May 17-May 25 ( 05-17-18 - 05-25-18 )
        */
        week_to_time_interval_str = new LinkedHashMap<String, Pair<String,String> >();
        week_to_time_interval_str.put("Week 1", new Pair<String,String>("04-10-18-00-00-00","04-17-18-23-59-59"));
        week_to_time_interval_str.put("Week 2", new Pair<String,String>("04-19-18-00-00-00","04-27-18-23-59-59"));
        week_to_time_interval_str.put("Week 3", new Pair<String,String>("04-26-18-00-00-00","05-01-18-23-59-59"));
        week_to_time_interval_str.put("Week 4", new Pair<String,String>("05-17-18-00-00-00","05-25-18-23-59-59"));

    }

    public HashMap< String, LinkedHashMap< String, ArrayList<String> > > getPlayers_v2() {

        LinkedHashMap<String, Pair<Long,Long> > week_to_time_interval = new LinkedHashMap<String, Pair<Long,Long> >();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yy-HH-mm-ss");

        for ( String week : week_to_time_interval_str.keySet() ) {
            if ( DEBUG > 0 ) {
                System.out.println("Week: " + week + ", Time interval: " + week_to_time_interval_str.get(week).p1 + "," + week_to_time_interval_str.get(week).p2);
            }
            LocalDateTime start_time = LocalDateTime.parse(week_to_time_interval_str.get(week).p1,formatter);
            LocalDateTime end_time = LocalDateTime.parse(week_to_time_interval_str.get(week).p2,formatter);

            /* Time in seconds */
            long st_ = ( start_time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );
            long et_ = ( end_time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

            week_to_time_interval.put(week, new Pair<Long,Long>(st_,et_));
        }

        HashMap< String, LinkedHashMap< String, ArrayList<String> > > players = new HashMap< String, LinkedHashMap< String, ArrayList<String> > >();
        File directory = new File(log_dir);
        File [] dir_list = directory.listFiles();
        for ( File f : dir_list ) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line;
                String name = "";
                String date = "";
                ArrayList<String> levels_played = new ArrayList<String>();
                while ( (line = br.readLine() ) != null ) {
                    String [] tmp = line.split("\t");
                    if ( tmp.length != 6 ) {
                        continue;
                    }
                    if ( tmp[1].equals("SessionUser") ) {
                        name = tmp[2];
                        date = tmp[0];
                    }

                    if ( tmp[1].equals("TriggerLoadLevel") && tmp[2].startsWith("l") ) {
                        levels_played.add(tmp[2]);
                    }
                }

                if ( !players.containsKey(name) ) {
                    LinkedHashMap<String, ArrayList<String> > week_to_files = new LinkedHashMap<String, ArrayList<String> > ();
                    week_to_files.put("Week 1",new ArrayList<String>());
                    week_to_files.put("Week 2",new ArrayList<String>());
                    week_to_files.put("Week 3",new ArrayList<String>());
                    week_to_files.put("Week 4",new ArrayList<String>());

                    LocalDateTime time = LocalDateTime.parse(date,formatter);
                    /* Time in seconds */
                    long s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

                    for ( String week : week_to_levels.keySet() ) {
                        long start = week_to_time_interval.get(week).p1;
                        long end = week_to_time_interval.get(week).p2;
                        if ( s_ < start && s_ > end ) {
                            /* This is past the time frame for the week */
                            continue;
                        }

                        for ( String level : week_to_levels.get(week) ) {
                            if ( levels_played.contains(level) ) { //&& (s_ >= start && s_ <= end) ) {
                                /* Want to add files based on the time in which the were done */
                                ArrayList<String> tmp = week_to_files.get(week);
                                tmp.add(f.getName());
                                week_to_files.replace(week, tmp);
                                break;
                            }
                        }
                    }

                    players.put(name,week_to_files);
                } else {
                    LinkedHashMap<String, ArrayList<String> > week_to_files = players.get(name);

                    /* Time in seconds */
                    LocalDateTime time = LocalDateTime.parse(date,formatter);
                    long s_ = ( time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000 );

                    for ( String week : week_to_levels.keySet() ) {

                        long start = week_to_time_interval.get(week).p1;
                        long end = week_to_time_interval.get(week).p2;
                        if ( s_ < start && s_ > end ) {
                            /* This is past the time frame for the week */
                            continue;
                        }

                        for ( String level : week_to_levels.get(week) ) {
                            if ( levels_played.contains(level) ) {
                                ArrayList<String> tmp = week_to_files.get(week);
                                tmp.add(f.getName());
                                week_to_files.replace(week,tmp);
                                break;
                            }
                        }
                    }
                    players.replace(name,week_to_files);
                }
            } catch ( IOException o ) {
                o.printStackTrace();
            }
        }

        /* Sort logs based on start time (while this is terribly inefficient, this works for now) */
        for ( String student : players.keySet() ) {
            for ( String week : players.get(student).keySet() ) {
                ArrayList< Pair<String,Long> > file_to_time = new ArrayList<>();
                for ( String name : players.get(student).get(week) ) {
                    try {
                        String fname = log_dir + "/" + name;
                        BufferedReader br = new BufferedReader(new FileReader(fname));
                        String line;
                        while ((line = br.readLine()) != null) {
                            String[] tmp = line.split("\t");
                            if (tmp.length != 6) {
                                continue;
                            }
                            if (tmp[1].equals("SessionUser")) {
                                LocalDateTime time = LocalDateTime.parse(tmp[0], formatter);
                                /* Time in seconds */
                                long s_ = (time.toInstant(ZoneOffset.ofHours(0)).toEpochMilli() / 1000);
                                file_to_time.add(new Pair<String, Long>(name, s_));
                                break;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Collections.sort(file_to_time, new Comparator<Pair<String, Long>>() {
                    @Override
                    public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
                        return o1.p2.compareTo(o2.p2);
                    }
                });
                ArrayList<String> tmp = players.get(student).get(week);
                tmp.clear();
                for ( Pair<String,Long> s : file_to_time ) {
                    tmp.add(s.p1);
                }
                players.get(student).replace(week,tmp);
            }
        }
        return players;
    }

    public HashMap< String, ArrayList<String> > getPlayers_v1() {

        HashMap< String, ArrayList<String> > players = new HashMap< String, ArrayList<String> >();
        File directory = new File(log_dir);
        File [] dir_list = directory.listFiles();
        for ( File f : dir_list ) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line;
                String name = "";
                while ( (line = br.readLine() ) != null ) {
                    String [] tmp = line.split("\t");
                    if ( tmp[1].equals("SessionUser") ) {
                        name = tmp[2];
                        break;
                    }
                }
                if ( !players.containsKey(name) ) {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(f.getName());
                    players.put(name,tmp);
                } else {
                    players.get(name).add(f.getName());
                }

            } catch ( IOException o ) {
                o.printStackTrace();
            }
        }
        return players;

    }

    public ArrayList<String> getTelemetryData( String filename ) {
        ArrayList<String> telemetry = new ArrayList<String>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                telemetry.add(line);
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return telemetry;
    }

    public Pair< ArrayList<String> , ArrayList<Integer> > getDataInIntervals(ArrayList<String> data, double t1, double t2, ArrayList<Integer> bit_vector) {
        ArrayList<String> ret = new ArrayList<String>();
        ArrayList<Integer> bit_vector_in_interval = new ArrayList<Integer>();

        for ( int i = 0; i < data.size(); i++ ) {
            String [] tmp = data.get(i).split("\t");
            double time_ = Double.parseDouble(tmp[3]);
            if ( ( t1 < time_ ) && ( t2 > time_ ) ) {
                ret.add(data.get(i));
                bit_vector_in_interval.add(bit_vector.get(i));
            }
        }
        return new Pair< ArrayList<String> , ArrayList<Integer> >(ret,bit_vector_in_interval);
    }

    public LinkedHashMap<String, ArrayList<String> > getLevelData( String data_path ) {
        /* NOTE: Should this be here? */

        LinkedHashMap<String, ArrayList<String> > ret = new  LinkedHashMap<String, ArrayList<String> >();

        File directory = new File(data_path);
        File [] dir_list = directory.listFiles();
        for ( File f : dir_list ) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("filename")) {
                        String filename = line.split("\t")[1];
                        if ( !ret.containsKey(filename) ) {
                            ArrayList<String> tmp = new ArrayList<String>();
                            tmp.add(f.getName());
                            ret.put(filename, tmp);
                        } else {
                            ret.get(filename).add(f.getName());
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public LinkedHashMap<String, ArrayList<String> > getTelemetryByLevel( ArrayList<String> telemetry ) {
        /* This is more accurate to train on as the player modeling engine will mostly run when a user is playing a level */
        /* This is actually what should be fed into the feature extraction. Anything done between levels should not be considered. */

        LinkedHashMap<String, ArrayList<String> > telemetry_by_level = new LinkedHashMap<String, ArrayList<String> >();

        /* 1) Find level "levelX" , 2) Find level "levelX+1", 3) Get data in between these */

        /* Current issues:
            1) What if a user plays a level multiple times? How do we deal with this? This might actually be a non-issue...
        */

        boolean start_parse = false;

        String current_level = "";
        for ( String tel : telemetry ) {
            String [] data = tel.split("\t");

            if ( data.length != 6 ) {
                /* Don't consider this kind of data */
                if ( DEBUG > 0 ) {
                    System.out.println("======================================================");
                    for ( String s : data ) {
                        System.out.println("Data: " + s);
                    }
                }
                continue;
            }

            String name_ = data[1];
            String data_ = data[2];

            if ( name_.equals("TriggerLoadLevel") ) {
                if (data_.length() != 0) {
                    if (data_.startsWith("l")) {
                        start_parse = true;
                        current_level = data_;
                    }
                }
            }

            if ( start_parse ) {
                if ( !telemetry_by_level.containsKey(current_level) ) {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(tel);
                    telemetry_by_level.put(current_level,tmp);
                } else {
                    ArrayList<String> tmp = telemetry_by_level.get(current_level);
                    tmp.add(tel);
                    telemetry_by_level.replace(current_level,tmp);
                }
            }
        }

        return telemetry_by_level;
    }

    public String getCalibration( ArrayList<String> telemetry ) {

        for ( String tel : telemetry ) {
            String[] data = tel.split("\t");
            String name_ = data[1];
            String data_ = data[2];

            if (name_.equals("Calibration")) {
                return data_;
            }
        }
        /* This should actually never arrive here */
        System.out.println("Should never arrive here");
        System.exit(-1);
        return null;
    }

    public String getUser(ArrayList<String> data) {
        /* NOTE: There are a few users from the study that were not correctly mentioned in the "SessionUser" event.. */

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
                        case "CAT":
                            return "MOUSE";
                        case "DOG":
                            return "DRAGON";
                    }
                }
                if ( tmp[2].equals("CHICKEN") ) {
                    return "MONKEY";
                }
                return tmp[2];
            }
        }
        System.err.println("Execution should never be here.");
        System.exit(-1);

        return "";
    }

}
