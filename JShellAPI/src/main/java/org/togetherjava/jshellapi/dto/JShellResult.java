package org.togetherjava.jshellapi.dto;

import java.util.List;

public record JShellResult(
        SnippetStatus status,
        SnippetType type,
        int id,
        String source,
        String result,
        JShellExceptionResult exception,
        boolean stdoutOverflow,
        String stdout,
        List<String> errors) {
    public JShellResult {
        errors = List.copyOf(errors);
    }
}
