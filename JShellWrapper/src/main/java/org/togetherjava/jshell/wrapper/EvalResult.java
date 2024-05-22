package org.togetherjava.jshell.wrapper;

import java.util.List;

import jdk.jshell.SnippetEvent;

public record EvalResult(List<SnippetEvent> events, JShellEvalAbortion abortion) {}
