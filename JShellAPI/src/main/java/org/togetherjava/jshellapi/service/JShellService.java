package org.togetherjava.jshellapi.service;

import org.apache.tomcat.util.http.fileupload.util.Closeable;
import org.togetherjava.jshellapi.dto.JShellResult;
import org.togetherjava.jshellapi.dto.SnippetStatus;
import org.togetherjava.jshellapi.dto.SnippetType;
import org.togetherjava.jshellapi.exceptions.DockerException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class JShellService implements Closeable {
    private final String id;
    private Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;

    private Instant lastTimeoutUpdate;
    private final int timeout;
    private final boolean renewable;

    public JShellService(String id, int timeout, boolean renewable) throws DockerException {
        this.id = id;
        this.timeout = timeout;
        this.renewable = renewable;
        this.lastTimeoutUpdate = Instant.now();
        try {
            process = new ProcessBuilder(
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
                    "--name", "\"user%s\"".formatted(id),
                    "jshellwrapper",
                    "java", "-DevalTimeoutSeconds=15", "-jar", "JShellWrapper.jar")
                    .directory(new File(".."))
                    .start();
            writer = process.outputWriter();
            reader = process.inputReader();
        } catch (IOException e) {
            throw new DockerException(e);
        }
    }
    public JShellResult eval(String code) throws DockerException {
        updateLastTimeout();
        if(!code.endsWith("\n")) code += '\n';
        try {
            writer.write("eval");
            writer.newLine();
            writer.write(String.valueOf(code.lines().count()));
            writer.newLine();
            writer.write(code);
            writer.flush();

            SnippetStatus status = Utils.nameOrElseThrow(SnippetStatus.class, reader.readLine(), name -> new DockerException(name + " isn't an enum constant"));
            SnippetType type = Utils.nameOrElseThrow(SnippetType.class, reader.readLine(), name -> new DockerException(name + " isn't an enum constant"));
            int id = Integer.parseInt(reader.readLine());
            String source = desanitize(reader.readLine());
            String result = reader.readLine();
            if(result.equals("NONE")) result = null;
            boolean stdoutOverflow = Boolean.parseBoolean(reader.readLine());
            String stdout = desanitize(reader.readLine());
            List<String> errors = new ArrayList<>();
            String error;
            while(!(error = reader.readLine()).isEmpty()) {
                errors.add(desanitize(error));
            }
            return new JShellResult(status, type, id, source, result, stdoutOverflow, stdout, errors);
        } catch (IOException | NumberFormatException ex) {
            close();
            throw new DockerException(ex);
        }
    }

    public List<String> snippets() throws DockerException {
        updateLastTimeout();
        try {
            writer.write("snippets");
            writer.newLine();
            writer.flush();
            List<String> snippets = new ArrayList<>();
            String snippet;
            while(!(snippet = reader.readLine()).isEmpty()) {
                snippets.add(desanitize(snippet));
            }
            return snippets;
        } catch (IOException ex) {
            close();
            throw new DockerException(ex);
        }
    }

    public boolean shouldDie() {
        return lastTimeoutUpdate.plusSeconds(timeout * 60L).isBefore(Instant.now());
    }

    public void stop() throws DockerException {
        try {
            writer.write("stop");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new DockerException(e);
        }
    }

    public String id() {
        return id;
    }

    @Override
    public void close() {
        process.destroyForcibly();
        try {
            try {
                writer.close();
            } finally {
                reader.close();
            }
            new ProcessBuilder("docker", "kill", "user" + id)
                    .directory(new File(".."))
                    .start()
                    .waitFor();
        } catch(IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        process = null;
    }

    @Override
    public boolean isClosed() {
        return process == null;
    }

    private void updateLastTimeout() {
        if(renewable) {
            lastTimeoutUpdate = Instant.now();
        }
    }

    private static String desanitize(String text) {
        return text.replace("\\n", "\n").replace("\\\\", "\\");
    }

}
