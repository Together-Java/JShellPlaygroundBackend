package org.togetherjava.jshell.wrapper;

public record JShellEvalAbortion(
    String sourceCause,
    String remainingSource,
    JShellEvalAbortionCause cause
) {}
