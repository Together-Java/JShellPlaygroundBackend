import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class StringOutputStream extends OutputStream {
    private static final char UNKNOWN_CHAR = '\uFFFD';
    private final int maxSize;
    private byte[] bytes;
    private int index;
    private boolean byteOverflow;

    /**
     * Constructs a new StringOutputStream.
     * @param maxSize the limit in terms of java char, so two bytes per unit
     */
    public StringOutputStream(int maxSize) {
        this.bytes = new byte[maxSize*2];
        this.maxSize = maxSize;
        this.index = 0;
        this.byteOverflow = false;
    }

    @Override
    public void write(int b) {
        if(index == bytes.length) {
            byteOverflow = true;
            return;
        }
        bytes[index++] = (byte)b;
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        Objects.checkFromIndexSize(off, len, b.length);
        if(index == bytes.length) {
            byteOverflow = true;
            return;
        }
        int actualLen = Math.min(bytes.length - index, len);
        System.arraycopy(b, off, bytes, index, actualLen);
        index += actualLen;
        if(len != actualLen) {
            byteOverflow = true;
        }
    }

    public Result readAll() {
        if(index > bytes.length) throw new IllegalStateException(); // Should never happen
        String s = new String(index == bytes.length ? bytes : Arrays.copyOf(bytes, index), StandardCharsets.UTF_8);
        index = 0;
        if(byteOverflow) {
            byteOverflow = false;
            return new Result(s.charAt(s.length()-1) == UNKNOWN_CHAR ? s.substring(0, s.length()-1) : s, true);
        }
        if(s.length() > maxSize) return new Result(s.substring(0, maxSize), true);
        return new Result(s, false);
    }

    @Override
    public void close() {
        bytes = null;
    }

    public record Result(String content, boolean isOverflow) {
    }
}
