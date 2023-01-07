FROM eclipse-temurin:19

WORKDIR /home

COPY JShellWrapper/out/JShellWrapper.jar /home/JShellWrapper.jar

CMD [ "java", "-jar", "JShellWrapper.jar" ]