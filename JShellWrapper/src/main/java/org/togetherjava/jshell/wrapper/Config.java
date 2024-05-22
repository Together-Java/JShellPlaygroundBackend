package org.togetherjava.jshell.wrapper;

public record Config(int evalTimeoutSeconds, int sysOutCharLimit) {
    static int loadIntEnv(String envName) {
        return Integer.parseInt(System.getenv(envName));
    }

    public static Config load() {
        return new Config(loadIntEnv("evalTimeoutSeconds"), loadIntEnv("sysOutCharLimit"));
    }

    public Config {
        if (evalTimeoutSeconds <= 0)
            throw new IllegalArgumentException(
                "Invalid evalTimeoutSeconds : " + evalTimeoutSeconds
            );
        if (sysOutCharLimit <= 0)
            throw new IllegalArgumentException("Invalid sysOutCharLimit : " + sysOutCharLimit);
    }
}
