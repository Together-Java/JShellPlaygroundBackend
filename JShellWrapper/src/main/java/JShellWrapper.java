import jdk.jshell.*;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Enter eval to evaluate code, snippets to see snippets, exit to stop.
 * How to use : at startup, one line need to be sent, startup script, first enter the command, for example eval or snippets, then any needed argument. Then "OK" should immediately be sent back, then after some time, the rest of the data.
 */
public class JShellWrapper {

    public void run() {
        Config config = Config.load();
        Scanner processIn = new Scanner(System.in);
        PrintStream processOut = System.out;
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

    private void verifyStartupEval(List<SnippetEvalResult> results) {
        for (SnippetEvalResult result : results) {
            if(!(result instanceof NormalEvalResult normalEvalResult)) {    // TODO Improve with java 21
                throw new RuntimeException("Timeout exceeded.");
            }
            SnippetEvent event = normalEvalResult.event();
            if(event.status() == Snippet.Status.REJECTED) {
                throw new RuntimeException("Following startup script was REJECTED : " + sanitize(event.snippet().source()));
            } else if(event.exception() != null) {
                throw new RuntimeException("Following startup script resulted in an exception : " + sanitize(event.snippet().source()), event.exception());
            }
        }
    }

    private void ok(PrintStream processOut) {
        processOut.println("OK");
        processOut.flush();
    }

    private List<SnippetEvalResult> eval(JShell shell, String code, AtomicBoolean hasStopped) {
        List<SnippetEvalResult> events = new ArrayList<>();
        do {
            var completion = shell.sourceCodeAnalysis().analyzeCompletion(clean(code));
            if(hasStopped.get()) {
                events.add(new AfterInterruptionEvalResult(completion.source()));
            } else {
                for(SnippetEvent event : shell.eval(completion.source())) {
                    if(event.causeSnippet() == null) {  // Only keep snippet creation events
                        events.add(new NormalEvalResult(event));
                    }
                }
            }
            code = completion.remaining();
        } while(!code.isEmpty());
        return events;
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
     * next number of snippets, see writeEvalSnippetEvent and writeAfterTimeout, writeAfterTimeout is only called after the first ABORTED<br>
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
        List<SnippetEvalResult> events = eval(shell, code, hasStopped);
        watcher.stop();

        List<String> outBuffer = writeEvalResult(watcher.isTimeout(), events, shell, jshellOut);
        for(String line : outBuffer) {
            processOut.println(line);
        }
    }

    private List<String> writeEvalResult(boolean isTimeout, List<SnippetEvalResult> events, JShell shell, StringOutputStream jshellOut) {
        List<String> outBuffer = new ArrayList<>();
        outBuffer.add(String.valueOf(events.size()));
        for (int i = 0; i < events.size(); i++) {
            SnippetEvalResult result = events.get(i);
            if(result instanceof NormalEvalResult normalEvalResult) {
                SnippetEvent event = normalEvalResult.event();
                boolean resultTimeout = (i < events.size() - 1 && events.get(i+1) instanceof AfterInterruptionEvalResult)
                        || (i == events.size() - 1 && isTimeout);
                writeEvalSnippetEvent(resultTimeout, outBuffer, event, shell);
            } else if(result instanceof AfterInterruptionEvalResult afterInterruptionEvalResult) {
                String source = afterInterruptionEvalResult.source();
                writeAfterTimeout(outBuffer, source);
            }
            /*  TODO replace with switch in java 21
            switch (result) {
                case NormalEvalResult(SnippetEvent event) -> {
                    boolean resultTimeout = (i < events.size() - 1 && events.get(i+1) instanceof AfterInterruptionEvalResult)
                            || (i == events.size() - 1 && isTimeout);
                    writeEvalSnippetEvent(resultTimeout, outBuffer, event, shell);
                }
                case AfterInterruptionEvalResult(String source) -> writeAfterTimeout(outBuffer, source);
            }*/
        }
        outBuffer.add(String.valueOf(jshellOut.isOverflow()));
        outBuffer.add(sanitize(jshellOut.readAll()));
        return outBuffer;
    }

    /**
     * Output format :<br>
     * <code>
     * status within VALID, RECOVERABLE_DEFINED, RECOVERABLE_NOT_DEFINED, REJECTED, ABORTED<br>
     * ADDITION/MODIFICATION<br>
     * snippet id<br>
     * source<br>
     * NONE or result<br>
     * NONE or ExceptionClass:Exception message<br>
     * number of errors, only if REJECTED<br>
     * error 1<br>
     * error 2<br>
     * etc<br>
     * </code>
     */
    private void writeEvalSnippetEvent(boolean aborted, List<String> outBuffer, SnippetEvent event, JShell shell) {
        String status = aborted ? "ABORTED" : switch (event.status()) {
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
        if(event.exception() == null) {
            outBuffer.add("");
        } else if(event.exception() instanceof EvalException evalException) {
            outBuffer.add(sanitize(evalException.getExceptionClassName() + ":" + evalException.getMessage()));
        } else {
            outBuffer.add(sanitize(event.exception().getClass().getName() + ":" + event.exception().getMessage()));
        }
        if(event.status() == Snippet.Status.REJECTED) {
            List<String> errors = shell.diagnostics(event.snippet()).map(d -> sanitize(d.getMessage(Locale.ENGLISH))).toList();
            outBuffer.add(String.valueOf(errors.size()));
            outBuffer.addAll(errors);
        }
    }

    /**
     * Output format :<br>
     * <code>
     * source
     * </code>
     */
    private void writeAfterTimeout(List<String> outBuffer, String source) {
        outBuffer.add(sanitize(source));
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
