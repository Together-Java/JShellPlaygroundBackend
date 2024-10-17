package org.togetherjava.jshellapi.dto;

import java.io.BufferedReader;
import java.io.BufferedWriter;

public record ContainerState(boolean isCached, String containerId, BufferedReader containerOutput,
        BufferedWriter containerInput) {
}
