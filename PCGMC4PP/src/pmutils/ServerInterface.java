package pmutils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.cli.*;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
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

    public static final String SERVER_HOST = "129.25.151.236";
    public static final String CURRENT_PARAMS_FILENAME = "currentParams.txt";

    private CloseableHttpClient client;

    public ServerInterface() {
        client = HttpClients.createDefault();
    }

    public void saveSkillVector(String level, String user, String paramFilepath) {
        String jsonString = getPlayerModelData(user);
        String skillVector = readSkillVector(paramFilepath);
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
        String updatedJsonString = jsonObject.getAsString();
        postPlayerModelData(updatedJsonString);
    }

    public void readSkillVectorFromServer(String user, String paramFilepath) {
        String jsonString = getPlayerModelData(user);
        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject)jsonParser.parse(jsonString);
        JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive("current");
        String skillVector = jsonPrimitive.getAsString();
        writeSkillVector(skillVector, paramFilepath);
    }

    public void postPlayerModelData(String jsonString) {
        try {
            URI uri = new URIBuilder("http")
                    .setHost(SERVER_HOST)
                    .setPath("/playermodel")
                    .build();
            HttpPost httpPost = new HttpPost(uri);
            StringEntity entity = new StringEntity(jsonString);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            System.out.println(httpPost.getURI());

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
            URI uri = new URIBuilder("http")
                    .setHost(SERVER_HOST)
                    .setPath("/playermodel")
                    .setParameter("uname", username)
                    .build();
            HttpPost httpGet = new HttpPost(uri);
            CloseableHttpResponse response = client.execute(httpGet);
            try {
                HttpEntity entity = response.getEntity();
                if ( entity != null ) {
                    long len = entity.getContentLength();
                    if ( len != -1 ) {
                        jsonString = EntityUtils.toString(entity);
                    }
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

    public void writeSkillVector(String skillVector, String paramFliePath) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(paramFliePath + "/" + CURRENT_PARAMS_FILENAME));
            String [] skillVectorSplit = skillVector.split(":");
            for ( String s : skillVectorSplit ) {
                writer.println(s);
            }
            writer.close();
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    public String readSkillVector(String paramFliePath) {
        ArrayList<String> skillVector = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(paramFliePath + "/" + CURRENT_PARAMS_FILENAME));
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
        cliOptions.addOption("user",true,"Name of player");
        cliOptions.addOption("level",true,"Current level");

        cliOptions.addOption("path",true,"Path to currentParams.txt");

        String mode = "";
        String user = "";
        String level = "";
        String paramFilePath = "";
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse( cliOptions, args );
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

        ServerInterface serverInterface = new ServerInterface();
        if ( mode.equals("read") ) {
            serverInterface.readSkillVectorFromServer(user, paramFilePath);
        } else {
            serverInterface.saveSkillVector(level, user, paramFilePath);
        }
        serverInterface.closeClient();
    }
}
