import io.annovibe.vibe;

public class ByteUtils {

    @vibe("format raw bytes into human readable string like 1.4 MB or 512 KB")
    public static String formatBytes(long bytes) {
        return null; // Empty stub: synthesized at compile-time by AI
    }

    public static void main(String[] args) {
        System.out.println("512 bytes    -> " + formatBytes(512L));
        System.out.println("1,048,576 B  -> " + formatBytes(1048576L));
        System.out.println("1,572,864 B  -> " + formatBytes(1572864L));
    }
}
