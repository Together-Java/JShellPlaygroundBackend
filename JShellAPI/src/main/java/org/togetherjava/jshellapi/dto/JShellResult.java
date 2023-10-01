package org.togetherjava.jshellapi.dto;

import java.util.List;

public record JShellResult(
        List<JShellSnippetResult> snippetsResults,
        boolean stdoutOverflow,
        String stdout) {
    public JShellResult {
        snippetsResults = List.copyOf(snippetsResults);
    }
}
