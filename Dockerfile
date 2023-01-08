FROM eclipse-temurin:19-alpine

COPY JShellWrapper/out/JShellWrapper.jar .

CMD [ "java", "-jar", "JShellWrapper.jar" ]
