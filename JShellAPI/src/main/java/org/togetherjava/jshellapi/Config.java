package org.togetherjava.jshellapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;

@ConfigurationProperties("jshellapi")
public record Config(long regularSessionTimeoutSeconds, long oneTimeSessionTimeoutSeconds,
        long evalTimeoutSeconds, long evalTimeoutValidationLeeway, int sysOutCharLimit,
        long maxAliveSessions, int dockerMaxRamMegaBytes, double dockerCPUsUsage,
        @Nullable String dockerCPUSetCPUs, long schedulerSessionKillScanRateSeconds,
        long dockerResponseTimeout, long dockerConnectionTimeout) {
    public Config {
        if (regularSessionTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Invalid value " + regularSessionTimeoutSeconds);
        if (oneTimeSessionTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Invalid value " + oneTimeSessionTimeoutSeconds);
        if (evalTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Invalid value " + evalTimeoutSeconds);
        if (evalTimeoutValidationLeeway <= 0)
            throw new IllegalArgumentException("Invalid value " + evalTimeoutSeconds);
        if (sysOutCharLimit <= 0)
            throw new IllegalArgumentException("Invalid value " + sysOutCharLimit);
        if (maxAliveSessions <= 0)
            throw new IllegalArgumentException("Invalid value " + maxAliveSessions);
        if (dockerMaxRamMegaBytes <= 0)
            throw new IllegalArgumentException("Invalid value " + dockerMaxRamMegaBytes);
        if (dockerCPUsUsage <= 0)
            throw new IllegalArgumentException("Invalid value " + dockerCPUsUsage);
        if (dockerCPUSetCPUs != null && !dockerCPUSetCPUs.matches("[1-9]?\\d([-,]\\d?\\d)?"))
            throw new IllegalArgumentException("Invalid value " + dockerCPUSetCPUs);
        if (schedulerSessionKillScanRateSeconds <= 0)
            throw new IllegalArgumentException(
                    "Invalid value " + schedulerSessionKillScanRateSeconds);
        if (dockerResponseTimeout <= 0)
            throw new IllegalArgumentException("Invalid value " + dockerResponseTimeout);
        if (dockerConnectionTimeout <= 0)
            throw new IllegalArgumentException("Invalid value " + dockerConnectionTimeout);
    }
}
