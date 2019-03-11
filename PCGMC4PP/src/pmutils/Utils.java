package pmutils;

import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class Utils {

    private static final Logger logger = LogManager.getLogger(Utils.class);

    public static HashMap<String,Object> parseComponentInformation(String componentInfo) {
        Gson gson = new Gson();
        HashMap<String,Object> info = gson.fromJson(componentInfo,HashMap.class);
        return info;
    }

    public static LinkedHashMap<String, ArrayList<String> > mapMEFileKeyToFile(String levelDataPath) {
        LinkedHashMap<String, ArrayList<String> > ret = new  LinkedHashMap<String, ArrayList<String> >();

        File directory = new File(levelDataPath);
        File [] dirList = directory.listFiles();
        for ( File f : dirList ) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String line;
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
                logger.fatal("Unable to read ME file.");
                logger.catching(Level.FATAL, e);
            } catch (Exception e) {
                logger.catching(Level.FATAL, e);
            }
        }
        return ret;
    }
}
