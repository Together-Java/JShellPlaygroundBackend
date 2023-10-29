import jdk.jshell.*;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Enter eval to evaluate code, snippets to see snippets, exit to stop.
 * How to use : at startup, one line need to be sent, startup script, first enter the command, for example eval or snippets, then any needed argument. Then "OK" should immediately be sent back, then after some time, the rest of the data.
 */
public class JShellWrapper {

    public void run(Config config, InputStream in, PrintStream processOut) {
        Scanner processIn = new Scanner(in);
        String startup = desanitize(processIn.nextLine());
        StringOutputStream jshellOut = new StringOutputStream(1024);
        try (JShell shell = JShell.builder().out(new PrintStream(jshellOut)).build()) {
            verifyStartupEval(eval(shell, startup, new AtomicBoolean()));
            while(true) {
                String command = processIn.nextLine();
                switch (command) {
                    case "eval" -> eval(processIn, processOut, config, shell, jshellOut);
                    case "snippets" -> snippets(processOut, shell);
                    case "exit" -> {
                        ok(processOut);
                        return;
                    }
                    default -> throw new RuntimeException("No such command \"" + command + "\"");
                }
                processOut.flush();
            }
        }
    }

    private void verifyStartupEval(EvalResult result) {
        JShellEvalAbortion abortion = result.abortion();
        if (abortion == null) return;
        SnippetEvent event = result.events().get(result.events().size() - 1);
        // TODO Replace with switch
        if(abortion.cause() instanceof JShellEvalAbortionCause.TimeoutAbortionCause) {
            throw new RuntimeException("Timeout exceeded.");
        } else if(abortion.cause() instanceof JShellEvalAbortionCause.UnhandledExceptionAbortionCause) {
            throw new RuntimeException("Following startup script resulted in an exception : " + sanitize(abortion.sourceCause()), event.exception());
        } else if(abortion.cause() instanceof JShellEvalAbortionCause.CompileTimeErrorAbortionCause) {
            throw new RuntimeException("Following startup script was REJECTED : " + sanitize(abortion.sourceCause()));
        } else if(abortion.cause() instanceof JShellEvalAbortionCause.SyntaxErrorAbortionCause) {
            throw new RuntimeException("Following startup script has a syntax error : " + sanitize(abortion.sourceCause()));
        } else throw new AssertionError();
    }

    private void ok(PrintStream processOut) {
        processOut.println("OK");
        processOut.flush();
    }

    private EvalResult eval(JShell shell, String code, AtomicBoolean hasStopped) {
        List<SnippetEvent> resultEvents = new ArrayList<>();
        JShellEvalAbortion abortion = null;
        do {
            var completion = shell.sourceCodeAnalysis().analyzeCompletion(clean(code));
            if(completion.completeness() == SourceCodeAnalysis.Completeness.DEFINITELY_INCOMPLETE) {
                abortion = new JShellEvalAbortion(code, completion.remaining(), new JShellEvalAbortionCause.SyntaxErrorAbortionCause());
                break;
            }
            List<SnippetEvent> evalEvents = shell.eval(completion.source());
            if(hasStopped.get()) {
                abortion = new JShellEvalAbortion(completion.source(), completion.remaining(), new JShellEvalAbortionCause.TimeoutAbortionCause());
                break;
            }
            JShellEvalAbortionCause abortionCause = handleEvents(shell, evalEvents, resultEvents);
            if(abortionCause != null) {
                abortion = new JShellEvalAbortion(completion.source(), completion.remaining(), abortionCause);
                break;
            }
            code = completion.remaining();
        } while(!code.isEmpty());
        return new EvalResult(resultEvents, abortion);
    }
    private JShellEvalAbortionCause handleEvents(JShell shell, List<SnippetEvent> evalEvents, List<SnippetEvent> resultEvents) {
        for(SnippetEvent event : evalEvents) {
            if (event.causeSnippet() == null) {  // Only keep snippet creation events
                resultEvents.add(event);
                if(event.status() == Snippet.Status.REJECTED)  return createCompileErrorCause(shell, event);
                if(event.exception() != null) return createExceptionCause(event);
            }
        }
        return null;
    }
    private JShellEvalAbortionCause.UnhandledExceptionAbortionCause createExceptionCause(SnippetEvent event) {
        if(event.exception() == null) {
            return null;
        } else if(event.exception() instanceof EvalException evalException) {
            return new JShellEvalAbortionCause.UnhandledExceptionAbortionCause(evalException.getExceptionClassName(), evalException.getMessage());
        } else {
            return new JShellEvalAbortionCause.UnhandledExceptionAbortionCause(event.exception().getClass().getName(), event.exception().getMessage());
        }
    }
    private JShellEvalAbortionCause.CompileTimeErrorAbortionCause createCompileErrorCause(JShell shell, SnippetEvent event) {
        return new JShellEvalAbortionCause.CompileTimeErrorAbortionCause(shell.diagnostics(event.snippet()).map(d -> sanitize(d.getMessage(Locale.ENGLISH))).toList());
    }

