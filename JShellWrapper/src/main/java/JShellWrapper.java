import jdk.jshell.*;

import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Enter eval to evaluate code, snippets to see snippets, exit to stop.
 * How to use : at startup, two lines need to be sent, imports and startup script, each in one line, first enter the command, for example eval or snippets, then any needed argument. Then "OK" should immediately be sent back, then after some time, the rest of the data.
 */
public class JShellWrapper {

    public void run() {
        Config config = Config.load();
        Scanner processIn = new Scanner(System.in);
        PrintStream processOut = System.out;
        String imports = desanitize(processIn.nextLine());
        String startup = desanitize(processIn.nextLine());
        StringOutputStream jshellOut = new StringOutputStream(1024);
        try (JShell shell = JShell.builder().out(new PrintStream(jshellOut)).build()) {
            shell.eval(imports);
            shell.eval(startup);
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

    private void ok(PrintStream processOut) {
        processOut.println("OK");
        processOut.flush();
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
     * status within VALID, RECOVERABLE_DEFINED, RECOVERABLE_NOT_DEFINED, REJECTED, ABORTED<br>
     * ADDITION/MODIFICATION<br>
     * snippet id<br>
     * source<br>
     * result or NONE<br>
     * nothing or ExceptionClass:Exception message<br>
     * is stdout overflow<br>
     * stdout<br>
     * error 1<br>
     * error 2<br>
     * etc<br>
     * empty line<br>
     * </code>
     */
    private void eval(Scanner processIn, PrintStream processOut, Config config, JShell shell, StringOutputStream jshellOut) {
        TimeoutWatcher watcher = new TimeoutWatcher(config.evalTimeoutSeconds(), shell::stop);
        int lineCount = Integer.parseInt(processIn.nextLine());
        String code = IntStream.range(0, lineCount).mapToObj(i -> processIn.nextLine()).collect(Collectors.joining("\n"));
        ok(processOut);
        watcher.start();
        List<SnippetEvent> events = shell.eval(code);
        watcher.stop();

        List<String> result = new ArrayList<>();
        for(SnippetEvent event : events) {
            if (event.causeSnippet() == null) {
                //  We have a snippet creation event
                String status = watcher.isTimeout() ? "ABORTED" : switch (event.status()) {
                    case VALID, RECOVERABLE_DEFINED, RECOVERABLE_NOT_DEFINED, REJECTED -> event.status().name();
                    default -> throw new RuntimeException("Invalid status");
                };
                result.add(status);
                if (event.previousStatus() == Snippet.Status.NONEXISTENT) {
                    result.add("ADDITION");
                } else {
                    result.add("MODIFICATION");
                }
                result.add(event.snippet().id());
                result.add(sanitize(event.snippet().source()));
                result.add(event.value() != null ? event.value() : "NONE");
                if(event.exception() == null) {
                    result.add("");
                } else if(event.exception() instanceof EvalException evalException) {
                    result.add(sanitize(evalException.getExceptionClassName() + ":" + evalException.getMessage()));
                } else {
                    result.add(sanitize(event.exception().getClass().getName() + ":" + event.exception().getMessage()));
                }
                result.add(String.valueOf(jshellOut.isOverflow()));
                result.add(sanitize(jshellOut.readAll()));
                if(event.status() == Snippet.Status.REJECTED) {
                    result.addAll(shell.diagnostics(event.snippet()).map(d -> sanitize(d.getMessage(Locale.ENGLISH))).toList());
                }
                result.add("");
            }
        }
        for(String line : result) {
            processOut.println(line);
        }
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

    private static String sanitize(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }
    private static String desanitize(String text) {
        return text.replace("\\n", "\n").replace("\\\\", "\\");
    }

}
