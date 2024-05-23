package org.togetherjava.jshell.wrapper;

import jdk.jshell.JShell;

import java.util.concurrent.atomic.AtomicBoolean;

public record JShellEvalStop(JShell shell, AtomicBoolean hasStopped) implements Runnable {
    @Override
    public void run() {
        hasStopped.set(true);
        shell.stop();
    }
}
