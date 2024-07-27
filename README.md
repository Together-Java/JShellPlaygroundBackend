# JShellPlaygroundBackend
## Prerequisites
- Java 21
- Docker

# Structure of the project
There are two projects, JShellAPI and JShellWrapper
JShellAPI is a REST API, and whenever some code is received, it will create a session, by creating a docker container, which will run JShellWrapper inside and then execute the given code in JShellWrapper.

## How to use JShellWrapper alone in local
### From IntelliJ
- Go in Run → Run... → Edit Configurations → create a configuration
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

# How to build JShellAPI in and run it in Docker
- Launch Docker
- Run `jibDockerBuild` to create the image
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

## How to use JShellApi ?
See [JShellAPI README](JShellAPI/README.MD)
