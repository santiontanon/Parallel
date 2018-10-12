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

    public TelemetryUtils() {}

    public HashMap< String, ArrayList<String> > getPlayers(String log_dir) {
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
                    if ( tmp.length != 6 ) {
                        continue;
                    }
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

    public ArrayList<String> readTelemetryFile(String filename) {
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

    public Pair< ArrayList<String> , ArrayList<Integer> > getTelemetryInInterval(ArrayList<String> data, double t1, double t2, ArrayList<Integer> bit_vector) {
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
        return new Pair< ArrayList<String> , ArrayList<Integer> >(ret, bit_vector_in_interval);
    }

    public ArrayList<String> getTelemetryInInterval(ArrayList<String> data, double t1, double t2) {
        ArrayList<String> ret = new ArrayList<String>();
        for ( int i = 0; i < data.size(); i++ ) {
            String [] tmp = data.get(i).split("\t");
            double time_ = Double.parseDouble(tmp[3]);
            if ( ( t1 < time_ ) && ( t2 > time_ ) ) {
                ret.add(data.get(i));
            }
        }
        return ret;
    }

    public LinkedHashMap<String, ArrayList<String> > getLevelData(String data_path) {
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

    public LinkedHashMap<String, ArrayList<String> > splitTelemetryByLevels(ArrayList<String> telemetry) {
        LinkedHashMap<String, ArrayList<String> > telemetry_by_level = new LinkedHashMap<String, ArrayList<String> >();
        /* TODO: What if a user plays a level multiple times?
        *   Telemetry from multiple plays are aggregated
        * */

        boolean start_parse = false;

        String current_level = "";
        for ( String tel : telemetry ) {
            String [] data = tel.split("\t");

            if ( data.length != 6 ) {
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
                    telemetry_by_level.put(current_level, tmp);
                } else {
                    ArrayList<String> tmp = telemetry_by_level.get(current_level);
                    tmp.add(tel);
                    telemetry_by_level.replace(current_level, tmp);
                }
            }
        }

        return telemetry_by_level;
    }
}
