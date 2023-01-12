package org.togetherjava.jshellapi.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.togetherjava.jshellapi.Config;
import org.togetherjava.jshellapi.exceptions.DockerException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class JShellSessionService {
    private Config config;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(6);
    private final Map<String, JShellService> jshellSessions = new HashMap<>();

    public JShellSessionService() {
        executor.scheduleAtFixedRate(() -> {
            List<String> toDie = jshellSessions.keySet()
                    .stream()
                    .filter(id -> jshellSessions.get(id).shouldDie())
                    .toList();
            for(String id : toDie) {
                try {
                    deleteSession(id);
                } catch (DockerException ex) {
                    ex.printStackTrace();
                }
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public boolean hasSession(String id) {
        return jshellSessions.containsKey(id);
    }

    public JShellService sessionById(String id) throws DockerException {
        if(!jshellSessions.containsKey(id)) {
            jshellSessions.put(id, new JShellService(id, config.regularSessionTimeoutSeconds(), true, config.evalTimeoutSeconds()));
        }
        return jshellSessions.get(id);
    }
    public JShellService oneTimeSession() throws DockerException {
        JShellService service = new JShellService(UUID.randomUUID().toString(), config.oneTimeSessionTimeoutSeconds(), false, config.evalTimeoutSeconds());
        jshellSessions.put(service.id(), service);
        return service;
    }

    public void deleteSession(String id) throws DockerException {
        JShellService service = jshellSessions.remove(id);
        service.stop();
        executor.schedule(service::close, 500, TimeUnit.MILLISECONDS);
    }

    @Autowired
    public void setConfig(Config config) {
        this.config = config;
    }
}
