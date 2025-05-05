import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

// BackupService.java
class BackupService {
    public static void backupFiles(List<File> sources, File destDir) {
        for (File src : sources) {
            try {
                Path target = destDir.toPath().resolve(src.getName());
                if (src.isDirectory()) {
                    copyDirectory(src.toPath(), target);
                } else {
                    Files.copy(src.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
                }
                Logger.log("Backed up: " + src.getAbsolutePath());
            } catch (IOException ex) {
                Logger.log("Error backing up " + src + ": " + ex.getMessage());
            }
        }
    }

    static void copyDirectory(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(path -> {
            try {
                Path rel = src.relativize(path);
                Path target = dest.resolve(rel);
                if (Files.isDirectory(path)) Files.createDirectories(target);
                else Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}