import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final Path LOG_FILE = Paths.get(System.getProperty("user.home"), "backup.log");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String msg) {
        String timestamp = LocalDateTime.now().format(FMT);
        String line = String.format("[%s] %s%n", timestamp, msg);
        try {
            Files.write(LOG_FILE, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Logger failed: " + e.getMessage());
        }
    }
}