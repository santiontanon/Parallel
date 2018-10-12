package pmutils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.cli.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class ServerInterface {

    public static final String SERVER_HOST = "10.250.48.248";  /* TODO: Can we get this from Parallel */
    public static final int PORT = 8787;
    public static final String CURRENT_PARAMS_FILENAME = "currentParameters.txt";
    public static final String LOCAL_PM_DATA = "localPMData.json";

    private CloseableHttpClient client;
    private String paramFilepath;

    public ServerInterface(String paramFilepath_) {
        client = HttpClients.createDefault();
        paramFilepath = paramFilepath_;
    }

    public void saveSkillVectorToServer(String level, String user) {
        String jsonString = getPlayerModelData(user);
        System.out.println("JSON String: " + jsonString);
        String skillVector = readSkillVector();
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject)jsonParser.parse(jsonString);
        jsonObject.addProperty("current", skillVector);
        if ( jsonObject.has(level) ) {
            jsonObject.getAsJsonArray(level).add(skillVector);
        } else {
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(skillVector);
            jsonObject.add(level, jsonArray);
        }
        String updatedJsonString = jsonObject.toString();
        System.out.println("Updating player modeling: "+ updatedJsonString);
        savePMDataLocally(updatedJsonString);
        postPlayerModelData(updatedJsonString, user);
    }

    public void savePMDataLocally(String jsonData) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(paramFilepath + "/" + LOCAL_PM_DATA));
            writer.println(jsonData);
            writer.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public void readSkillVectorFromServer(String user) {
        String jsonString = getPlayerModelData(user);
        System.out.println("JSON String: " + jsonString);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject)jsonParser.parse(jsonString);
        JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive("current");
        String skillVector = jsonPrimitive.getAsString();
        savePMDataLocally(jsonObject.toString());
        writeSkillVector(skillVector);
    }

    public void postPlayerModelData(String jsonString, String username) {
        try {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost(SERVER_HOST);
            uriBuilder.setPort(PORT);
            uriBuilder.setPath("/playermodel");
            uriBuilder.setParameter("user", username);
            URI uri = uriBuilder.build();
            HttpPost httpPost = new HttpPost(uri);
            StringEntity entity = new StringEntity(jsonString);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            CloseableHttpResponse response = client.execute(httpPost);
            assert(response.getStatusLine().getStatusCode() == 200);
            client.close();
        } catch ( URISyntaxException e ) {
            e.printStackTrace();
        } catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public String getPlayerModelData(String username) {
        String jsonString = "";
        try {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost(SERVER_HOST);
            uriBuilder.setPort(PORT);
            uriBuilder.setPath("/playermodel");
            uriBuilder.setParameter("user", username);
            URI uri = uriBuilder.build();
            HttpGet httpGet = new HttpGet(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            try {
                HttpEntity entity = response.getEntity();
                if ( entity != null ) {
                    jsonString = EntityUtils.toString(entity);
                }
            } finally {
                response.close();
            }
        } catch ( URISyntaxException e ) {
            e.printStackTrace();
        } catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return jsonString;
    }

    public void closeClient() {
        try {
            client.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public void writeSkillVector(String skillVector) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(paramFilepath + "/" + CURRENT_PARAMS_FILENAME));
            String [] skillVectorSplit = skillVector.split(":");
            for ( String s : skillVectorSplit ) {
                writer.println(s);
            }
            writer.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public String readSkillVector() {
        ArrayList<String> skillVector = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(paramFilepath + "/" + CURRENT_PARAMS_FILENAME));
            String line;
            while ((line = br.readLine()) != null) {
                String skillValuePair = line.trim();
                String [] skillValuePairSplit = skillValuePair.split(",");
                String skill = skillValuePairSplit[0];
                double skillValue = Double.parseDouble(skillValuePairSplit[1]);
                skillVector.add(String.format("%s,%f", skill, skillValue));
            }
            br.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        return String.join(":", skillVector);
    }

    public static void main(String [] args) {

        Options cliOptions = new Options();
        cliOptions.addOption("mode",true,"Mode of operation (read or write)");
        cliOptions.addOption("local",true,"Get data locally (only used if there is no network connection)");
        cliOptions.addOption("user",true,"Name of player");
        cliOptions.addOption("level",true,"Current level");
        cliOptions.addOption("path",true,"Path to currentParameters.txt");

        String mode = "";
        String user = "";
        String level = "";
        boolean local = false;
        String paramFilePath = "";
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(cliOptions, args);
            if ( line.hasOption("user") ) {
                user = line.getOptionValue("user");
            }
            if ( line.hasOption("mode") ) {
                mode = line.getOptionValue("mode");
            }
            if ( line.hasOption("level") ) {
                level = line.getOptionValue("level");
            }
            if ( line.hasOption("path") ) {
                paramFilePath = line.getOptionValue("path");
            }
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        System.out.println("------------------- Arguments -------------------");
        System.out.println("Username of Player: " + user);
        System.out.println("Current level: " + level);
        System.out.println("Mode: " + mode);
        System.out.println("Path to parameter file: " + paramFilePath);
        System.out.println("-------------------------------------------------");

        ServerInterface serverInterface = new ServerInterface(paramFilePath);
        if ( mode.equals("read") ) {
            serverInterface.readSkillVectorFromServer(user);
        } else {
            serverInterface.saveSkillVectorToServer(level, user);
        }
        serverInterface.closeClient();
    }
}
