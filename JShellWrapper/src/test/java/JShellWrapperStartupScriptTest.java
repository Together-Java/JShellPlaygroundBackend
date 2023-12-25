import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;

class JShellWrapperStartupScriptTest {
    @Test
    void testDoubleSnippets() {
        Config config = new Config(5, 1024);
        StringInputStream inputStream = new StringInputStream("""
                import java.util.*; void println(Object o) { System.out.println(o); }
                eval
                1
                println(List.of("a", "b", "c"))
                exit""");
        UnboundStringOutputStream outputStream = new UnboundStringOutputStream();
        JShellWrapper jshell = new JShellWrapper();
        jshell.run(config, inputStream, new PrintStream(outputStream));
        assertEquals("""
                OK
                1
                VALID
                ADDITION
                3
                println(List.of("a", "b", "c"))
                
                
                false
                [a, b, c]\\n
                OK
                """, outputStream.readAll());
    }
}
