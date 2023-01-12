package org.togetherjava.jshellapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jshellapi")
public record Config(
        long regularSessionTimeoutSeconds,
        long oneTimeSessionTimeoutSeconds,
        long evalTimeoutSeconds) {
    public Config {
        if(regularSessionTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + regularSessionTimeoutSeconds);
        if(oneTimeSessionTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + oneTimeSessionTimeoutSeconds);
        if(evalTimeoutSeconds <= 0) throw new RuntimeException("Invalid value " + evalTimeoutSeconds);
    }
}
