# JShellPlaygroundBackend
## Prerequisites
- Java 19
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
- Create a folder
- Create `docker-compose.yml`:
 ```yml
services:
  jshell-backend-master:
    image: "togetherjava.org:5001/togetherjava/jshellbackend:master"
    command: ["--spring.config.location=file:///home/backend/config/application.properties"]
    restart: always
    ports:
      - 54321:8080
      - 5005:5005
    environment:
      JAVA_TOOL_OPTIONS: "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./docker-config.json:/root/.docker/config.json
      - ./config:/home/backend/config
    networks:
      - jshell-backend

networks:
  jshell-backend:
    external: true
    name: develop-bot
  ```
- Optionaly, create `config/application.properties` where you can put custom config (see the actual [application.properties](JShellAPI/src/main/resources/application.properties))
- If you don't create it, delete the `command: ["--spring.config.location=file:///home/backend/config/application.properties"]` line in the `docker-compose.yml`
- Run `docker compose build` or `docker-compose build` in the folder, depending of your version of Docker.
- Run `docker compose start` or `docker-compose start` in the folder, depending of your version of Docker.

## How to use JShellApi ?
See [GUIDE.MD](JShellAPI/README.MD)
