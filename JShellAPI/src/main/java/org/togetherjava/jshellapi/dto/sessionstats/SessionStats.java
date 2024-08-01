package org.togetherjava.jshellapi.dto.sessionstats;

/**
 * Represents the stats of a session.
 *
 * @param id the id of this session
 * @param timeSinceCreation the time in seconds since the creation of this session
 * @param timeUntilExpiration the time in seconds until the expiration of this session
 * @param totalEvalTime the time spent evaluating code
 * @param doingOperation if the session is currently evaluating some code
 */
public record SessionStats(
        String id,
        long timeSinceCreation,
        long timeUntilExpiration,
        long totalEvalTime,
        boolean doingOperation) {}
