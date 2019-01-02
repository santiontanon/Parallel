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

### ME

The ME (the Model Checker, called the "ME" as a reference to the "Malevolence Engine" in the Manufactoria game) is in charge of testing the solutions provided by the user. It can run in two modes: "test" and "submit". In "test", it will just generate one execution of the solution, and in "submit", it will search throught the space of all possible schedules trying to find one that fails. To run "test", use the support.Play file, and to run "submit" use the support.ME file.

#### Command Line Arguments for support.Play


1. **REQURIED** `level` - the path to the level file, including the player solution, to be simulated. The output of the execution will be saved in the same folder as this level file.
2. **REQURIED** `selection_strategy` - if a negative value is specified, this is taken as a random seed to be used for random selection of speeds of the arrows. If a positive value is specified (e.g., 0), the execution will not use random number generation, but always select the "selection_strategy"-th possible arrow speed at each time interval (this is only useful for debugging)
3. **OPTIONAL** `max_steps` - maximum number of steps to simulate (by default it's 1000).

#### Running the support.Play system

To run the Model Engine, execute the following command:

```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml support.Play -1000
```

#### Command Line Arguments for support.ME


1. **REQURIED** `level` - the path to the level file, including the player solution, to be simulated. The output of the execution will be saved in the same folder as this level file.
2. **REQURIED** `budget` - the amount of search to be done byt he ME when searching for a schedule that fails before giving up. The recommended value is 60000 
3. **OPTIONAL** `time` - Timeout in milliseconds (if the search takes longer than this, it will be abandoned).

#### Running the support.ME system

To run the Model Engine, execute the following command:

```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml support.ME my-level.txt 60000
```

## Procedural Content Generator (PCG) System

The PCG system is designed to generate new levels for Parallel. It can generate personalized levels based on a player model generated by the Player Modeling System.

#### Command Line Arguments

1. **REQURIED** `player_model` (the path to the player model to be used as input, alternatively this can just be the string `debug`, for running the PCG component in debug mode)
2. **REQURIED** `random_seed` - specify a negative random seed to run the PCG component in deterministic mode (generating the same level eery time for the same random seed, given the same player_model), or a positive one for running in non-deterministic mode (in this case, the random seed still affects generation, but it is not guaranteed that the same level will be generated every time)
3. **REQUIRED** `size` - Level size: 0 for the smallest levels. 1, 2 or 3 for larger levels. Values larger than 2 or 3 are not recommended, as levels will be too large for the ME.
4. **OPTIONAL** `path` - optional parameter specifying the path where to write the generate level. If it is not specified, it will be generated in the same path as the player_model file was found

#### Running the system

For example, to run the PCG System, execute the following command:

```
java -classpath PCGPM4PP.jar -Dlog4j.configurationFile=pmfiles/log4j2.xml support.PCG parameter_file.txt -1000 1
```
