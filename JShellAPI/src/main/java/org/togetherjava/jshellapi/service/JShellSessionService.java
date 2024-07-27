package org.togetherjava.jshellapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.togetherjava.jshellapi.Config;
import org.togetherjava.jshellapi.exceptions.DockerException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class JShellSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JShellSessionService.class);
    private Config config;
    private StartupScriptsService startupScriptsService;
    private ScheduledExecutorService scheduler;
    private DockerService dockerService;
    private final Map<String, JShellService> jshellSessions = new HashMap<>();

    private void initScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            LOGGER.info("Scheduler heartbeat: started.");
            jshellSessions.keySet()
                .stream()
                .filter(id -> jshellSessions.get(id).isClosed())
                .forEach(this::notifyDeath);
            List<String> toDie = jshellSessions.keySet()
                .stream()
                .filter(id -> jshellSessions.get(id).shouldDie())
                .toList();
            LOGGER.info("Scheduler heartbeat: sessions ready to die: {}", toDie);
            for (String id : toDie) {
                try {
                    deleteSession(id);
                } catch (DockerException ex) {
                    LOGGER.error("Unexpected error when deleting session.", ex);
                }
            }
        }, config.schedulerSessionKillScanRateSeconds(),
                config.schedulerSessionKillScanRateSeconds(), TimeUnit.SECONDS);
    }

    void notifyDeath(String id) {
        JShellService shellService = jshellSessions.remove(id);
        if (shellService == null) {
            LOGGER.debug("Notify death on already removed session {}.", id);
            return;
        }
        if (!shellService.isClosed()) {
            LOGGER.error("JShell Service isn't dead when it should for id {}.", id);
        }
        LOGGER.info("Session {} died.", id);
    }

    public boolean hasSession(String id) {
        return jshellSessions.containsKey(id);
    }

    public JShellService session(String id, @Nullable StartupScriptId startupScriptId)
            throws DockerException {
        if (!hasSession(id)) {
            return createSession(new SessionInfo(id, true, startupScriptId, false, config));
        }
        return jshellSessions.get(id);
    }

    public JShellService session(@Nullable StartupScriptId startupScriptId) throws DockerException {
        return createSession(new SessionInfo(UUID.randomUUID().toString(), false, startupScriptId,
                false, config));
    }

    public JShellService oneTimeSession(@Nullable StartupScriptId startupScriptId)
            throws DockerException {
        return createSession(new SessionInfo(UUID.randomUUID().toString(), false, startupScriptId,
                true, config));
    }

    public void deleteSession(String id) throws DockerException {
        JShellService service = jshellSessions.remove(id);
        try {
            service.stop();
        } finally {
            scheduler.schedule(service::close, 500, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized JShellService createSession(SessionInfo sessionInfo)
            throws DockerException {
        // Just in case race condition happens just before createSession
        if (hasSession(sessionInfo.id())) {
            return jshellSessions.get(sessionInfo.id());
        }
        if (jshellSessions.size() >= config.maxAliveSessions()) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many sessions, try again later :(.");
        }
        LOGGER.info("Creating session : {}.", sessionInfo);
        JShellService service = new JShellService(dockerService, this, sessionInfo.id(),
                sessionInfo.sessionTimeout(), sessionInfo.renewable(), sessionInfo.evalTimeout(),
                sessionInfo.evalTimeoutValidationLeeway(), sessionInfo.sysOutCharLimit(),
                config.dockerMaxRamMegaBytes(), config.dockerCPUsUsage(), config.dockerCPUSetCPUs(),
                startupScriptsService.get(sessionInfo.startupScriptId()));
        jshellSessions.put(sessionInfo.id(), service);
        return service;
    }

    /**
     * Schedule the validation of the session timeout. In case the code runs for too long, checks if
     * the wrapper correctly followed the eval timeout and canceled it, if it didn't, forcefully
     * close the session.
     *
     * @param id the id of the session
     * @param timeSeconds the time to schedule
     */
    public void scheduleEvalTimeoutValidation(String id, long timeSeconds) {
        scheduler.schedule(() -> {
            JShellService service = jshellSessions.get(id);
            if (service == null)
                return;
            if (service.isInvalidEvalTimeout()) {
                service.close();
            }
        }, timeSeconds, TimeUnit.SECONDS);
    }

    @Autowired
    public void setConfig(Config config) {
        this.config = config;
        initScheduler();
    }

    @Autowired
    public void setStartupScriptsService(StartupScriptsService startupScriptsService) {
        this.startupScriptsService = startupScriptsService;
    }

    @Autowired
    public void setDockerService(DockerService dockerService) {
        this.dockerService = dockerService;
    }
}
