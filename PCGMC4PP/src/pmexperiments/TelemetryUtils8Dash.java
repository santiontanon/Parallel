package pmexperiments;

import playermodeling.Pair;
import playermodeling.PlayerModeler;
import playermodeling.TelemetryUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TelemetryUtils8Dash extends TelemetryUtils {

    private LinkedHashMap<String, Pair<String,String>> week_to_time_interval_str;
    private LinkedHashMap<String, ArrayList< String > > week_to_levels;

    public TelemetryUtils8Dash() {

        /* NOTE: For now, this is hard-coded ( 8 dash data is structured differently ). I will remove this once we get more data */
        week_to_levels = new LinkedHashMap<String, ArrayList<String> >();
        week_to_levels.put("Week 4", new ArrayList<String>());
        week_to_levels.put("Week 3", new ArrayList<String>());
        week_to_levels.put("Week 2", new ArrayList<String>());
        week_to_levels.put("Week 1", new ArrayList<String>());

        try {
            BufferedReader br = new BufferedReader(new FileReader(PlayerModeler.PLAYER_MODELING_DATA_DIR + "week_to_levels.txt"));
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

    public HashMap< String, LinkedHashMap< String, ArrayList<String> > > getPlayers8Dash(String log_dir) {
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

}
