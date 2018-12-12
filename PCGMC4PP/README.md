# Player Modeling and PCG Systems for Parallel

This README provides instructions on running the Player Modeling System and the Procedural Content Generator (PCG) System. The Player Modeling System is made up of three components: `Player Modeler (PM)`, `Server Interface (SI)`, and `Model Execution (ME)`. There is one JAR executable for both systems, `PCGPM4PP.jar`, located in the `dist/` directory. All systems are run using this JAR executable.

## Installation/Setup Instructions

Prior to executing any of the systems, make sure the Parallel Server is running! Both the Player Modeler and Server Interface make calls to the server! If the server is not available, both components will save data to the local filesystem.

## Player Modeling System

 Recall that the Player Modeling System consists of three components: `Player Modeler (PM)`, `Server Interface (SI)`, and `Model Execution (ME)`. We describe how to run each of these below.

**NOTE:** Prior to running the Player Modeling System, you need to place the directory `pmfiles/` in the same directory as the JAR file. This directory contains information relevant for analyzing player behavior.

The `pmfiles/` directory **must** contain the following files:

* `critical_sections/` - critical section information for non-PCG levels
* `level_skills` - list of skills per level for non-PCG level_skills
* `skills.txt` - list of all skills
* `classifier-model.model` - Trained model of machine learning classifier
* `log4j2.xml` - Logging configuration file

### Player Modeler

The Player Modeler takes telemetry information from the game and execution from the Model Engine, and attempts to predict the level of understanding of a set of skills.

#### Command Line Arguments

1. **REQURIED** `mepath` - Path to Model Engine Execution File
2. **REQURIED** `telemetrypath` - Path to Telemetry Log
3. **REQUIRED** `parameterpath` - Path to Parameter file
4. **REQUIRED** `level` - Level Name
5. **REQUIRED** `user` - User Name
6. **REQUIRED** `hostname` - Host Name of Server
7. **REQUIRED** `port` - Port of Server

#### Running the system

To run the Player Modeling System, execute the following command:
```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml playermodeling.Main -mepath testing/me_out_.txt -telemetrypath testing/Session-model.log -parameterpath testing/currentParameters.txt -user username -level levelname -hostname hostname -port 8787
```

### Server Interface

The Server Interface allows Parallel and the Player Modeler to communicate with the Parallel Server. The Server Interface uses HTTP requests to send and receive player modeling data for a given player. The skill vector of a player will be saved on the local machine at the path provided as a command line argument. Additionally, player modeling data will be saved locally in the same directory as the parameter file under the name `localPMData.json`

#### Command Line Arguments

1. **REQURIED** `mode` (read or sync)
 * sync: Synchronize local player modeling data with server
 * save: Add skill vector to player modeling data  
2. **REQURIED** `user` - User Name
3. **REQUIRED if writing** `level` - Level Name
4. **REQUIRED** `path` - Path to Parameter file
5. **REQUIRED** `hostname` - Host Name of Server
6. **REQUIRED** `port` - Port of Server

#### Running the system

To run the Server Interface, execute the following command:

```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml server.ServerInterface -mode sync -user username -level levelname -path testing/ -hostname hostname -port 8787
```

### Model Engine

TODO: Add a brief description of the ME.

#### Command Line Arguments

#### Running the system

To run the Model Engine, execute the following command:

```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml
```

## Procedural Content Generator (PCG) System

TODO: Add a brief description of the PCG system.

#### Command Line Arguments

#### Running the system

To run the PCG System, execute the following command:

```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml
```
