# Player Modeling for Parallel

This README provides an introduction to running our player modeling system. There are two JAR executables: `PlayerModel.jar` and `ServerInterface.jar`. Both of these are located in the `dist/` directory.

## Installation/Setup Instructions

Prior to executing any of these JAR files, make sure the Parallel Server is running! Both of these make calls to the server.

## Player Modeling System

The Player Modeling System takes telemetry information from a player and the execution from the Model Engine, and attempts to predict the level of understanding of a set of skills. s

**NOTE:** Prior to running the `PlayerModel.jar` file, you need to place the directory `pmfiles/` in the same directory as the JAR file. This directory contains information relevant for analyzing player behavior.  

The `pmfiles/` directory **must** contain the following files:

* `critical_sections/` - critical section information for non-PCG levels
* `level_skills` - list of skills per level for non-PCG level_skills
* `skills.txt` - list of all skills
* `classifier-model.model` - Trained model of machine learning classifier

### Command Line Arguments

1. **REQURIED** `mepath` - Path to Model Engine Execution File
2. **REQURIED** `telemetrypath` - Path to Telemetry Log
3. **REQUIRED** `parameterpath` - Path to Parameter file `currentParameters.txt`
4. **REQUIRED** `level` - Level Name
5. **REQUIRED** `user` - User Name
6. **REQUIRED** `hostname` - Host Name of Server
7. **REQUIRED** `port` - Port of Server
8. **OPTIONAL** `debug` - Turn on debugging information

 NOTE: Debug should not have an argument.

### Running the system

To run the Player Modeling System, execute the following command:
```
java -jar PlayerModel.jar  -mepath testing/me_out_.txt -telemetrypath testing/Session-model.log -parameterpath testing/ -user username -level levelname -hostname hostname -port 8787 -debug
```

## Server Interface

The Server Interface allows Parallel and the Player Modeling System to communicate with the Parallel Server. The Server Interface uses HTTP requests to send and receive player modeling data for some player.

The skill vector of a player will be saved on the local machine at the path provided as a commnad line argument. Note that this path **must** contain `currentParameters.txt`.

### Command Line Arguments

1. **REQURIED** `mode` (read and write)
 * Read: Read player modeling code from server
 * Write: Write player modeling to server  
2. **REQURIED** `user` - User Name
3. **REQUIRED if writing** `level` - Level Name
4. **REQUIRED** `path` - Path to Parameter file `currentParameters.txt`
5. **REQUIRED** `hostname` - Host Name of Server
6. **REQUIRED** `port` - Port of Server
7. **OPTIONAL** `debug` - Turn on debugging information

 NOTE: Debug should not have an argument.

### Running the system

To run the Server Interface, execute the following command:

 ```
java -jar ServerInterface.jar -mode write -user username -level levelname -path testing/ -hostname hostname -port 8787 -debug
```
