package org.togetherjava.jshellapi.service;

import org.apache.tomcat.util.http.fileupload.util.Closeable;
import org.togetherjava.jshellapi.dto.*;
import org.togetherjava.jshellapi.exceptions.DockerException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JShellService implements Closeable {
    private final JShellSessionService sessionService;
    private final String id;
    private Process process;
    private final BufferedWriter writer;
    private final BufferedReader reader;

    private Instant lastTimeoutUpdate;
    private final long timeout;
    private final boolean renewable;
    private boolean doingOperation;

    public JShellService(JShellSessionService sessionService, String id, long timeout, boolean renewable, long evalTimeout, int maxMemory, double cpus, String startupScript) throws DockerException {
        this.sessionService = sessionService;
        this.id = id;
        this.timeout = timeout;
        this.renewable = renewable;
        this.lastTimeoutUpdate = Instant.now();
        try {
            Path errorLogs = Path.of("logs", "container", containerName() + ".log");
            if(!Files.isRegularFile(errorLogs)) {
                Files.createDirectories(errorLogs.getParent());
                Files.createFile(errorLogs);
            }
            process = new ProcessBuilder(
                    "docker",
                    "run",
                    "--rm",
                    "-i",
                    "--init",
                    "--cap-drop=ALL",
                    "--network=none",
                    "--pids-limit=2000",
                    "--read-only",
                    "--memory=" + maxMemory + "m",
                    "--cpus=" + cpus,
                    "--name", containerName(),
                    "-e", "\"evalTimeoutSeconds=%d\"".formatted(evalTimeout),
                    "togetherjava.org:5001/togetherjava/jshellwrapper:master")
                    .directory(new File(".."))
                    .redirectError(errorLogs.toFile())
                    .start();
            writer = process.outputWriter();
            reader = process.inputReader();
            writer.write(sanitize(startupScript));
            writer.newLine();
        } catch (IOException e) {
            throw new DockerException(e);
        }
        this.doingOperation = false;
    }
    public Optional<JShellResult> eval(String code) throws DockerException {
        synchronized (this) {
            if(!tryStartOperation())  {
                return Optional.empty();
            }
        }
        updateLastTimeout();
        if(!code.endsWith("\n")) code += '\n';
        try {
            writer.write("eval");
            writer.newLine();
            writer.write(String.valueOf(code.lines().count()));
            writer.newLine();
            writer.write(code);
            writer.flush();

            checkContainerOK();

            return Optional.of(readResult());
        } catch (IOException | NumberFormatException ex) {
            close();
            throw new DockerException(ex);
        } finally {
            stopOperation();
        }
    }
    private JShellResult readResult() throws IOException, NumberFormatException, DockerException {
        final int snippetsCount = Integer.parseInt(reader.readLine());
        List<JShellSnippetResult> snippetResults = new ArrayList<>();
        for(int i = 0; i < snippetsCount; i++) {
            SnippetStatus status = Utils.nameOrElseThrow(SnippetStatus.class, reader.readLine(), name -> new DockerException(name + " isn't an enum constant"));
            SnippetType type = Utils.nameOrElseThrow(SnippetType.class, reader.readLine(), name -> new DockerException(name + " isn't an enum constant"));
            int snippetId = Integer.parseInt(reader.readLine());
            String source = desanitize(reader.readLine());
            String result = reader.readLine().transform(r -> r.equals("NONE") ? null : r);
            snippetResults.add(new JShellSnippetResult(status, type, snippetId, source, result));
        }
        JShellEvalAbortion abortion = null;
        String rawAbortionCause = reader.readLine();
        if(!rawAbortionCause.isEmpty()) {
            JShellEvalAbortionCause abortionCause = switch (rawAbortionCause) {
                case "TIMEOUT" -> new JShellEvalAbortionCause.TimeoutAbortionCause();
                case "UNCAUGHT_EXCEPTION" -> {
                    String[] split = reader.readLine().split(":");
                    yield new JShellEvalAbortionCause.UnhandledExceptionAbortionCause(split[0], split[1]);
                }
                case "COMPILE_TIME_ERROR" -> {
                    int errorCount = Integer.parseInt(reader.readLine());
                    List<String> errors = new ArrayList<>();
                    for(int i = 0; i < errorCount; i++) {
                        errors.add(desanitize(reader.readLine()));
                    }
                    yield new JShellEvalAbortionCause.CompileTimeErrorAbortionCause(errors);
                }
                case "SYNTAX_ERROR" -> new JShellEvalAbortionCause.SyntaxTimeErrorAbortionCause();
                default -> throw new DockerException("Abortion cause " + rawAbortionCause + " doesn't exist");
            };
            String causeSource = reader.readLine();
            String remainingSource = reader.readLine();
            abortion = new JShellEvalAbortion(causeSource, remainingSource, abortionCause);
        }
        boolean stdoutOverflow = Boolean.parseBoolean(reader.readLine());
        String stdout = desanitize(reader.readLine());
        return new JShellResult(snippetResults, abortion, stdoutOverflow, stdout);
    }

    public Optional<List<String>> snippets() throws DockerException {
        synchronized (this) {
            if(!tryStartOperation())  {
                return Optional.empty();
            }
        }
        updateLastTimeout();
        try {
            writer.write("snippets");
            writer.newLine();
            writer.flush();

            checkContainerOK();

            List<String> snippets = new ArrayList<>();
            String snippet;
            while(!(snippet = reader.readLine()).isEmpty()) {
                snippets.add(desanitize(snippet));
            }
            return Optional.of(snippets);
        } catch (IOException ex) {
            close();
            throw new DockerException(ex);
        } finally {
            stopOperation();
        }
    }

    public String containerName() {
        return "session_" + id;
    }

    public boolean shouldDie() {
        return lastTimeoutUpdate.plusSeconds(timeout).isBefore(Instant.now());
    }

    public void stop() throws DockerException {
        try {
            writer.write("exit");
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
            new ProcessBuilder("docker", "kill", containerName())
                    .directory(new File(".."))
                    .start()
                    .waitFor();
        } catch(IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
        process = null;
        sessionService.notifyDeath(id);
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

    private void checkContainerOK() throws DockerException {
        try {
            String OK = reader.readLine();
            if(OK == null) {
                try {
                    close();
                } finally {
                    throw new DockerException("Container of session " + id + " is dead");
                }
            }
            if(!OK.equals("OK")) {
                try {
                    close();
                } finally {
                    throw new DockerException("Container of session " + id + " returned invalid info : " + OK);
                }
            }
        } catch (IOException ex) {
            throw new DockerException(ex);
        }
    }

    private synchronized boolean tryStartOperation() {
        if(doingOperation) return false;
        doingOperation = true;
        return true;
    }
    private void stopOperation() {
        doingOperation = false;
    }

    private static String sanitize(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String desanitize(String text) {
        return text.replace("\\n", "\n").replace("\\\\", "\\");
    }

}
