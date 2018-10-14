# Player Modeling for Parallel

This README provides an introduction to running our player modeling system. There are two JAR executables: `PlayerModel.jar` and `ServerInterface.jar`. Both of these are located in the `executables/` directory.

## Installation/Setup Instructions

Prior to executing any of these JAR files, make sure the Parallel Server is running! Both of these make calls to the server.

To run the `PlayerModel.jar` file, you need to place the directory `pmfiles/` in the same directory as the JAR file. This directory contains information relevant for analyzing player behavior.  

## Player Modeling

### Command Line Arguments
There are a few command line arguments required to execute the player modeling system.

1. mepath - Path to Model Engine Execution File
2. telemetrypath - Path to Telemetry Log
3. parameterpath - Path to Parameter file `currentParameters.txt`
4. level - Level Name
5. user - User Name
6. hostname - Host Name of Server
7. port - Port of Server
8. debug - Turn on debugging information

 NOTE: Debug should not have an argument.

### Running the system

To run the Player Modeling System, execute the following command:
```
java -jar PlayerModel.jar  -mepath testing/me_out_.txt -telemetrypath testing/Session-model.log -parameterpath testing/ -user username -level levelname -hostname hostname -port 8787 -debug
```

## Server Interface

1. mode (read and write)
 * Read: Read player modeling code from server
 * Write: Write player modeling to server  
2. user - User Name
3. level - Level Name
4. path - Path to Parameter file `currentParameters.txt`
5. hostname - Host Name of Server
6. port - Port of Server
7. debug - Turn on debugging information

 NOTE: Debug should not have an argument.

### Running the system

To run the Server Interface, execute the following command:

 ```
java -jar ServerInterface.jar -mode write -user username -level levelname -path testing/ -hostname hostname -port 8787 -debug
```
