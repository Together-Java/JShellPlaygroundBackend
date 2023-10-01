package org.togetherjava.jshellapi.dto;

import org.springframework.lang.Nullable;

import java.util.List;

public record NormalJShellSnippetResult(
        SnippetStatus status,
        SnippetType type,
        int id,
        String source,
        @Nullable
        String result,
        @Nullable
        JShellExceptionResult exception,
        List<String> errors) implements JShellSnippetResult {
    public NormalJShellSnippetResult {
        errors = List.copyOf(errors);
    }
}
