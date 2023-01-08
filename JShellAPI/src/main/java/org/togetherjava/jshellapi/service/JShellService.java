package org.togetherjava.jshellapi.service;

import org.togetherjava.jshellapi.exceptions.DockerException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

public class JShellService {
    public String helloWorld(long id) throws DockerException {
        try {
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
                .start();
            try (BufferedWriter writer = process.outputWriter()) {
                writer.write(String.valueOf(id));
                writer.newLine();
            }
            String hello;
            try (BufferedReader reader = process.inputReader()) {
                hello = reader.readLine();
            }
            process.waitFor();
            return hello;
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

}
