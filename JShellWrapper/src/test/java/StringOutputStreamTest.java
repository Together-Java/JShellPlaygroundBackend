import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.togetherjava.jshell.wrapper.StringOutputStream;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class StringOutputStreamTest {
    static final String E_ACUTE = "\u00E9";
    static final String SMILEY = "\uD83D\uDE0A";

    StringOutputStream stream;

    @BeforeEach
    void setUp() {
        stream = new StringOutputStream(10);
    }

    @AfterEach
    void tearDown() {
        stream.close();
    }

    @Test
    void testNoOverflow() {
        final String hello = "HelloWorld"; // length = 10
        for (byte b : hello.getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertResult(false, hello, stream.readAll());

        final String eAcuteX10 = E_ACUTE.repeat(10);
        for (byte b : eAcuteX10.getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertResult(false, eAcuteX10, stream.readAll());

        final String smileyX5 = SMILEY.repeat(5);
        for (byte b : smileyX5.getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertResult(false, smileyX5, stream.readAll());
    }

    @Test
    void testOverflow() {
        final String hello = "Hello World"; // length = 11
        for (byte b : hello.getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertResult(true, "Hello Worl", stream.readAll());

        final String eAcuteX11 = E_ACUTE.repeat(11);
        for (byte b : eAcuteX11.getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertResult(true, E_ACUTE.repeat(10), stream.readAll());
    }

    @Test
    void testOverflowWithHalfCharacter() {
        final String aAndSmileyX5 = 'a' + SMILEY.repeat(5);
        for (byte b : aAndSmileyX5.getBytes(StandardCharsets.UTF_8)) {
            stream.write(b);
        }
        assertResult(true, 'a' + SMILEY.repeat(4), stream.readAll());
    }

    @Test
    void testWriteOverload() {
        final String hello = "HelloWorld"; // length = 10
        stream.write(hello.getBytes(StandardCharsets.UTF_8));
        assertResult(false, hello, stream.readAll());

        final String eAcuteX15 = E_ACUTE.repeat(15);
        stream.write(eAcuteX15.getBytes(StandardCharsets.UTF_8), 0, 2 * 10);
        assertResult(false, E_ACUTE.repeat(10), stream.readAll());

        final String eAcuteX11 = E_ACUTE.repeat(11);
        stream.write(eAcuteX11.getBytes(StandardCharsets.UTF_8), 0, 2 * 11);
        assertResult(true, E_ACUTE.repeat(10), stream.readAll());

        final String eAcuteX5AndSmileyX5 = E_ACUTE.repeat(5) + SMILEY.repeat(5);
        stream.write(eAcuteX5AndSmileyX5.getBytes(StandardCharsets.UTF_8), 2 * 3,
                (2 * 2) + (4 * 4));
        assertResult(false, E_ACUTE.repeat(2) + SMILEY.repeat(4), stream.readAll());

        final String eAcuteX5AndSmileyX5AndA = E_ACUTE.repeat(5) + SMILEY.repeat(5) + 'a';
        stream.write(eAcuteX5AndSmileyX5AndA.getBytes(StandardCharsets.UTF_8), 2 * 3,
                (2 * 2) + (4 * 4) + 1);
        assertResult(true, E_ACUTE.repeat(2) + SMILEY.repeat(4), stream.readAll());
    }

    void assertResult(boolean isOverflow, String expected, StringOutputStream.Result result) {
        assertEquals(isOverflow, result.isOverflow());
        assertEquals(expected, result.content());
    }
}