    /**
     * Input format :<br>
     * <code><br>
     * eval<br>
     * line count<br>
     * code for each line count lines<br>
     * </code>
     * Output format :<br>
     * <code>
     * number of snippet results<br>
     * next number of snippets, see writeEvalSnippetEvent<br>
     * empty or abortion cause within TIMEOUT, UNCAUGHT_EXCEPTION, COMPILE_TIME_ERROR, SYNTAX_ERROR
     * if abortion cause is UNCAUGHT_EXCEPTION then ExceptionClass:Exception message, if abortion cause is COMPILE_TIME_ERROR, then number of errors, then for each number of errors lines, the errors, one per line
     * if abortion cause isn't empty, cause source code
     * if abortion cause isn't empty, remaining source code
     * is stdout overflow<br>
     * stdout<br>
     * </code>
     */
    private void eval(Scanner processIn, PrintStream processOut, Config config, JShell shell, StringOutputStream jshellOut) {
        AtomicBoolean hasStopped = new AtomicBoolean();
        TimeoutWatcher watcher = new TimeoutWatcher(config.evalTimeoutSeconds(), new JShellEvalStop(shell, hasStopped));
        int lineCount = Integer.parseInt(processIn.nextLine());
        String code = IntStream.range(0, lineCount).mapToObj(i -> processIn.nextLine()).collect(Collectors.joining("\n"));
        ok(processOut);

        watcher.start();
        EvalResult result = eval(shell, code, hasStopped);
        watcher.stop();

        List<String> outBuffer = writeEvalResult(watcher.isTimeout(), result, shell, jshellOut);
        for(String line : outBuffer) {
            processOut.println(line);
        }
    }

    private List<String> writeEvalResult(boolean isTimeout, EvalResult result, JShell shell, StringOutputStream jshellOut) {
        List<SnippetEvent> events = result.events();
        List<String> outBuffer = new ArrayList<>();
        outBuffer.add(String.valueOf(events.size()));
        for (int i = 0; i < events.size(); i++) {
            SnippetEvent event = events.get(i);
            writeEvalSnippetEvent(i == events.size() - 1 && isTimeout, outBuffer, event, shell);
        }
        JShellEvalAbortion abortion = result.abortion();
        if(abortion != null) {
            // TODO replace with switch
            if(abortion.cause() instanceof JShellEvalAbortionCause.TimeoutAbortionCause) {
                outBuffer.add("TIMEOUT");
            } else if(abortion.cause() instanceof JShellEvalAbortionCause.UnhandledExceptionAbortionCause c) {
                outBuffer.add("UNCAUGHT_EXCEPTION");
                outBuffer.add(getExceptionFromCause(c));
            } else if(abortion.cause() instanceof JShellEvalAbortionCause.CompileTimeErrorAbortionCause c) {
                outBuffer.add("COMPILE_TIME_ERROR");
                outBuffer.add(String.valueOf(c.errors().size()));
                outBuffer.addAll(c.errors());
            } else if(abortion.cause() instanceof JShellEvalAbortionCause.SyntaxErrorAbortionCause c) {
                outBuffer.add("SYNTAX_ERROR");
            } else throw new AssertionError();
            outBuffer.add(sanitize(abortion.sourceCause()));
            outBuffer.add(sanitize(abortion.remainingSource()));
        } else {
            outBuffer.add("");
        }

        outBuffer.add(String.valueOf(jshellOut.isOverflow()));
        outBuffer.add(sanitize(jshellOut.readAll()));
        return outBuffer;
    }

    /**
     * Output format :<br>
     * <code>
     * status within VALID, RECOVERABLE_DEFINED, RECOVERABLE_NOT_DEFINED, REJECTED<br>
     * ADDITION/MODIFICATION<br>
     * snippet id<br>
     * source<br>
     * NONE or result<br>
     * </code>
     */
    private void writeEvalSnippetEvent(boolean isTimeout, List<String> outBuffer, SnippetEvent event, JShell shell) {
        String status = switch (event.status()) {
            case VALID, RECOVERABLE_DEFINED, RECOVERABLE_NOT_DEFINED, REJECTED -> event.status().name();
            default -> throw new RuntimeException("Invalid status");
        };
        outBuffer.add(status);
        if (event.previousStatus() == Snippet.Status.NONEXISTENT) {
            outBuffer.add("ADDITION");
        } else {
            outBuffer.add("MODIFICATION");
        }
        outBuffer.add(event.snippet().id());
        outBuffer.add(sanitize(event.snippet().source()));
        outBuffer.add(event.value() != null ? event.value() : "NONE");
    }

    private String getExceptionFromCause(JShellEvalAbortionCause.UnhandledExceptionAbortionCause cause) {
        return sanitize(cause.exceptionClass() + ":" + cause.exceptionMessage());
    }

    /**
     * Input format : none<br>
     * Output format :<br>
     * <code>
     * Snippet 1<br>
     * empty line<br>
     * Snippet 2<br>
     * empty line<br>
     * etc<br>
     * empty line<br>
     * </code>
     */
    private void snippets(PrintStream processOut, JShell shell) {
        ok(processOut);
        shell.snippets().map(Snippet::source).map(JShellWrapper::sanitize).forEach(processOut::println);
        processOut.println();
    }

    private static String clean(String s) {
        return s.replace("\r", "");
    }
    private static String sanitize(String s) {
        return clean(s).replace("\\", "\\\\").replace("\n", "\\n");
    }
    private static String desanitize(String text) {
        return text.replace("\\n", "\n").replace("\\\\", "\\");
    }

}
