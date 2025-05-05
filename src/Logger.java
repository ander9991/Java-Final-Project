import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Logger.java
class Logger {
    private static final Path logFile = Paths.get(System.getProperty("user.home"), "backup.log");
    public static synchronized void log(String msg) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = String.format("[%s] %s%n", time, msg);
        try {
            Files.write(logFile, line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }
}