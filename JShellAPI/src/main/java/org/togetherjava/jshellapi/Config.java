package org.togetherjava.jshellapi;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

@ConfigurationProperties("jshellapi")
public record Config(long regularSessionTimeoutSeconds, long oneTimeSessionTimeoutSeconds,
        long evalTimeoutSeconds, long evalTimeoutValidationLeeway, int sysOutCharLimit,
        long maxAliveSessions, int dockerMaxRamMegaBytes, double dockerCPUsUsage,
        @Nullable String dockerCPUSetCPUs, long schedulerSessionKillScanRateSeconds,
        long dockerResponseTimeout, long dockerConnectionTimeout, String jshellWrapperImageName) {

    public static final String JSHELL_WRAPPER_IMAGE_NAME_TAG = ":master";

    private static boolean checkJShellWrapperImageName(String imageName) {
        if (!StringUtils.hasText(imageName)
                || !imageName.endsWith(Config.JSHELL_WRAPPER_IMAGE_NAME_TAG)) {
            return false;
        }

        final String imageNameFirstPart = imageName.split(Config.JSHELL_WRAPPER_IMAGE_NAME_TAG)[0];

        return StringUtils.hasText(imageNameFirstPart);
    }

    public Config {
        if (regularSessionTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Invalid regularSessionTimeoutSeconds " + regularSessionTimeoutSeconds);
        if (oneTimeSessionTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Invalid oneTimeSessionTimeoutSeconds " + oneTimeSessionTimeoutSeconds);
        if (evalTimeoutSeconds <= 0)
            throw new IllegalArgumentException("Invalid evalTimeoutSeconds " + evalTimeoutSeconds);
        if (evalTimeoutValidationLeeway <= 0)
            throw new IllegalArgumentException("Invalid evalTimeoutValidationLeeway " + evalTimeoutSeconds);
        if (sysOutCharLimit <= 0)
            throw new IllegalArgumentException("Invalid sysOutCharLimit " + sysOutCharLimit);
        if (maxAliveSessions <= 0)
            throw new IllegalArgumentException("Invalid maxAliveSessions " + maxAliveSessions);
        if (dockerMaxRamMegaBytes <= 0)
            throw new IllegalArgumentException("Invalid dockerMaxRamMegaBytes " + dockerMaxRamMegaBytes);
        if (dockerCPUsUsage <= 0)
            throw new IllegalArgumentException("Invalid dockerCPUsUsage " + dockerCPUsUsage);
        if (dockerCPUSetCPUs != null && !dockerCPUSetCPUs.matches("[1-9]?\\d([-,]\\d?\\d)?"))
            throw new IllegalArgumentException("Invalid dockerCPUSetCPUs " + dockerCPUSetCPUs);
        if (schedulerSessionKillScanRateSeconds <= 0)
            throw new IllegalArgumentException(
                    "Invalid schedulerSessionKillScanRateSeconds " + schedulerSessionKillScanRateSeconds);
        if (dockerResponseTimeout <= 0)
            throw new IllegalArgumentException("Invalid dockerResponseTimeout " + dockerResponseTimeout);
        if (dockerConnectionTimeout <= 0)
            throw new IllegalArgumentException("Invalid dockerConnectionTimeout " + dockerConnectionTimeout);

        if (!checkJShellWrapperImageName(jshellWrapperImageName)) {
            throw new IllegalArgumentException("Invalid jshellWrapperImageName " + jshellWrapperImageName);
        }
    }
}
