package org.togetherjava.jshell.wrapper;

import jdk.jshell.SnippetEvent;

import java.util.List;

public record EvalResult(List<SnippetEvent> events, JShellEvalAbortion abortion) {
}
