public class TimeoutWatcher {
    private final Thread thread;
    private boolean stopped;

    public TimeoutWatcher(int timeoutSeconds, Runnable timeoutAction) {
        Runnable runnable = () -> {
            try {
                Thread.sleep(timeoutSeconds * 1000L);
                timeoutAction.run();
            } catch (InterruptedException e) { //Stopped
            }
        };
        thread = new Thread(runnable);
        thread.setName("Timeout Watcher");
    }
    public synchronized void start() {
        stopped = false;
        thread.start();
    }
    public synchronized void stop() {
        stopped = true;
        thread.interrupt();
    }
    public boolean stopped() {
        return stopped;
    }
}
