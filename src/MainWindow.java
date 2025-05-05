/*
 * BackupApp: A simple desktop Java backup application using Swing for GUI,
 * file I/O for backup/restore, multithreading for scheduling.
 */

//package com.backupapp;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;

public class MainWindow extends JFrame {
    private DefaultListModel<File> fileListModel = new DefaultListModel<>();
    private JList<File> fileList = new JList<>(fileListModel);
    private File backupFolder;
    private ScheduledExecutorService scheduler;

    public MainWindow() {
        super("Backup & Restore");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // File list
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        panel.add(new JScrollPane(fileList), BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new GridLayout(1, 5, 5, 5));
        buttons.add(createButton("Add Files", this::onAddFiles));
        buttons.add(createButton("Choose Backup Folder", this::onChooseBackupFolder));
        buttons.add(createButton("Backup Now", e -> onBackup()));
        buttons.add(createButton("Restore", e -> onRestore()));
        buttons.add(createButton("Schedule Auto (60m)", e -> onSchedule()));
        panel.add(buttons, BorderLayout.SOUTH);

        add(panel);
    }

    private JButton createButton(String text, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.addActionListener(listener);
        return btn;
    }

    private void onAddFiles(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            for (File f : chooser.getSelectedFiles()) fileListModel.addElement(f);
        }
    }

    private void onChooseBackupFolder(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            backupFolder = chooser.getSelectedFile();
            JOptionPane.showMessageDialog(this, "Backup folder set to: " + backupFolder);
        }
    }

    private void onBackup() {
        if (backupFolder == null) {
            JOptionPane.showMessageDialog(this, "Choose backup folder first.");
            return;
        }
        List<File> toBackup = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) toBackup.add(fileListModel.get(i));
        new Thread(() -> BackupService.backupFiles(toBackup, backupFolder)).start();
    }

    private void onRestore() {
        if (backupFolder == null) {
            JOptionPane.showMessageDialog(this, "Choose backup folder first.");
            return;
        }
        new Thread(() -> RestoreService.restoreFiles(backupFolder)).start();
    }

    private void onSchedule() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
            JOptionPane.showMessageDialog(this, "Auto backup stopped.");
        } else {
            if (backupFolder == null) {
                JOptionPane.showMessageDialog(this, "Choose backup folder first.");
                return;
            }
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(() -> BackupService.backupFiles(getAllFiles(), backupFolder),
                    0, 60, TimeUnit.MINUTES);
            JOptionPane.showMessageDialog(this, "Auto backup scheduled every 60 minutes.");
        }
    }

    private List<File> getAllFiles() {
        List<File> list = new ArrayList<>();
        for (int i = 0; i < fileListModel.size(); i++) list.add(fileListModel.get(i));
        return list;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
