import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private final List<BackupJob> jobs = new ArrayList<>();
    private final JobTableModel tableModel = new JobTableModel();
    private final JTable table = new JTable(tableModel);
    private final JButton addJobBtn = new JButton("Add Backup Job");
    private final JTextArea logArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready");
    private final JComboBox<String> logSelector = new JComboBox<>();
    private final JButton clearLogBtn = new JButton("Clear Log");

    private static final DateTimeFormatter TABLE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter LOG_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainWindow() {
        super("Backup Job Manager");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 500);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JTabbedPane tabs = new JTabbedPane();

        // Jobs tab
        table.setRowHeight(30);
        table.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        table.getColumn("Actions").setCellEditor(new ButtonEditor());
        tabs.addTab("Jobs", new JScrollPane(table));

        // Log tab
        JPanel logPanel = new JPanel(new BorderLayout(5,5));
        JPanel logTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 5,5));
        logTop.add(new JLabel("Job:"));
        logTop.add(logSelector);
        logTop.add(clearLogBtn);
        logPanel.add(logTop, BorderLayout.NORTH);
        logArea.setEditable(false);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        tabs.addTab("Log", logPanel);

        add(tabs, BorderLayout.CENTER);

        // Bottom panel
        JPanel bottom = new JPanel(new BorderLayout(5,5));
        bottom.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        bottom.add(statusLabel, BorderLayout.WEST);
        bottom.add(addJobBtn, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        addJobBtn.addActionListener(e -> new AddJobDialog(this).setVisible(true));
        logSelector.addActionListener(e -> {
            String jobName = (String) logSelector.getSelectedItem();
            if (jobName != null) updateLogView(jobName);
        });
        clearLogBtn.addActionListener(e -> {
            String jobName = (String) logSelector.getSelectedItem();
            if (jobName != null) {
                try {
                    Files.newBufferedWriter(getLogPath(jobName),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING
                    ).close();
                    logArea.setText("");
                    statusLabel.setText("Log cleared for " + jobName);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                        "Failed to clear log: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    void addJob(BackupJob job, boolean doNow, Integer interval) {
        jobs.add(job);
        int row = jobs.size() - 1;
        tableModel.fireTableRowsInserted(row, row);

        String jobName = job.getSource().getName();
        try {
            Path p = getLogPath(jobName);
            if (!Files.exists(p)) Files.createFile(p);
        } catch (IOException ignored) {}

        logSelector.addItem(jobName);
        logSelector.setSelectedItem(jobName);

        if (doNow) {
            job.backupNow();
            tableModel.fireTableCellUpdated(row, 2);
            appendToJobLog(jobName,
                String.format("%s Backed up to %s",
                    job.getLastBackup().format(LOG_FMT),
                    job.getBackupDir().getAbsolutePath()
                )
            );
            statusLabel.setText(String.format(
                "%s backed up to folder %s",
                jobName, job.getBackupDir().getAbsolutePath()
            ));
            updateLogView(jobName);
        }
        if (interval != null && interval > 0) {
            job.scheduleBackup(interval);
        }
    }

    private void updateLogView(String jobName) {
        SwingUtilities.invokeLater(() -> {
            try {
                Path p = getLogPath(jobName);
                if (Files.exists(p)) {
                    logArea.setText(String.join("\n", Files.readAllLines(p)));
                } else {
                    logArea.setText("No log for " + jobName);
                }
            } catch (IOException ex) {
                logArea.setText("Error loading log: " + ex.getMessage());
            }
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void appendToJobLog(String jobName, String line) {
        try {
            Path p = getLogPath(jobName);
            Files.write(p, (line + "\n").getBytes(),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
    }

    private Path getLogPath(String jobName) {
        BackupJob job = jobs.stream()
            .filter(j -> j.getSource().getName().equals(jobName))
            .findFirst().orElse(null);
        if (job == null) {
            return Paths.get(System.getProperty("user.home"), jobName + ".log");
        }
        return Paths.get(job.getSource().getParent(), jobName + ".log");
    }

    // ----- Table Model -----
    private class JobTableModel extends AbstractTableModel {
        private final String[] cols = { "File", "Backup Folder", "Last Backup", "Actions" };
        @Override public int getRowCount() { return jobs.size(); }
        @Override public int getColumnCount() { return cols.length; }
        @Override public String getColumnName(int c) { return cols[c]; }
        @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        @Override
        public Object getValueAt(int r, int c) {
            BackupJob job = jobs.get(r);
            switch (c) {
                case 0: return job.getSource().getAbsolutePath();
                case 1: return job.getBackupDir().getAbsolutePath();
                case 2:
                    return job.getLastBackup() == null
                        ? ""
                        : job.getLastBackup().format(TABLE_FMT);
                default: return "Actions";
            }
        }
    }

    // ----- Button Renderer -----
    private class ButtonRenderer extends JPanel implements TableCellRenderer {
        ButtonRenderer() { super(new FlowLayout(FlowLayout.LEFT,5,0)); }
        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object val,
                                                       boolean sel, boolean foc,
                                                       int row, int col) {
            removeAll();
            add(new JButton("Restore"));
            add(new JButton("Delete"));
            return this;
        }
    }

    // ----- Button Editor -----
    private class ButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        private final JButton restoreBtn = new JButton("Restore");
        private final JButton deleteBtn  = new JButton("Delete");
        private int currentRow;

        ButtonEditor() {
            panel.add(restoreBtn);
            panel.add(deleteBtn);

            restoreBtn.addActionListener(e -> {
                BackupJob job = jobs.get(currentRow);
                Path backupPath = job.getBackupDir().toPath()
                                     .resolve(job.getSource().getName());
                if (!Files.exists(backupPath)) {
                    int choice = JOptionPane.showOptionDialog(
                        MainWindow.this,
                        "Backup file is missing or corrupted.\nWhat would you like to do?",
                        "Restore Error",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        new String[]{ "Back up now", "Delete job" },
                        "Back up now"
                    );
                    if (choice == 0) {
                        job.backupNow();
                        tableModel.fireTableCellUpdated(currentRow, 2);
                    } else {
                        job.cancelSchedule();
                        jobs.remove(currentRow);
                        tableModel.fireTableRowsDeleted(currentRow, currentRow);
                    }
                } else {
                    LocalDateTime last = job.getLastBackup();
                    String timeStr = last == null ? "unknown" : last.format(LOG_FMT);
                    int confirm = JOptionPane.showConfirmDialog(
                        MainWindow.this,
                        String.format(
                            "The %s backed up at %s will overwrite the original file.\nContinue?",
                            job.getSource().getName(), timeStr
                        ),
                        "Confirm Restore",
                        JOptionPane.YES_NO_OPTION
                    );
                    if (confirm == JOptionPane.YES_OPTION) {
                        job.restoreNow();
                        appendToJobLog(job.getSource().getName(),
                            String.format("%s Restored to original location",
                                LocalDateTime.now().format(LOG_FMT))
                        );
                        statusLabel.setText(String.format(
                            "%s is restored to folder %s",
                            job.getSource().getName(),
                            job.getSource().getParent()
                        ));
                        updateLogView(job.getSource().getName());
                    }
                }
                fireEditingStopped();
            });

            deleteBtn.addActionListener(e -> {
                BackupJob job = jobs.remove(currentRow);
                job.cancelSchedule();
                tableModel.fireTableRowsDeleted(currentRow, currentRow);
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object val,
                                                     boolean sel, int row, int col) {
            currentRow = row;
            return panel;
        }
        @Override public Object getCellEditorValue() { return null; }
    }

    // ----- Add-Job Dialog -----
    private class AddJobDialog extends JDialog {
        private final CardLayout cards = new CardLayout();
        private final JPanel cardPanel = new JPanel(cards);
        private File chosenFile, chosenFolder;

        // Wizard fields:
        private JLabel fileLabel, sourceLabel, pathLabel;
        private JButton nextBtn, backupNowBtn, prevBtn;
        private JCheckBox autoChk;
        private JSpinner intervalSpinner;

        AddJobDialog(MainWindow parent) {
            super(parent, "Add Backup Job", true);
            setSize(500, 300);
            setLocationRelativeTo(parent);

            cardPanel.add(buildStep1(), "STEP1");
            cardPanel.add(buildStep2(), "STEP2");
            add(cardPanel);
            cards.show(cardPanel, "STEP1");
        }

        private JPanel buildStep1() {
            JPanel p = new JPanel(new BorderLayout());
            JPanel content = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            content.add(new JLabel("Step 1: Select file or directory to back up:"), gbc);

            gbc.gridy = 1; gbc.gridwidth = 2;
            fileLabel = new JLabel("⎯ none selected ⎯");
            content.add(fileLabel, gbc);

            gbc.gridy = 2; gbc.gridwidth = 2;
            JButton chooseBtn = new JButton("Choose File/Folder…");
            content.add(chooseBtn, gbc);

            nextBtn = new JButton("Next");
            nextBtn.setEnabled(false);
            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnBar.add(nextBtn);

            chooseBtn.addActionListener(evt -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    chosenFile = fc.getSelectedFile();
                    fileLabel.setText(chosenFile.getAbsolutePath());
                    nextBtn.setEnabled(true);
                }
            });
            nextBtn.addActionListener(evt -> {
                sourceLabel.setText(chosenFile.getName());
                cards.show(cardPanel, "STEP2");
            });

            p.add(content, BorderLayout.CENTER);
            p.add(btnBar, BorderLayout.SOUTH);
            return p;
        }

        private JPanel buildStep2() {
            JPanel p = new JPanel(new BorderLayout());
            JPanel content = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5,5,5,5);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
            content.add(new JLabel("File to back up:"), gbc);
            gbc.gridx = 1;
            sourceLabel = new JLabel("⎯ none selected ⎯");
            content.add(sourceLabel, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            content.add(new JLabel("Backup folder:"), gbc);
            gbc.gridx = 1;
            pathLabel = new JLabel("⎯ none selected ⎯");
            content.add(pathLabel, gbc);

            gbc.gridy = 2; gbc.gridwidth = 2;
            JButton chooseFld = new JButton("Choose Folder…");
            content.add(chooseFld, gbc);

            gbc.gridy = 3; gbc.gridwidth = 1; gbc.gridx = 0;
            autoChk = new JCheckBox("Enable auto backup (minutes)");
            content.add(autoChk, gbc);
            gbc.gridx = 1;
            intervalSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 1440, 1));
            intervalSpinner.setEnabled(false);
            content.add(intervalSpinner, gbc);

            chooseFld.addActionListener(evt -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    chosenFolder = fc.getSelectedFile();
                    pathLabel.setText(chosenFolder.getAbsolutePath());
                    backupNowBtn.setEnabled(true);
                }
            });
            autoChk.addActionListener(evt ->
                intervalSpinner.setEnabled(autoChk.isSelected())
            );

            prevBtn = new JButton("Previous");
            backupNowBtn = new JButton("Backup Now");
            backupNowBtn.setEnabled(false);
            JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.CENTER));
            btnBar.add(prevBtn);
            btnBar.add(backupNowBtn);

            prevBtn.addActionListener(evt -> cards.show(cardPanel, "STEP1"));
            backupNowBtn.addActionListener(evt -> {
                MainWindow.this.addJob(
                    new BackupJob(chosenFile, chosenFolder),
                    true,
                    autoChk.isSelected() ? (Integer) intervalSpinner.getValue() : null
                );
                dispose();
            });

            p.add(content, BorderLayout.CENTER);
            p.add(btnBar, BorderLayout.SOUTH);
            return p;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}