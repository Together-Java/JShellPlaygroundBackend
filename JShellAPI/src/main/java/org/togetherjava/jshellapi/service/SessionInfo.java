package org.togetherjava.jshellapi.service;

import org.springframework.lang.Nullable;
import org.togetherjava.jshellapi.Config;

public record SessionInfo(String id, long sessionTimeout, boolean renewable, long evalTimeout, long evalTimeoutValidationLeeway, int sysOutCharLimit, @Nullable StartupScriptId startupScriptId) {
    public SessionInfo(String id, boolean renewable, StartupScriptId startupScriptId, Config config) {
        this(id, config.regularSessionTimeoutSeconds(), renewable, config.evalTimeoutSeconds(), config.evalTimeoutValidationLeeway(), config.sysOutCharLimit(), startupScriptId);
    }
}
