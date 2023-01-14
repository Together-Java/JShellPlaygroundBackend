import jdk.jshell.JShell;
import jdk.jshell.Snippet;
import jdk.jshell.SnippetEvent;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Enter eval to evaluate code, snippets to see snippets, exit to stop.
 * How to use : first enter the command, for example eval or snippets, then any needed argument. Then "OK" should immediately be sent back, then after some time, the rest of the data.
 */
public class JShellWrapper {

    public void run() {
        Scanner scanner = new Scanner(System.in);
        Config config = Config.load();
        StringOutputStream out = new StringOutputStream(1024);
        try (JShell shell = JShell.builder().out(new PrintStream(out)).build()) {
            while(true) {
                String command = scanner.nextLine();
                System.out.println("OK");
                System.out.flush();
                switch (command) {
                    case "eval" -> eval(scanner, config, shell, out);
                    case "snippets" -> snippets(shell);
                    case "exit" -> {
                        return;
                    }
                    default -> {
                        throw new RuntimeException("No such command \"" + command + "\"");
                    }
                }
                System.out.flush();
            }
        }
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
     * is stdout overflow<br>
     * stdout<br>
     * error 1<br>
     * error 2<br>
     * etc<br>
     * empty line<br>
     * </code>
     */
    private void eval(Scanner scanner, Config config, JShell shell, StringOutputStream out) {
        TimeoutWatcher watcher = new TimeoutWatcher(config.evalTimeoutSeconds(), shell::stop);
        int lineCount = Integer.parseInt(scanner.nextLine());
        String code = IntStream.range(0, lineCount).mapToObj(i -> scanner.nextLine()).collect(Collectors.joining("\n"));

        watcher.start();
        List<SnippetEvent> events = shell.eval(code);
        watcher.stop();

        List<String> result = new ArrayList<>();
        for(SnippetEvent event : events) {
            if (event.causeSnippet() == null) {
                //  We have a snippet creation event
                String status = watcher.stopped() ? "ABORTED" : switch (event.status()) {
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
                result.add(String.valueOf(out.isOverflow()));
                result.add(sanitize(out.readAll()));
                if(event.status() == Snippet.Status.REJECTED) {
                    result.addAll(shell.diagnostics(event.snippet()).map(d -> sanitize(d.getMessage(Locale.ENGLISH))).toList());
                }
                result.add("");
            }
        }
        for(String line : result) {
            System.out.println(line);
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
    private void snippets(JShell shell) {
        shell.snippets().map(Snippet::source).map(JShellWrapper::sanitize).forEach(System.out::println);
        System.out.println();
    }

    private static String sanitize(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

}
