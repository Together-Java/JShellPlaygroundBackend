package org.togetherjava.jshellapi.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
public sealed interface JShellEvalAbortionCause {

    @JsonTypeName("TIMEOUT")
    record TimeoutAbortionCause() implements JShellEvalAbortionCause {}

    @JsonTypeName("UNCAUGHT_EXCEPTION")
    record UnhandledExceptionAbortionCause(
        String exceptionClass,
        String exceptionMessage
    ) implements JShellEvalAbortionCause {}

    @JsonTypeName("COMPILE_TIME_ERROR")
    record CompileTimeErrorAbortionCause(List<String> errors) implements JShellEvalAbortionCause {}

    @JsonTypeName("SYNTAX_ERROR")
    record SyntaxErrorAbortionCause() implements JShellEvalAbortionCause {}
}
