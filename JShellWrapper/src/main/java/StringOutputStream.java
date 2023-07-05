import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class StringOutputStream extends OutputStream {
    private final byte[] bytes;
    private int index;
    private boolean overflow;

    public StringOutputStream(int sizeLimit) {
        this.bytes = new byte[sizeLimit];
        this.index = 0;
        this.overflow = false;
    }

    @Override
    public void write(int b) throws IOException {
        if(index >= bytes.length) {
            overflow = true;
            return;
        }
        bytes[index++] = (byte)b;
    }

    public String readAll() {
        String s = new String(isOverflow() ? bytes : Arrays.copyOf(bytes, index));
        index = 0;
        return s;
    }

    public boolean isOverflow() {
        return overflow;
    }
}
