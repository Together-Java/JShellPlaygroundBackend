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
 * Format :
 * status
 * ADDITION/MODIFICATION
 * snippet id
 * source
 * result or NONE
 * is stdout overflow
 * stdout
 * error 1
 * error 2
 * etc
 * empty line
 */
public class JShellWrapper {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int timeout = scanner.nextInt();
        boolean renewable = scanner.nextLine().equals("true");

        StringOutputStream out = new StringOutputStream(1024);
        try (JShell shell = JShell.builder().out(new PrintStream(out)).build()) {
            while(true) {
                String command = scanner.nextLine();
                switch (command) {
                    case "eval" -> eval(scanner, shell, out);
                    case "snippets" -> snippets(shell);
                    default -> {
                        return;
                    }
                }
            }
        }
    }

    private static void eval(Scanner scanner, JShell shell, StringOutputStream out) {
        String input = scanner.nextLine();
        int lineCount = Integer.parseInt(input);
        String code = IntStream.range(0, lineCount).mapToObj(i -> scanner.nextLine()).collect(Collectors.joining("\n"));

        List<SnippetEvent> events = shell.eval(code);
        List<String> result = new ArrayList<>();
        for(SnippetEvent event : events) {
            if (event.causeSnippet() == null) {
                //  We have a snippet creation event
                String status = switch (event.status()) {
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
        System.out.flush();
    }

    private static void snippets(JShell shell) {
        shell.snippets().map(Snippet::source).map(JShellWrapper::sanitize).forEach(System.out::println);
        System.out.println();
        System.out.flush();
    }

    private static String sanitize(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

}
