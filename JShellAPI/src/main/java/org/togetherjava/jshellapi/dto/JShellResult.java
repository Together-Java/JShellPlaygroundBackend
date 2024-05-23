package org.togetherjava.jshellapi.dto;

import org.springframework.lang.Nullable;

import java.util.List;

public record JShellResult(List<JShellSnippetResult> snippetsResults,
        @Nullable JShellEvalAbortion abortion, boolean stdoutOverflow, String stdout) {
    public JShellResult {
        snippetsResults = List.copyOf(snippetsResults);
    }
}
