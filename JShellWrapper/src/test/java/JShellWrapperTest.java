import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.togetherjava.jshell.wrapper.Config;
import org.togetherjava.jshell.wrapper.JShellWrapper;

class JShellWrapperTest {
    static Config config;
    static JShellWrapper jshell;
    InputStream in;
    PrintStream out;

    @BeforeAll
    static void setUp() {
        config = new Config(5, 1024);
        jshell = new JShellWrapper();
    }

    void evalTest(String input, String expectedOutput) {
        UnboundStringOutputStream out = new UnboundStringOutputStream(128);
        jshell.run(config, new StringInputStream("\n" + input + "\nexit\n"), new PrintStream(out));
        assertEquals(expectedOutput + "\nOK\n", out.readAll());
    }

    @Test
    void testHelloWorld() {
        evalTest(
            """
                eval
                1
                System.out.println("Hello world!")""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                System.out.println("Hello world!")


                false
                Hello world!\\n"""
        );
    }

    @Test
    void testMultilinesInput() {
        evalTest(
            """
                eval
                4
                for(int i = 0; i < 10; i++) {
                    System.out.print(i);
                }
                System.out.println();""",
            """
                OK
                0
                OK
                2
                VALID
                ADDITION
                1
                for(int i = 0; i < 10; i++) {\\n    System.out.print(i);\\n}

                VALID
                ADDITION
                2
                \\nSystem.out.println();


                false
                0123456789\\n"""
        );
    }

    @Test
    void testStdoutOverflow() {
        evalTest(
            """
                eval
                1
                for(int i = 0; i < 1024; i++) System.out.print(0)""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                for(int i = 0; i < 1024; i++) System.out.print(0);


                false
                %s"""
                .formatted("0".repeat(1024))
        );
        evalTest(
            """
                eval
                1
                for(int i = 0; i <= 1024; i++) System.out.print(0)""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                for(int i = 0; i <= 1024; i++) System.out.print(0);


                true
                %s"""
                .formatted("0".repeat(1024))
        );
    }

    @Test
    void testModificationAndMultiplesSnippets() {
        evalTest(
            """
                eval
                2
                int i = 0;
                int i = 2;""",
            """
                OK
                0
                OK
                2
                VALID
                ADDITION
                1
                int i = 0;
                0
                VALID
                MODIFICATION
                1
                \\nint i = 2;
                2

                false
                """
        );
    }

    @Test
    void testUseId() {
        evalTest(
            """
                eval
                1
                System.out.println("Hello world!")""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                System.out.println("Hello world!")


                false
                Hello world!\\n"""
        );
    }

    @Test
    void testTimeout() {
        evalTest(
            """
                eval
                1
                while(true);""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                while(true);
                NONE
                TIMEOUT
                while(true);

                false
                """
        );
    }

    @Test
    void testUncaughtException() { // TODO other kind of exception, not in EvalException
        evalTest(
            """
                eval
                1
                throw new RuntimeException("Some message : fail")""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                throw new RuntimeException("Some message : fail");
                NONE
                UNCAUGHT_EXCEPTION
                java.lang.RuntimeException:Some message : fail
                throw new RuntimeException("Some message : fail");

                false
                """
        );
    }

    @Test
    void testRejected() {
        evalTest(
            """
                eval
                1
                print""",
            """
                OK
                0
                OK
                1
                REJECTED
                ADDITION
                1
                print
                NONE
                COMPILE_TIME_ERROR
                1
                cannot find symbol\\n  symbol:   variable print\\n  location: class\s
                print

                false
                """
        );
    }

    @Test
    void testSyntaxError() {
        // DEFINITELY_INCOMPLETE
        evalTest(
            """
                eval
                1
                print(""",
            """
                OK
                0
                OK
                0
                SYNTAX_ERROR
                print(

                false
                """
        );
        // CONSIDERED_INCOMPLETE
        evalTest(
            """
                eval
                1
                while(true)""",
            """
                OK
                0
                OK
                0
                SYNTAX_ERROR
                while(true)

                false
                """
        );
        evalTest(
            """
                eval
                1
                for(int i = 0; i < 10; i++)""",
            """
                OK
                0
                OK
                0
                SYNTAX_ERROR
                for(int i = 0; i < 10; i++)

                false
                """
        );
    }

    @Test
    void testRejectedAndMultiples() {
        evalTest(
            """
                eval
                3
                int i = 0;
                print;
                System.out.println(i);""",
            """
                OK
                0
                OK
                2
                VALID
                ADDITION
                1
                int i = 0;
                0
                REJECTED
                ADDITION
                2
                \\nprint;
                NONE
                COMPILE_TIME_ERROR
                1
                cannot find symbol\\n  symbol:   variable print\\n  location: class\s
                \\nprint;
                \\nSystem.out.println(i);
                false
                """
        );
    }

    @Test
    void testMultilinesAndHardcodedNewLineInString() {
        evalTest(
            """
                eval
                3
                {
                    System.out.println("\\n");
                }""",
            """
                OK
                0
                OK
                1
                VALID
                ADDITION
                1
                {\\n    System.out.println("\\\\n");\\n}


                false
                \\n\\n"""
        );
    }
}
