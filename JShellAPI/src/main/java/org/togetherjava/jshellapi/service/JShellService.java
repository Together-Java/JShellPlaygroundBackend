package org.togetherjava.jshellapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

import org.togetherjava.jshellapi.Config;
import org.togetherjava.jshellapi.dto.*;
import org.togetherjava.jshellapi.exceptions.DockerException;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JShellService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JShellService.class);
    private final JShellSessionService sessionService;
    private final String id;
    private final BufferedWriter writer;
    private final BufferedReader reader;
    private boolean markedAsDead;
    private Instant lastTimeoutUpdate;
    private final long timeout;
    private final boolean renewable;
    private final long evalTimeoutValidationLeeway;
    private final long evalTimeout;
    private boolean doingOperation;
    private final DockerService dockerService;
    private final int startupScriptSize;

    public JShellService(
            DockerService dockerService,
            JShellSessionService sessionService,
            SessionInfo sessionInfo,
            Config config
    ) throws DockerException {
        this.dockerService = dockerService;
        this.sessionService = sessionService;
        this.id = sessionInfo.id();
        this.timeout = config.dockerConnectionTimeout();
        this.renewable = sessionInfo.renewable();
        this.evalTimeout = sessionInfo.evalTimeout();
        this.evalTimeoutValidationLeeway = sessionInfo.evalTimeoutValidationLeeway();
        this.lastTimeoutUpdate = Instant.now();

        if (!dockerService.isDead(containerName())) {
            LOGGER.warn("Tried to create an existing container {}.", containerName());
            throw new DockerException("The session isn't completely destroyed, try again later.");
        }

        try {
            ContainerState containerState = dockerService.initializeContainer(containerName(), sessionInfo.startupScriptId());
            this.writer = containerState.containerInput();
            this.reader = containerState.containerOutput();
            checkContainerOK();
            startupScriptSize = Integer.parseInt(reader.readLine());
        } catch (Exception e) {
            LOGGER.warn("Unexpected error during creation.", e);
            markAsDead();
            throw new DockerException("Creation of the session failed.", e);
        }
        this.doingOperation = false;
    }

    public Optional<JShellResult> eval(String code) throws DockerException {
        if (shouldDie())
            throw new DockerException("Session %s is already dead.".formatted(id));
        synchronized (this) {
            if (!tryStartOperation()) {
                return Optional.empty();
            }
        }
        updateLastTimeout();
        sessionService.scheduleEvalTimeoutValidation(id, evalTimeout + evalTimeoutValidationLeeway);
        if (!code.endsWith("\n"))
            code += '\n';
        try {
            writer.write("eval");
            writer.newLine();
            writer.write(String.valueOf(code.lines().count()));
            writer.newLine();
            writer.write(code);
            writer.flush();

            checkContainerOK();

            return Optional.of(readResult());
        } catch (DockerException | IOException | NumberFormatException ex) {
            LOGGER.warn("Unexpected error.", ex);
            markAsDead();
            throw new DockerException(ex);
        } finally {
            stopOperation();
        }
    }

    private JShellResult readResult() throws IOException, NumberFormatException, DockerException {
        final int snippetsCount = Integer.parseInt(reader.readLine());
        List<JShellSnippetResult> snippetResults = new ArrayList<>();
        for (int i = 0; i < snippetsCount; i++) {
            SnippetStatus status = Utils.nameOrElseThrow(SnippetStatus.class, reader.readLine(),
                    name -> new DockerException(name + " isn't an enum constant"));
            SnippetType type = Utils.nameOrElseThrow(SnippetType.class, reader.readLine(),
                    name -> new DockerException(name + " isn't an enum constant"));
            int snippetId = Integer.parseInt(reader.readLine());
            String source = cleanCode(reader.readLine());
            String result = reader.readLine().transform(r -> r.equals("NONE") ? null : r);
            snippetResults.add(new JShellSnippetResult(status, type, snippetId, source, result));
        }
        JShellEvalAbortion abortion = null;
        String rawAbortionCause = reader.readLine();
        if (!rawAbortionCause.isEmpty()) {
            JShellEvalAbortionCause abortionCause = switch (rawAbortionCause) {
                case "TIMEOUT" -> new JShellEvalAbortionCause.TimeoutAbortionCause();
                case "UNCAUGHT_EXCEPTION" -> {
                    String[] split = reader.readLine().split(":");
                    yield new JShellEvalAbortionCause.UnhandledExceptionAbortionCause(split[0],
                            split[1]);
                }
                case "COMPILE_TIME_ERROR" -> {
                    int errorCount = Integer.parseInt(reader.readLine());
                    List<String> errors = new ArrayList<>();
                    for (int i = 0; i < errorCount; i++) {
                        errors.add(Utils.deSanitizeStartupScript((reader.readLine())));
                    }
                    yield new JShellEvalAbortionCause.CompileTimeErrorAbortionCause(errors);
                }
                case "SYNTAX_ERROR" -> new JShellEvalAbortionCause.SyntaxErrorAbortionCause();
                default -> throw new DockerException(
                        "Abortion cause " + rawAbortionCause + " doesn't exist");
            };
            String causeSource = cleanCode(reader.readLine());
            String remainingSource = cleanCode(reader.readLine());
            abortion = new JShellEvalAbortion(causeSource, remainingSource, abortionCause);
        }
        boolean stdoutOverflow = Boolean.parseBoolean(reader.readLine());
        String stdout = Utils.deSanitizeStartupScript(reader.readLine());
        return new JShellResult(snippetResults, abortion, stdoutOverflow, stdout);
    }

    public Optional<List<String>> snippets(boolean includeStartupScript) throws DockerException {
        if (shouldDie())
            throw new DockerException("Session %s is already dead.".formatted(id));
        synchronized (this) {
            if (!tryStartOperation()) {
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
            while (!(snippet = reader.readLine()).isEmpty()) {
                snippets.add(cleanCode(snippet));
            }
            return Optional.of(includeStartupScript ? snippets
                    : snippets.subList(startupScriptSize, snippets.size()));
        } catch (Exception ex) {
            LOGGER.warn("Unexpected error.", ex);
            markAsDead();
            throw new DockerException(ex);
        } finally {
            stopOperation();
        }
    }

    public String containerName() {
        return "session_" + id;
    }

    public boolean isInvalidEvalTimeout() {
        return doingOperation
                && lastTimeoutUpdate.plusSeconds(evalTimeout + evalTimeoutValidationLeeway)
                    .isBefore(Instant.now());
    }

    /**
     * Returns if this session should be killed in the next heartbeat of the session killer.
     * 
     * @return true if this session should be killed in the next heartbeat of the session killer
     *         false otherwise
     */
    public boolean isMarkedAsDead() {
        return this.markedAsDead;
    }

    /**
     * Marks this session as dead and also tries to gracefully close it, so it can be killed in the
     * next heartbeat of the session killer.
     */
    public synchronized void markAsDead() {
        if (this.markedAsDead)
            return;
        LOGGER.info("Session {} marked as dead.", id);
        this.markedAsDead = true;

        try {
            writer.write("exit");
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            LOGGER.debug("Couldn't close session {} gracefully.", id, ex);
        }
    }

    /**
     * Returns if this session should be killed. Returns true if either it is marked as dead, if the
     * timeout is reached or if the container is dead.
     * 
     * @return true if this session should be killed, false otherwise
     */
    public boolean shouldDie() {
        return markedAsDead || lastTimeoutUpdate.plusSeconds(timeout).isBefore(Instant.now())
                || dockerService.isDead(containerName());
    }

    public String id() {
        return id;
    }

    public void close() {
        try {
            writer.close();
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while closing the writer.", ex);
        }
        try {
            reader.close();
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while closing the reader.", ex);
        }
        try {
            if (!dockerService.isDead(containerName())) {
                dockerService.killContainerByName(containerName());
            }
        } catch (Exception ex) {
            LOGGER.error("Unexpected error while destroying the container.", ex);
        }
    }

    private void updateLastTimeout() {
        if (renewable) {
            lastTimeoutUpdate = Instant.now();
        }
    }

    private void checkContainerOK() throws DockerException {
        try {
            if (dockerService.isDead(containerName())) {
                throw new IOException("Container of session " + id + " is dead");
            }
            String ok = reader.readLine();
            if (ok == null || !ok.equals("OK")) {
                throw new IOException(
                        "Container of session " + id + " is dead because status was " + ok);
            }
        } catch (IOException ex) {
            throw new DockerException(ex);
        }
    }

    private synchronized boolean tryStartOperation() {
        if (doingOperation)
            return false;
        doingOperation = true;
        return true;
    }

    private void stopOperation() {
        doingOperation = false;
    }

    private static String cleanCode(String code) {
        return code.translateEscapes();
    }
}
