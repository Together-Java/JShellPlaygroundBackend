package org.togetherjava.jshellapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jshellapi")
public record Config(
        long regularSessionTimeoutSeconds,
        long oneTimeSessionTimeoutSeconds,
        long evalTimeoutSeconds,
        long maxAliveSessions,
        int schedulerThreadCount,
        long schedulerSessionKillScanRate) {
    public Config {
        if(regularSessionTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + regularSessionTimeoutSeconds);
        if(oneTimeSessionTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + oneTimeSessionTimeoutSeconds);
        if(evalTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + evalTimeoutSeconds);
        if(maxAliveSessions <= 0) throw new RuntimeException("Invalid value " + maxAliveSessions);
        if(schedulerThreadCount <= 0) throw new RuntimeException("Invalid value " + schedulerThreadCount);
        if(schedulerSessionKillScanRate <= 0) throw new RuntimeException("Invalid value " + schedulerSessionKillScanRate);
    }
}
