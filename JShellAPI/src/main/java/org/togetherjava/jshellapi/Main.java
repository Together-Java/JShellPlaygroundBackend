package org.togetherjava.jshellapi;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "docker",
                "run",
                "--rm",
                "-i",
                "--init",
                "--cap-drop=ALL",
                "--network=none",
                "--pids-limit=2000",
                "--memory=500M",
                "--read-only",
                "--name", "\"user%d\"".formatted(138235433115975680L),
                "jshellwrapper")
                .directory(new File(".."))
                .inheritIO()
                .start();
        process.waitFor();
        System.out.println("Done");
    }
}