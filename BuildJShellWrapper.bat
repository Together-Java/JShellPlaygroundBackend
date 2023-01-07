cd JShellWrapper
rmdir /s /q out
javac -d out/production/JShellWrapper src/main/java/*.java
xcopy src\\main\\resources\\META-INF\\MANIFEST.MF out\\production\\JShellWrapper\\META-INF\\
cd out/production/JShellWrapper/
jar -cfm ../../JShellWrapper.jar META-INF/MANIFEST.MF *.class