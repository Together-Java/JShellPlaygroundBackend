package org.togetherjava.jshellapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Session may have been forcibly destroyed.")
public class DockerException extends Exception {
    public DockerException() {}

    public DockerException(String message) {
        super(message);
    }

    public DockerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerException(Throwable cause) {
        super(cause);
    }

    public DockerException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
