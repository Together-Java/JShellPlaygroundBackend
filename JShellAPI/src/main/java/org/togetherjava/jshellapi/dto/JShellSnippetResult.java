package org.togetherjava.jshellapi.dto;

public sealed interface JShellSnippetResult permits NormalJShellSnippetResult, AfterInterruptionJShellSnippetResult {
}
