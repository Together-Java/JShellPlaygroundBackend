import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

public class UnboundStringOutputStream extends OutputStream {
    private byte[] bytes;
    private int index;

    public UnboundStringOutputStream(int defaultCapacity) {
        this.bytes = new byte[defaultCapacity];
        this.index = 0;
    }

    public UnboundStringOutputStream() {
        this(16);
    }

    @Override
    public void write(int b) throws IOException {
        if(index == bytes.length) {
            bytes = Arrays.copyOf(bytes, bytes.length * 2);
        }
        bytes[index++] = (byte)b;
    }

    public String readAll() {
        String s = new String(Arrays.copyOf(bytes, index));
        index = 0;
        return s;
    }
}
