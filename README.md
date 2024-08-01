# JShellPlaygroundBackend
## Prerequisites
- Java 21
- Docker
## Used technologies
Technologies used in this project, and so technologies that you should know before contributing.
- Java
- Spring
- Gradle
- Docker (knowing Docker is the most important thing in this project)

# Structure of the project
There are two projects, JShellAPI and JShellWrapper.

JShellAPI is a REST API, where whenever some code is received, it will create a session, by creating a docker container, which will run JShellWrapper inside and then execute the given code.

There are three unrelated ways to run JShellPlaygroundBackend:
- [JShellWrapper alone](#how-to-use-jshellwrapper-alone-in-local) in case you specifically want to improve or debug the wrapper
- [JShellAPI in local](#how-to-use-jshellapi) in case you specifically want to improve or debug the API
- [JShellAPI in docker](#how-to-build-jshellapi-and-run-it-in-docker) in case you want to run it like it is used in production, which can be important, because the API needs to create containers from docker inside itself

## How to use JShellWrapper alone in local
This is only useful if your intent is to run the wrapper alone, without the API, for example if you want to improve the wrapper or debug it.
### From console
* Set the environment variable `evalTimeoutSeconds=15` and `sysOutCharLimit=1024`
* * On `Windows CMD`, use `set evalTimeoutSeconds=15 && set sysOutCharLimit=1024`
* * On `Windows Powershell`, use `$env:evalTimeoutSeconds=15 && $env:sysOutCharLimit=1024`
* * On `Linux Shell`, use `export evalTimeoutSeconds=15 && export sysOutCharLimit=1024`
* Run `./gradlew JShellWrapper:run`
* Alternatively, run:
* * On `Windows CMD`, `set evalTimeoutSeconds=15 && set sysOutCharLimit=1024 gradlew JShellWrapper:run`
* * On `Windows Powershell`, `$env:evalTimeoutSeconds=15 && $env:sysOutCharLimit=1024 ./gradlew JShellWrapper:run`
* * On `Linux Shell`, `evalTimeoutSeconds=15 sysOutCharLimit=1024 ./gradlew JShellWrapper:run`
### From IntelliJ
- Go in Run → Run... → Edit Configurations → create a gradle configuration
- Select the command `JShellWrapper:run`
- Click on modify options, and check `VM options`
- Then add `evalTimeoutSeconds=15;sysOutCharLimit=1024` in the VM options
- Press the run button
  ![image](https://github.com/Together-Java/JShellPlaygroundBackend/assets/45936420/01821444-b30f-4f3b-9c23-adda5a3376ae)

## How to use JShellAPI
### From the console
- Launch Docker
- Run `./gradlew :JShellWrapper:jibDockerBuild` to build the image
- Run `./gradlew bootRun`
### How to build and run from IntelliJ
- Launch Docker
- Run `JShellWrapper:jibDockerBuild` to build the image
- Run `bootRun`

# How to build JShellAPI and run it in Docker
- Launch Docker
- Run `./gradlew jibDockerBuild` to create the image
- Create a folder outside the project
- `cd` to this folder
- Copy `docker-compose.yaml` inside it
- Optionally, create `config/application.yaml` where you can put custom config (see the actual [application.yaml](JShellAPI/src/main/resources/application.yaml))
* * If you don't create it, delete the `command: ["--spring.config.location=file:///home/backend/config/application.yaml"]` line in the `docker-compose.yaml`
- Run `docker compose build` or `docker-compose build` in the folder, depending on your version of Docker.
- Run `docker compose start` or `docker-compose start` in the folder, depending on your version of Docker.
- Note that some folders or files may be created automatically inside this folder
- File tree representation:
```
-folder outside the project
    -docker-compose.yaml
    -config (optionnal)
        -application.yaml
```

### Try JShell API
Create a session called `test` and execute `2+2` on it.
Note that the port might be different.
```shell
curl --request POST --url http://localhost:8080/jshell/eval/test --data '2+2'
```

### How to use JShellApi ?
See [JShellAPI README](JShellAPI/README.MD)
