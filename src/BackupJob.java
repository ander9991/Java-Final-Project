import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.*;

public class BackupJob {
    private final File source;
    private final File backupDir;
    private LocalDateTime lastBackup;
    private ScheduledFuture<?> scheduledTask;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public BackupJob(File source, File backupDir) {
        this.source = source;
        this.backupDir = backupDir;
    }

    
    public File getSource() { 
    	return source; 
    }
    
    public File getBackupDir() { 
    	return backupDir; 
    }
    
    public LocalDateTime getLastBackup() {
    	return lastBackup;
    }

    
    public void backupNow() {
        try {
            Path targetRoot = backupDir.toPath().resolve(source.getName());
            if (source.isDirectory()) {
                BackupService.copyDirectory(source.toPath(), targetRoot);
            } else {
                Files.copy(source.toPath(), targetRoot, StandardCopyOption.REPLACE_EXISTING);
            }
            lastBackup = LocalDateTime.now();
            Logger.log("Backed up job: " + source.getAbsolutePath());
        } catch (IOException e) {
            Logger.log("Error in job backup: " + e.getMessage());
        }
    }


    public void scheduleBackup(int intervalMinutes) {
        scheduledTask = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                backupNow();
            }}, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }

    public void cancelSchedule() {
        if (scheduledTask != null) scheduledTask.cancel(true);
        scheduler.shutdownNow();
    }

    public void restoreNow() {
        try {
            Path src = backupDir.toPath().resolve(source.getName());
            if (Files.isDirectory(src)) {
                BackupService.copyDirectory(src, source.toPath());
            } else {
                Files.copy(src, source.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Logger.log("Restored job: " + source.getAbsolutePath());
        } catch (IOException e) {
            Logger.log("Error in job restore: " + e.getMessage());
        }
    }
}
