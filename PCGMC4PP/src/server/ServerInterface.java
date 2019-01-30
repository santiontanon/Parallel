package server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.commons.cli.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ServerInterface {

    public static final String LOCAL_PM_DATA = "localPMData.json";
    public static final int TIMEOUT_MILLISECONDS = 5000;
    private CloseableHttpClient client;
    private String paramFilepath;
    private String serverHost;
    private int port;

    private String startTimestamp;
    private String endTimestamp;
    private String meFileName;
    private static final Logger logger = LogManager.getLogger(ServerInterface.class);


    public ServerInterface(String paramFilepath_, String serverHost_, int port_,
                           String startTime_, String endTime_, String meFileName_) {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(TIMEOUT_MILLISECONDS)
                .setConnectTimeout(TIMEOUT_MILLISECONDS)
                .setConnectionRequestTimeout(TIMEOUT_MILLISECONDS)
                .build();
        client = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
        paramFilepath = paramFilepath_;
        serverHost = serverHost_;
        port = port_;

        startTimestamp = startTime_;
        endTimestamp = endTime_;
        meFileName = meFileName_;
    }

    public ServerInterface(String paramFilepath_, String serverHost_, int port_) {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .setSocketTimeout(TIMEOUT_MILLISECONDS)
                .setConnectTimeout(TIMEOUT_MILLISECONDS)
                .setConnectionRequestTimeout(TIMEOUT_MILLISECONDS)
                .build();
        client = HttpClients.custom().setDefaultRequestConfig(defaultRequestConfig).build();
        paramFilepath = paramFilepath_;
        serverHost = serverHost_;
        port = port_;

        startTimestamp = "";
        endTimestamp = "";
        meFileName = "";
    }

    public String postPlayerModelData(String jsonString, String username) {

        URI uri = null;
        logger.info("Sending HTTP POST request for user: " + username);

        try {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost(serverHost);
            uriBuilder.setPort(port);
            uriBuilder.setPath("/playermodel");
            uriBuilder.setParameter("user", username);
            uri = uriBuilder.build();
            HttpPost httpPost = new HttpPost(uri);
            StringEntity entity = new StringEntity(jsonString);
            httpPost.setEntity(entity);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            logger.debug(String.format("HTTP POST to server %s at port %d", serverHost, port));
            CloseableHttpResponse response = client.execute(httpPost);
            assert(response.getStatusLine().getStatusCode() == 200);
            client.close();
        } catch ( URISyntaxException e ) {
            if ( uri != null ) {
                logger.error(String.format("%s:%s", "URI Syntax Error in HTTP POST request", uri.toString()));
            } else {
                logger.error(String.format("%s", "URI Syntax Error in HTTP POST request"));
            }
            logger.catching(e);
            return "uri_syntax_error";
        } catch ( UnsupportedEncodingException e ) {
            logger.error("Unsupported Encoding Error in HTTP GET request");
            logger.catching(e);
            return "encoding_error";
        } catch ( ConnectTimeoutException e ) {
            logger.warn("Connection has timed out sending HTTP POST request!");
            return "timeout";
        } catch ( IOException e ) {
            logger.error("IO error sending HTTP POST request");
            logger.catching(e);
            return "io_error";
        } catch ( Exception e ) {
            logger.error("General error sending HTTP POST request");
            logger.catching(e);
            return "general_error";
        }
        return "success";
    }

    public String getPlayerModelData(String username) {
        String jsonString;
        URI uri = null;
        logger.info("Sending HTTP GET request for user: " + username);
        try {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost(serverHost);
            uriBuilder.setPort(port);
            uriBuilder.setPath("/playermodel");
            uriBuilder.setParameter("user", username);
            uri = uriBuilder.build();
            HttpGet httpGet = new HttpGet(uri);
            logger.debug(String.format("HTTP GET to server %s at port %d", serverHost, port));
            CloseableHttpResponse response = client.execute(httpGet);
            try {
                BufferedReader rd = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent()));

                StringBuffer result = new StringBuffer();
                String line;
                while ((line = rd.readLine()) != null) {
                    result.append(line);
                }
                jsonString = result.toString();
            } finally {
                response.close();
            }
        } catch ( URISyntaxException e ) {
            if ( uri != null ) {
                logger.error(String.format("%s:%s", "URI Syntax Error in HTTP GET request", uri.toString()));
            } else {
                logger.error(String.format("%s", "URI Syntax Error in HTTP GET request"));
            }
            logger.catching(e);
            return "uri_syntax_error";
        } catch ( UnsupportedEncodingException e ) {
            logger.error("Unsupported Encoding Error in HTTP GET request");
            logger.catching(e);
            return "unsupported_encoding_error";
        } catch ( ConnectTimeoutException e ) {
            logger.warn("Connection has timed out sending HTTP GET request!");
            return "timeout";
        } catch ( IOException e ) {
            logger.error("IO error sending HTTP GET request");
            logger.catching(e);
            return "io_error";
        } catch ( Exception e ) {
            logger.error("General error sending HTTP GET request");
            logger.catching(e);
            return "general_error";
        }
        return jsonString;
    }

    public void closeClient() {
        try {
            if (client != null) {
                client.close();
            }
        } catch ( IOException e ) {
            logger.error("Network connection has already been closed?");
            return;
        }
    }

    public void writeSkillVector(String skillVector) {
        try {
            logger.info("Writing skill vector: " + skillVector);
            logger.info("Writing skill vector to: " + paramFilepath);
            PrintWriter writer = new PrintWriter(new FileWriter(paramFilepath));

            String [] skillVectorSplit = skillVector.split(":");
            for ( String s : skillVectorSplit ) {
                writer.println(s);
            }
            writer.close();
        } catch ( IOException e ) {
            logger.error("Unable to read in skill vector " + paramFilepath);
            logger.catching(e);
        }
    }

    public String readSkillVector() {
        ArrayList<String> skillVector = new ArrayList<>();
        try {
            logger.info("Reading skill vector from: " + paramFilepath);
            BufferedReader br = new BufferedReader(new FileReader(paramFilepath));
            String line;
            while ((line = br.readLine()) != null) {
                skillVector.add(line.trim());
            }
            br.close();

        } catch ( IOException e ) {
            logger.error("Unable to read in skill vector " + paramFilepath);
            logger.catching(e);
        }
        logger.info("Read skill vector: " + String.join(":", skillVector));
        return String.join(":", skillVector);
    }

    private boolean localPMDataExists() {
        String filepath = paramFilepath.substring(0, paramFilepath.lastIndexOf(File.separator));
        File f = new File(filepath + File.separator + LOCAL_PM_DATA);
        return f.exists();
    }

    public String readLocalPMData() {
        try {
            String filepath = paramFilepath.substring(0, paramFilepath.lastIndexOf(File.separator));
            logger.info("Reading Player Modeling data locally from: " + filepath + File.separator + LOCAL_PM_DATA);
            BufferedReader br = new BufferedReader(new FileReader(filepath + File.separator + LOCAL_PM_DATA));
            String line;
            ArrayList<String> lines = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
            br.close();
            return String.join("\n", lines);
        } catch ( IOException e ) {
            logger.error("Unable to read player modeling data locally.");
            logger.catching(e);
            return "";
        }
    }

    public void writeLocalPMData(String jsonData) {
        try {
            String filepath = paramFilepath.substring(0, paramFilepath.lastIndexOf(File.separator));
            logger.info("Saving Player Modeling data locally at: " + filepath + File.separator + LOCAL_PM_DATA);
            PrintWriter writer = new PrintWriter(new FileWriter(filepath + File.separator + LOCAL_PM_DATA));
            writer.println(jsonData);
            writer.close();
        } catch ( IOException e ) {
            logger.error("Unable to save player modeling data locally.");
            logger.catching(e);
        }
    }

    public void synchronizePMData(String user) {
        logger.info("Synchronizing local and remote player modeling data!");
        logger.info("Retrieving remote player modeling data");

        String remotePMData = getPlayerModelData(user);
        if ( remotePMData.equals("\"\"") ) {
            logger.info("New user! Creating empty local files!");
            writeLocalPMData("");
            writeSkillVector("");
            logger.info("Synchronization complete!!");
            return;
        }

        boolean serverError = false;
        if ( remotePMData.contains("_error") || remotePMData.equals("timeout") ) {
            logger.warn(String.format("The following error has occured: %s. Relying on local player modeling data for now!", remotePMData));
            serverError = true;
        }

        logger.info("Retrieving local player modeling data");

        String localPMData = "";
        if ( localPMDataExists() ) {
            localPMData = readLocalPMData();
        }

        if ( serverError ) {
            logger.info("Handling server error condition....");
            if ( localPMData.isEmpty() ) {
                writeLocalPMData("");
                writeSkillVector("");
            } else {
                JsonParser jsonParser = new JsonParser();
                JsonObject jsonObject = (JsonObject)jsonParser.parse(localPMData);
                JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive("current");
                String skillVector = jsonPrimitive.getAsString();
                writeSkillVector(skillVector);
            }
            logger.info("Synchronization complete!!");
            return;
        }

        assert !remotePMData.isEmpty();

        if ( localPMData.isEmpty() ) {
            logger.info("Handling empty local player modeling data...");
            JsonParser jsonParser = new JsonParser();
            logger.info("Parsing data: " + remotePMData);
            JsonObject jsonObject = (JsonObject)jsonParser.parse(remotePMData);
            JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive("current");
            String skillVector = jsonPrimitive.getAsString();
            writeLocalPMData(jsonObject.toString());
            writeSkillVector(skillVector);
            logger.info("Synchronization complete!!");
            return;
        }

        logger.info("Both local and remote player modeling data exists....synchronizing");

        JsonParser jsonParser = new JsonParser();
        JsonObject localPMJsonObject = (JsonObject) jsonParser.parse(localPMData);
        JsonPrimitive localPMJsonPrimitive = localPMJsonObject.getAsJsonPrimitive("user");
        String localUsername = localPMJsonPrimitive.getAsString();

        System.out.println(remotePMData);
        JsonObject remotePMJsonObject = (JsonObject) jsonParser.parse(remotePMData);
        JsonPrimitive remotePMJsonPrimitive = remotePMJsonObject.getAsJsonPrimitive("user");
        String remoteUsername = remotePMJsonPrimitive.getAsString();
        String convertedRemotePMData = remotePMJsonObject.toString();

        if (!localUsername.equals(remoteUsername)) {
            logger.warn(String.format("Users differ (local: %s, remote: %s)....replacing local data with remote."), localUsername, remoteUsername);

            JsonPrimitive jsonPrimitive = remotePMJsonObject.getAsJsonPrimitive("current");
            String skillVector = jsonPrimitive.getAsString();
            writeLocalPMData(convertedRemotePMData);
            writeSkillVector(skillVector);
            logger.info("Synchronization complete!!");

            return;
        }

        JsonPrimitive lastUpdatedPrimitive = localPMJsonObject.getAsJsonPrimitive("last-updated");
        LocalDateTime localLastUpdatedTime = LocalDateTime.parse(lastUpdatedPrimitive.getAsString(), DateTimeFormatter.ISO_DATE_TIME);

        lastUpdatedPrimitive  = remotePMJsonObject.getAsJsonPrimitive("last-updated");
        LocalDateTime remoteLastUpdatedTime = LocalDateTime.parse(lastUpdatedPrimitive.getAsString(), DateTimeFormatter.ISO_DATE_TIME);
        logger.info(String.format("Local Last Updated Time: %s, Remote Last Updated Time: %s", localLastUpdatedTime.toString(), remoteLastUpdatedTime.toString()));

        if ( remoteLastUpdatedTime.isAfter(localLastUpdatedTime) ) {
            logger.info("Remote player modeling is more recent. Update local player modeling data");
            writeLocalPMData(convertedRemotePMData);
            JsonPrimitive jsonPrimitive = remotePMJsonObject.getAsJsonPrimitive("current");
            String skillVector = jsonPrimitive.getAsString();
            writeSkillVector(skillVector);
        } else {
            if ( localLastUpdatedTime.isAfter(remoteLastUpdatedTime) ) {
                logger.info("Local player modeling is more recent. Update remote player modeling data");
                postPlayerModelData(localPMData, user);
            }
        }

        logger.info("Synchronization complete!!");
        return;
    }

    public void saveSkillVector(String level, String user) {

        assert localPMDataExists();
        String localPMData = readLocalPMData();

        logger.info(String.format("Local JSON data for user %s: %s", user, localPMData));

        String skillVector = readSkillVector();
        JsonObject jsonObject;
        if ( localPMData.isEmpty() ) {
            jsonObject = new JsonObject();
            jsonObject.addProperty("user", user);
        } else {
            JsonParser jsonParser = new JsonParser();
            jsonObject = (JsonObject)jsonParser.parse(localPMData);
        }
        jsonObject.addProperty("current", skillVector);
        LocalDateTime currentTimeStamp = LocalDateTime.now();
        jsonObject.addProperty("last-updated", currentTimeStamp.toString());
        if ( jsonObject.has(level) ) {
            JsonObject skillVectorStructure = new JsonObject();
            skillVectorStructure.addProperty("start", startTimestamp);
            skillVectorStructure.addProperty("end", endTimestamp);
            skillVectorStructure.addProperty("meout", meFileName);
            skillVectorStructure.addProperty("sv", skillVector);
            jsonObject.getAsJsonArray(level).add(skillVectorStructure);
        } else {
            JsonArray jsonArray = new JsonArray();
            JsonObject skillVectorStructure = new JsonObject();
            skillVectorStructure.addProperty("start", startTimestamp);
            skillVectorStructure.addProperty("end", endTimestamp);
            skillVectorStructure.addProperty("meout", meFileName);
            skillVectorStructure.addProperty("sv", skillVector);
            jsonArray.add(skillVectorStructure);
            jsonObject.add(level, jsonArray);
        }
        String updatedJsonString = jsonObject.toString();
        logger.info("Updating player modeling with new data: " + updatedJsonString);

        writeLocalPMData(updatedJsonString);
        postPlayerModelData(updatedJsonString, user);
    }

    public static void main(String [] args) {

        Options cliOptions = new Options();
        cliOptions.addOption("mode",true,"Mode of operation (read or write)");
        cliOptions.addOption("user",true,"Name of player");
        cliOptions.addOption("level",true,"Current level");
        cliOptions.addOption("path",true,"Path to currentparameters.txt");
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
            logger.fatal("Parsing failed.  Reason: " + exp.getMessage());
            System.exit(1);
        }

        logger.info("------------------- Arguments -------------------");
        logger.info("Username of Player: " + user);
        logger.info("Current level: " + level);
        logger.info("Mode: " + mode);
        logger.info("Path to parameter file: " + paramFilePath);
        logger.info("Hostname: " + hostname);
        logger.info("Port: " + port);
        logger.info("Debugging: " + debug);
        logger.info("-------------------------------------------------");

        ServerInterface serverInterface = new ServerInterface(paramFilePath, hostname, port);
        if ( mode.equals("read") ) {
            serverInterface.synchronizePMData(user);
        } else {
            serverInterface.saveSkillVector(level, user);
        }
        serverInterface.closeClient();
    }
}
