import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public class RestoreService {
    public static void restoreFiles(File backupDir) {
        JFileChooser chooser = new JFileChooser(backupDir);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                try {
                    Path src = f.toPath();
                    Path dest = Paths.get(System.getProperty("user.home"))
                                     .resolve(f.getName());
                    if (Files.isDirectory(src)) {
                        BackupService.copyDirectory(src, dest);
                    } else {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                    Logger.log("Restored: " + src.toString());
                } catch (IOException ex) {
                    Logger.log("Error restoring " + f.getAbsolutePath()
                               + ": " + ex.getMessage());
                }
            }
        }
    }
}