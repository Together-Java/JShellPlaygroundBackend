package org.togetherjava.jshell.wrapper;

import java.util.concurrent.atomic.AtomicBoolean;

import jdk.jshell.JShell;

public record JShellEvalStop(JShell shell, AtomicBoolean hasStopped) implements Runnable {
    @Override
    public void run() {
        hasStopped.set(true);
        shell.stop();
    }
}
