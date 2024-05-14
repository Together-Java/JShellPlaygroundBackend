package org.togetherjava.jshellapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jshellapi")
public record Config(
        long regularSessionTimeoutSeconds,
        long oneTimeSessionTimeoutSeconds,
        long evalTimeoutSeconds,
        long evalTimeoutValidationLeeway,
        int sysOutCharLimit,
        long maxAliveSessions,
        int dockerMaxRamMegaBytes,
        double dockerCPUsUsage,
        long schedulerSessionKillScanRateSeconds,
        long dockerResponseTimeout,
        long dockerConnectionTimeout) {
    public Config {
        if(regularSessionTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + regularSessionTimeoutSeconds);
        if(oneTimeSessionTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + oneTimeSessionTimeoutSeconds);
        if(evalTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + evalTimeoutSeconds);
        if(evalTimeoutValidationLeeway <= 0) throw new RuntimeException("Invalid value " + evalTimeoutSeconds);
        if(sysOutCharLimit <= 0) throw new RuntimeException("Invalid value " + sysOutCharLimit);
        if(maxAliveSessions <= 0) throw new RuntimeException("Invalid value " + maxAliveSessions);
        if(dockerMaxRamMegaBytes <= 0) throw new RuntimeException("Invalid value " + dockerMaxRamMegaBytes);
        if(dockerCPUsUsage <= 0) throw new RuntimeException("Invalid value " + dockerCPUsUsage);
        if(schedulerSessionKillScanRateSeconds <= 0) throw new RuntimeException("Invalid value " + schedulerSessionKillScanRateSeconds);
        if(dockerResponseTimeout <= 0) throw new RuntimeException("Invalid value " + dockerResponseTimeout);
        if(dockerConnectionTimeout <= 0) throw new RuntimeException("Invalid value " + dockerConnectionTimeout);
    }
}
