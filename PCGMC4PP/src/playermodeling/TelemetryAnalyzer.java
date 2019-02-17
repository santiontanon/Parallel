package playermodeling;

/**
 * Created by pavankantharaju on 2/26/18.
 */

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pmutils.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TelemetryAnalyzer {

    private static final Logger logger = LogManager.getLogger(TelemetryAnalyzer.class);

    public TelemetryAnalyzer() {}

    public HashMap< String, ArrayList<String> > getPlayers(String logDirectory) {
        logger.info("Getting players from log data!");
        HashMap< String, ArrayList<String> > players = new HashMap< String, ArrayList<String> >();
        File directory = new File(logDirectory);
        File [] filesInLogDirectory = directory.listFiles();
        for ( File f : filesInLogDirectory ) {
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
                logger.fatal("Unable to read logging data to get list of players");
                logger.catching(Level.FATAL, o);
                logger.catching(o);
            } catch ( Exception e ) {
                logger.fatal("Error in getting list of players");
                logger.catching(Level.FATAL, e);
            }
        }
        logger.debug("-------------Players-------------");
        for (String p : players.keySet()) {
            logger.debug("Player: " + p);
        }
        logger.debug("---------------------------------");
        return players;
    }

    public ArrayList<String> readTelemetryFile(String filename) {
        ArrayList<String> telemetry = new ArrayList<>();
        logger.info("Reading telemetry file: " + filename);
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = br.readLine()) != null) {
                telemetry.add(line);
            }
            return telemetry;
        } catch ( IOException e ) {
            logger.fatal("Unable to read in logging data from: " + filename);
            logger.catching(Level.FATAL, e);
        }
        return null;
    }

    @Deprecated
    public Pair< ArrayList<String> , ArrayList<Integer> > getTelemetryInInterval(ArrayList<String> data, double t1, double t2, ArrayList<Integer> bitVector) {
        ArrayList<String> ret = new ArrayList<String>();
        ArrayList<Integer> bitVectorInInterval = new ArrayList<Integer>();

        for ( int i = 0; i < data.size(); i++ ) {
            String [] tmp = data.get(i).split("\t");
            double time_ = Double.parseDouble(tmp[3]);
            if ( ( t1 < time_ ) && ( t2 > time_ ) ) {
                ret.add(data.get(i));
                bitVectorInInterval.add(bitVector.get(i));
            }
        }
        return new Pair<>(ret, bitVectorInInterval);
    }

    public ArrayList<String> getTelemetryInInterval(ArrayList<String> data, double t1, double t2) {
        logger.info(String.format("Getting telemetry data in interval (%f,%f)", t1, t2));
        ArrayList<String> ret = new ArrayList<String>();
        for ( int i = 0; i < data.size(); i++ ) {
            String [] tmp = data.get(i).split("\t");
            if ( tmp.length != 6 ) {
                continue;
            }
            double time_ = Double.parseDouble(tmp[3]);
            if ( ( t1 < time_ ) && ( t2 > time_ ) ) {
                ret.add(data.get(i));
            }
        }
        return ret;
    }

    public LinkedHashMap< String , ArrayList<String> > splitTelemetryByRun(ArrayList<String> telemetry) {
        LinkedHashMap< String, ArrayList<String> > telemetryByRun = new LinkedHashMap<>();
        ArrayList<String> runTelemetry = new ArrayList<>();

        for ( String tel : telemetry ) {
            String[] data = tel.split("\t");

            if (data.length != 6) {
                logger.warn("Incorrectly formatted telemetry log line: " + tel);
                continue;
            }

            String name_ = data[1];
            String data_ = data[2];
            runTelemetry.add(tel);

            if (name_.equals("TriggerLoadLevel")) {
                if (data_.length() != 0) {
                    if (!data_.startsWith("l")) {
                        /* ME Execution */
                        if (telemetryByRun.containsKey(data_)) {
                            logger.fatal("ME Execution used in another run. Exiting!");
                            System.exit(1);
                        } else {
                            telemetryByRun.put(data_, (ArrayList<String>) runTelemetry.clone());
                            runTelemetry.clear();
                        }
                    }
                }
            }
        }
        return telemetryByRun;
    }

    public LinkedHashMap<String, ArrayList<String> > splitTelemetryByLevels(ArrayList<String> telemetry) {
        LinkedHashMap<String, ArrayList<String> > telemetryByLevel = new LinkedHashMap<String, ArrayList<String> >();
        /*
        *  If user plays multiple levels, telemetry from multiple plays are aggregated
        * */
        logger.info("Splitting telemetry by level!");
        boolean startParse = false;

        String currentLevel = "";
        for ( String tel : telemetry ) {
            String [] data = tel.split("\t");

            if ( data.length != 6 ) {
                logger.warn("Incorrectly formatted telemetry log line: " + tel);
                continue;
            }

            String name_ = data[1];
            String data_ = data[2];

            if ( name_.equals("TriggerLoadLevel") ) {
                if (data_.length() != 0) {
                    if (data_.startsWith("l")) {
                        startParse = true;
                        currentLevel = data_;
                    }
                }
            }

            if ( startParse ) {
                if ( !telemetryByLevel.containsKey(currentLevel) ) {
                    ArrayList<String> tmp = new ArrayList<String>();
                    tmp.add(tel);
                    telemetryByLevel.put(currentLevel, tmp);
                } else {
                    ArrayList<String> tmp = telemetryByLevel.get(currentLevel);
                    tmp.add(tel);
                    telemetryByLevel.replace(currentLevel, tmp);
                }
            }
        }

        return telemetryByLevel;
    }

    public String getUsername(ArrayList<String> data) {
        for ( String d : data ) {
            String [] tmp = d.split("\t");
            if ( tmp[1].equals("SessionUser") ) {
                return tmp[2];
            }
        }
        logger.fatal("Log data does not have a \"SessionUser\"");
        System.exit(1);
        return "username_not_found";
    }
}
