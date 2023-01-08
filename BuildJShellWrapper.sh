#!/bin/bash
cd JShellWrapper
rm -rf out
javac -d out/production/JShellWrapper src/main/java/*.java
mkdir out/production/JShellWrapper/META-INF
cp src/main/resources/META-INF/MANIFEST.MF out/production/JShellWrapper/META-INF/
cd out/production/JShellWrapper/
jar -cfm ../../JShellWrapper.jar META-INF/MANIFEST.MF *.class
