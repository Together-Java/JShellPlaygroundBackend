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
            List<String> toDie = jshellSessions.keySet()
                .stream()
                .filter(id -> jshellSessions.get(id).shouldDie())
                .toList();
            LOGGER.info("Scheduler heartbeat, sessions ready to die: {}", toDie);
            for (String id : toDie) {
                JShellService service = jshellSessions.get(id);
                if (service.isMarkedAsDead()) {
                    try {
                        jshellSessions.remove(id).close();
                        LOGGER.info("Session {} died.", id);
                    } catch (Exception ex) {
                        LOGGER.error("Unexpected exception for session {}", id, ex);
                    }
                } else {
                    service.markAsDead();
                }
            }
        }, config.schedulerSessionKillScanRateSeconds(),
                config.schedulerSessionKillScanRateSeconds(), TimeUnit.SECONDS);
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

    public void deleteSession(String id) {
        jshellSessions.get(id).markAsDead();
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
        JShellService service = new JShellService(dockerService, this, sessionInfo, config);


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
                deleteSession(id);
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
