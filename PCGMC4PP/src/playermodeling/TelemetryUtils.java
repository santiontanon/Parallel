package playermodeling;

/**
 * Created by pavankantharaju on 2/26/18.
 */

import com.sun.org.apache.bcel.internal.generic.ARRAYLENGTH;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;


public class TelemetryUtils {

    private String log_dir = "";

    public TelemetryUtils(String path) {
        log_dir = path + "/log/";

    }

    public HashMap< String, ArrayList<String> > getPlayers() {
        HashMap< String, ArrayList<String> > players = new HashMap< String, ArrayList<String> >();
        File directory = new File(log_dir);
        File [] dir_list = directory.listFiles();
        for ( File f : dir_list ) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line = "";
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
            String line = "";
            while ((line = br.readLine()) != null) {
                telemetry.add(line);
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return telemetry;
    }

    public ArrayList< String> getDataInIntervals(ArrayList<String> data, double t1, double t2) {
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

    public HashMap<String, ArrayList<String> > getLevelData( String data_path ) {
        HashMap<String, ArrayList<String> > ret = new  HashMap<String, ArrayList<String> >();

        File directory = new File(data_path);
        File [] dir_list = directory.listFiles();
        for ( File f : dir_list ) {
            try {
                //System.out.println("Filename: " + f.getAbsolutePath());
                BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("filename")) {
                        String filename = line.split("\t")[1];
                        //System.out.println("File: " + line + "," + filename);
                        if ( !ret.containsKey(filename) ) {
                            ArrayList<String> tmp = new ArrayList<String>();
                            //System.out.println("Name: " + f.getName());
                            tmp.add(f.getName());
                            ret.put(filename, tmp);
                        } else {
                            ret.get(filename).add(f.getName());
                        }
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

}
