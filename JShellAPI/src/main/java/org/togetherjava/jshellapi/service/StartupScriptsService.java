package org.togetherjava.jshellapi.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class StartupScriptsService {

    private final Map<StartupScriptId, String> scripts;

    private StartupScriptsService() {
        scripts = new EnumMap<>(StartupScriptId.class);
        for (StartupScriptId id : StartupScriptId.values()) {
            try (
                InputStream scriptStream =
                    Objects.requireNonNull(
                        StartupScriptsService.class.getResourceAsStream(
                            "/jshell_startup/" + id + ".jsh"
                        ),
                        "Couldn't load script " + id
                    )
            ) {
                String script = new String(scriptStream.readAllBytes(), StandardCharsets.UTF_8);
                script = cleanEndLines(script);
                scripts.put(id, script);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private String cleanEndLines(String s) {
        return s.replace("\r", "");
    }

    /**
     * Returns corresponding script, or default script if id is null
     *
     * @param id the id or the script, can be null
     * @return corresponding script, or default script if id is null
     */
    public String get(@Nullable StartupScriptId id) {
        String startupScript = scripts.get(id);
        return startupScript != null ? startupScript : scripts.get(StartupScriptId.EMPTY);
    }
}
