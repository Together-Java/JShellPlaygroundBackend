package org.togetherjava.jshellapi.rest;

/**
 * Holds endpoints mentioned in controllers.
 */
public final class ApiEndpoints {
    private ApiEndpoints() {}

    public static final String BASE = "/jshell";
    public static final String EVALUATE = "/eval";
    public static final String SINGLE_EVALUATE = "/single-eval";
    public static final String SNIPPETS = "/snippets";
    public static final String STARTING_SCRIPT = "/startup_script";
}
