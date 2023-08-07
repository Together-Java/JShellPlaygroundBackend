package org.togetherjava.jshellapi.service;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class StartupScriptsService {
    private record StartupScript(String imports, String script) {}

    private final Map<StartupScriptId, StartupScript> scripts;

    private StartupScriptsService() {
        scripts = new EnumMap<>(StartupScriptId.class);
        for (StartupScriptId id : StartupScriptId.values()) {
            try (
                    InputStream importsStream = Objects.requireNonNull(StartupScriptsService.class.getResourceAsStream("/jshell_startup/imports/" + id + ".jsh"), "Couldn't load script " + id);
                    InputStream scriptStream = Objects.requireNonNull(StartupScriptsService.class.getResourceAsStream("/jshell_startup/scripts/" + id + ".jsh"), "Couldn't load script " + id)) {
                String imports = new String(importsStream.readAllBytes(), StandardCharsets.UTF_8);
                String script = new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
                scripts.put(id, new StartupScript(imports, script));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
    private String get(@Nullable StartupScriptId id, Function<StartupScript, String> function) {
        StartupScript startupScript = scripts.get(id);
        return startupScript != null ? function.apply(startupScript) : function.apply(scripts.get(StartupScriptId.EMPTY));
    }

    /**
     * Returns corresponding imports, or default imports if id is null
     * @param id the id or the imports, can be null
     * @return corresponding imports, or default imports if id is null
     */
    public String getImports(@Nullable StartupScriptId id) {
        return get(id, StartupScript::imports);
    }

    /**
     * Returns corresponding script, or default script if id is null
     * @param id the id or the script, can be null
     * @return corresponding script, or default script if id is null
     */
    public String getScript(@Nullable StartupScriptId id) {
        return get(id, StartupScript::script);
    }
}
