public class TimeoutWatcher {
    private final Thread thread;
    private boolean timeout;

    public TimeoutWatcher(int timeoutSeconds, Runnable timeoutAction) {
        Runnable runnable =
                () -> {
                    try {
                        Thread.sleep(timeoutSeconds * 1000L);
                    } catch (InterruptedException e) { // Stopped
                        return;
                    }
                    timeout = true;
                    timeoutAction.run();
                };
        thread = new Thread(runnable);
        thread.setName("Timeout Watcher");
    }

    public synchronized void start() {
        timeout = false;
        thread.start();
    }

    public synchronized void stop() {
        thread.interrupt();
    }

    public boolean isTimeout() {
        return timeout;
    }
}
