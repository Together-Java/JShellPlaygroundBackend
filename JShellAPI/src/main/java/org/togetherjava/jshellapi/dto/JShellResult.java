package org.togetherjava.jshellapi.dto;

import org.springframework.lang.Nullable;

import java.util.List;

public record JShellResult(
        SnippetStatus status,
        SnippetType type,
        int id,
        String source,
        @Nullable
        String result,
        @Nullable
        JShellExceptionResult exception,
        boolean stdoutOverflow,
        String stdout,
        List<String> errors) {
    public JShellResult {
        errors = List.copyOf(errors);
    }
}
