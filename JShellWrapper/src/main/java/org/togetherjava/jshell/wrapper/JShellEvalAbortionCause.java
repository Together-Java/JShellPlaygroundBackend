package org.togetherjava.jshell.wrapper;

import java.util.List;

public sealed interface JShellEvalAbortionCause {

    record TimeoutAbortionCause() implements JShellEvalAbortionCause {
    }

    record UnhandledExceptionAbortionCause(String exceptionClass,
            String exceptionMessage) implements JShellEvalAbortionCause {
    }

    record CompileTimeErrorAbortionCause(List<String> errors) implements JShellEvalAbortionCause {
    }

    record SyntaxErrorAbortionCause() implements JShellEvalAbortionCause {
    }
}
