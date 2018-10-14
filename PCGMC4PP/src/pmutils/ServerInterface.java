package pmutils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.cli.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

public class ServerInterface {

    public static final String CURRENT_PARAMS_FILENAME = "currentParameters.txt";
    public static final String LOCAL_PM_DATA = "localPMData.json";

    private CloseableHttpClient client;
    private String paramFilepath;
    private String serverHost;
    private int port;
    private boolean debug;

    public ServerInterface(String paramFilepath_, String serverHost_, int port_, boolean debug_) {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(5000)
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .build();
        client = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
        paramFilepath = paramFilepath_;
        serverHost = serverHost_;
        port = port_;
        debug = debug_;
    }

    public void saveSkillVectorToServer(String level, String user) {
        String jsonString = getPlayerModelData(user);
        if ( debug ) {
            System.out.println(String.format("JSON from server for user %s %s", user, jsonString));
        }
        String skillVector = readSkillVector();
        JsonObject jsonObject;
        if ( jsonString.equals("\"\"") ) {
            jsonObject = new JsonObject();
            jsonObject.addProperty("user", user);
        } else {
            JsonParser jsonParser = new JsonParser();
            jsonObject = (JsonObject)jsonParser.parse(jsonString);
        }
        jsonObject.addProperty("current", skillVector);
        if ( jsonObject.has(level) ) {
            jsonObject.getAsJsonArray(level).add(skillVector);
        } else {
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(skillVector);
            jsonObject.add(level, jsonArray);
        }
        String updatedJsonString = jsonObject.toString();
        if ( debug ) {
            System.out.println("Updating player modeling with new data: " + updatedJsonString);
        }
        savePMDataLocally(updatedJsonString);
        postPlayerModelData(updatedJsonString, user);
    }

    public void savePMDataLocally(String jsonData) {
        try {
            if (debug) {
                System.out.println("Saving Player Modeling data locally at: " + paramFilepath + "/" + LOCAL_PM_DATA);
            }
            PrintWriter writer = new PrintWriter(new FileWriter(paramFilepath + "/" + LOCAL_PM_DATA));
            writer.println(jsonData);
            writer.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public void readSkillVectorFromServer(String user) {
        String jsonString = getPlayerModelData(user);
        if ( debug ) {
            System.out.println(String.format("JSON from server for user %s %s", user, jsonString));
        }
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
            uriBuilder.setHost(serverHost);
            uriBuilder.setPort(port);
            uriBuilder.setPath("/playermodel");
            uriBuilder.setParameter("user", username);
            URI uri = uriBuilder.build();
            HttpPost httpPost = new HttpPost(uri);
            StringEntity entity = new StringEntity(jsonString);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            if ( debug ) {
                System.out.println(String.format("HTTP POST to server %s at port %d", serverHost, port));
            }
            CloseableHttpResponse response = client.execute(httpPost);
            assert(response.getStatusLine().getStatusCode() == 200);
            client.close();
        } catch ( URISyntaxException e ) {
            e.printStackTrace();
        } catch ( UnsupportedEncodingException e ) {
            e.printStackTrace();
        } catch ( ConnectTimeoutException e ) {

        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public String getPlayerModelData(String username) {
        String jsonString = "";
        try {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost(serverHost);
            uriBuilder.setPort(port);
            uriBuilder.setPath("/playermodel");
            uriBuilder.setParameter("user", username);
            URI uri = uriBuilder.build();
            HttpGet httpGet = new HttpGet(uri);
            if ( debug ) {
                System.out.println(String.format("HTTP GET to server %s at port %d", serverHost, port));
            }
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
            if (debug) {
                System.out.println("Writing skill vector to: " + paramFilepath + "/" + CURRENT_PARAMS_FILENAME);
                System.out.println("Writing data: " + skillVector);
            }
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
            if (debug) {
                System.out.println("Reading skill vector from: " + paramFilepath + "/" + CURRENT_PARAMS_FILENAME);
            }
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
        if (debug) {
            System.out.println("Read skill vector: " +String.join(":", skillVector));
        }
        return String.join(":", skillVector);
    }

    public static void main(String [] args) {

        Options cliOptions = new Options();
        cliOptions.addOption("mode",true,"Mode of operation (read or write)");
        cliOptions.addOption("user",true,"Name of player");
        cliOptions.addOption("level",true,"Current level");
        cliOptions.addOption("path",true,"Path to currentParameters.txt");
        cliOptions.addOption("hostname",true,"Hostname of server");
        cliOptions.addOption("port",true,"Port number of server");
        cliOptions.addOption("debug",false,"Port number of server");

        String mode = "";
        String user = "";
        String level = "";
        String paramFilePath = "";
        String hostname = "129.25.141.236";
        int port = 8787;
        boolean debug = false;

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
            if ( line.hasOption("hostname") ) {
                hostname = line.getOptionValue("hostname");
            }
            if ( line.hasOption("port") ) {
                port = Integer.parseInt(line.getOptionValue("port"));
            }
            if ( line.hasOption("debug") ) {
                debug = true;
            }
        } catch( ParseException exp ) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }

        if ( debug ) {
            System.out.println("------------------- Arguments -------------------");
            System.out.println("Username of Player: " + user);
            System.out.println("Current level: " + level);
            System.out.println("Mode: " + mode);
            System.out.println("Path to parameter file: " + paramFilePath);
            System.out.println("Hostname: " + hostname);
            System.out.println("Port: " + port);
            System.out.println("Debugging: " + debug);
            System.out.println("-------------------------------------------------");
        }

        ServerInterface serverInterface = new ServerInterface(paramFilePath, hostname, port, debug);
        if ( mode.equals("read") ) {
            serverInterface.readSkillVectorFromServer(user);
        } else {
            serverInterface.saveSkillVectorToServer(level, user);
        }
        serverInterface.closeClient();
    }
}
