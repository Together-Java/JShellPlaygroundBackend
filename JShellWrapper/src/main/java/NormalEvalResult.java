import jdk.jshell.SnippetEvent;

public record NormalEvalResult(SnippetEvent event) implements SnippetEvalResult {
}
