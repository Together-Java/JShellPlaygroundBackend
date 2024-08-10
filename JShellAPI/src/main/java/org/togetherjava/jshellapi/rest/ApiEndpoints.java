package org.togetherjava.jshellapi.rest;

/**
 * This class holds endpoints mentioned in controllers. The main objective is to keep endpoints
 * synchronized with testing classes.
 *
 * @author Firas Regaieg
 */
public final class ApiEndpoints {
    private ApiEndpoints() {}

    public static final String BASE = "/jshell";
    public static final String EVALUATE = "/eval";
    public static final String SINGLE_EVALUATE = "/single-eval";
    public static final String SNIPPETS = "/snippets";
    public static final String STARTING_SCRIPT = "/startup_script";
}
