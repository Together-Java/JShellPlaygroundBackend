package org.togetherjava.jshellapi.dto;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;

public record ContainerState(boolean isCached, String containerId, BufferedReader containerOutput, BufferedWriter containerInput) {
}
