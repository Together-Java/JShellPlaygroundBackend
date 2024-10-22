package org.togetherjava.jshellapi.dto;

import java.io.BufferedReader;
import java.io.BufferedWriter;

/**
 * Data record for the state of a container.
 * 
 * @param containerId The id of the container.
 * @param containerOutput The output of the container.
 * @param containerInput The input of the container.
 */
public record ContainerState(String containerId, BufferedReader containerOutput,
        BufferedWriter containerInput) {
}
