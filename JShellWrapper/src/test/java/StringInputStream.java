import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class StringInputStream extends InputStream {
    private final byte[] content;
    private int i;

    public StringInputStream(String content) {
        this.content = content.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int read() {
        if (i == content.length) return -1;
        return content[i++] & 0xFF;
    }
}
