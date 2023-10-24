# JShellPlaygroundBackend
## Prerequisites
- Java 19
- Docker

## How to use JShellWrapper alone
### From command line
- Run BuildJShellWrapper.bat/.sh to compile the wrapper and package it into a jar
- Run java -DevalTimeoutSeconds=15 -jar ./JShellWrapper/out/JShellWrapper.jar
### From IntelliJ
- Open IntelliJ on the project JShellWrapper
- Go in Run → Run... → Edit Configurations → create a configuration
- Select the main class, the jdk, etc, click on modify options, and check VM options, then add -DevalTimeoutSeconds=15 in the VM options
- Press the run button
## How to use JShellWrapper with JShellApi
### From the console
- Run BuildJShellWrapper.bat/.sh to compile the wrapper and package it into a jar
- Run BuildImage.bat/.sh to build the image
- Run ./gradlew bootRun
### How to build and run from IntelliJ
- Run BuildJShellWrapper.bat/.sh
- Open IntelliJ on JShellAPI
- Run the main class
### using IntelliJ instead of BuildJShellWrapper script
- Open IntelliJ on the project JShellWrapper
- Go in build → Build Artifacts... → Build
- The jar should be located directly under JShellWrapper/out

## How to use JShellApi ?
See [GUIDE.MD](JShellAPI/GUIDE.MD)