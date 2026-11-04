import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ByteUtilsTest {

    @Test
    void testFormatBytes() {
        assertEquals("512 B", ByteUtils.formatBytes(512L));
        assertEquals("1.0 MB", ByteUtils.formatBytes(1048576L));
        assertEquals("1.5 MB", ByteUtils.formatBytes(1572864L));
    }
}
