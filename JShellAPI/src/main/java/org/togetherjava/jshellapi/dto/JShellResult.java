package org.togetherjava.jshellapi.dto;

import java.util.List;

import org.springframework.lang.Nullable;

public record JShellResult(
    List<JShellSnippetResult> snippetsResults,
    @Nullable JShellEvalAbortion abortion,
    boolean stdoutOverflow,
    String stdout
) {
    public JShellResult {
        snippetsResults = List.copyOf(snippetsResults);
    }
}
