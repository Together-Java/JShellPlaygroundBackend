package org.togetherjava.jshellapi.dto;

import org.springframework.lang.Nullable;

public record JShellSnippetResult(
    SnippetStatus status,
    SnippetType type,
    int id,
    String source,
    @Nullable String result
) {}
