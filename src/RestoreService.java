import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

// RestoreService.java
class RestoreService {
    public static void restoreFiles(File backupDir) {
        JFileChooser chooser = new JFileChooser(backupDir);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int ret = chooser.showOpenDialog(null);
        if (ret == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) {
                try {
                    Path src = f.toPath();
                    Path dest = Paths.get(System.getProperty("user.home"))
                            .resolve(f.getName());
                    if (Files.isDirectory(src)) BackupService.copyDirectory(src, dest);
                    else Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    Logger.log("Restored: " + src);
                } catch (IOException ex) {
                    Logger.log("Error restoring " + f + ": " + ex.getMessage());
                }
            }
        }
    }
}