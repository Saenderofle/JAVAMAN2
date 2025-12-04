import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI клас для інтерфейсу програми
 */
public class ImageCreatorGUI extends JFrame {
    private JButton btnSelectFiles;
    private JButton btnSelectOutput;
    private JButton btnProcess;
    private JButton btnClear;
    private JSpinner spinnerWidth;
    private JSpinner spinnerHeight;
    private JSpinner spinnerThreads;
    private JTextField txtPrefix;
    private JTextField txtOutputPath;
    private JTextArea txtLog;
    private JScrollPane scrollPane;
    private JLabel lblSelectedFiles;
    private JLabel lblStatus;
    private JProgressBar progressBar;
    private JPanel dropZonePanel;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;

    private List<File> selectedFiles = new ArrayList<>();
    private File outputDirectory = null;
    private ImageCreator creator;

    public ImageCreatorGUI() {
        // Встановлення UTF-8 для компонентів
        try {
            System.setProperty("file.encoding", "UTF-8");
        } catch (Exception e) {
            // Ігноруємо помилки
        }

        initComponents();
        setupDragAndDrop();
        setupClipboardPaste();
        redirectSystemOut();
    }

    private void initComponents() {
        setTitle(" ImageCreator - Багатопотоковий творець мініатюр");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(15, 15));

        // Встановлюємо колірну схему
        getContentPane().setBackground(new Color(240, 242, 245));

        // Головна панель з відступами
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 242, 245));

        // Панель налаштувань
        JPanel settingsPanel = createSettingsPanel();

        // Панель Drag & Drop
        JPanel dropPanel = createDropZonePanel();

        // Панель кнопок
        JPanel buttonPanel = createButtonPanel();

        // Статус панель
        JPanel statusPanel = createStatusPanel();

        // Лог панель
        createLogPanel();

        // Ліва панель
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBackground(new Color(240, 242, 245));
        leftPanel.add(settingsPanel, BorderLayout.NORTH);
        leftPanel.add(dropPanel, BorderLayout.CENTER);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Компонування
        mainPanel.add(leftPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);

        setSize(1200, 700);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private JPanel createSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(66, 133, 244), 2),
                        " Налаштування",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        new Color(66, 133, 244)
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        settingsPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Розмір зображення
        gbc.gridx = 0; gbc.gridy = 0;
        settingsPanel.add(createStyledLabel(" Ширина (px):"), gbc);
        gbc.gridx = 1;
        spinnerWidth = new JSpinner(new SpinnerNumberModel(200, 50, 2000, 10));
        styleSpinner(spinnerWidth);
        settingsPanel.add(spinnerWidth, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        settingsPanel.add(createStyledLabel(" Висота (px):"), gbc);
        gbc.gridx = 1;
        spinnerHeight = new JSpinner(new SpinnerNumberModel(200, 50, 2000, 10));
        styleSpinner(spinnerHeight);
        settingsPanel.add(spinnerHeight, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        settingsPanel.add(createStyledLabel(" Кількість потоків:"), gbc);
        gbc.gridx = 1;
        spinnerThreads = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
        styleSpinner(spinnerThreads);
        settingsPanel.add(spinnerThreads, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        settingsPanel.add(createStyledLabel("🏷 Префікс файлів:"), gbc);
        gbc.gridx = 1;
        txtPrefix = new JTextField("thumbnail", 15);
        styleTextField(txtPrefix);
        settingsPanel.add(txtPrefix, gbc);

        // Інформаційна панель
        JPanel infoPanel = createInfoPanel();
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        settingsPanel.add(infoPanel, gbc);

        return settingsPanel;
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel filesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filesPanel.setBackground(Color.WHITE);
        filesPanel.add(createStyledLabel(" Обрано файлів:"));
        lblSelectedFiles = new JLabel("0");
        lblSelectedFiles.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSelectedFiles.setForeground(new Color(52, 168, 83));
        filesPanel.add(lblSelectedFiles);

        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputPanel.setBackground(Color.WHITE);
        outputPanel.add(createStyledLabel(" Зберегти в:"), BorderLayout.WEST);
        txtOutputPath = new JTextField("Не обрано");
        txtOutputPath.setEditable(false);
        styleTextField(txtOutputPath);
        txtOutputPath.setBackground(new Color(245, 245, 245));
        outputPanel.add(txtOutputPath, BorderLayout.CENTER);

        infoPanel.add(filesPanel);
        infoPanel.add(outputPanel);

        return infoPanel;
    }

    private JPanel createDropZonePanel() {
        JPanel dropPanel = new JPanel(new BorderLayout(10, 10));
        dropPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(52, 168, 83), 2),
                        "Drop Zone / File List",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14),
                        new Color(52, 168, 83)
                ),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        dropPanel.setBackground(Color.WHITE);

        // Drop zone area
        dropZonePanel = new JPanel(new BorderLayout());
        dropZonePanel.setBackground(new Color(245, 255, 245));
        dropZonePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(52, 168, 83), 2, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        dropZonePanel.setPreferredSize(new Dimension(350, 120));

        JLabel dropLabel = new JLabel("<html><center>" +
                "<b style='font-size: 14px;'>Drop Images Here</b><br><br>" +
                "<span style='color: gray;'>or use Ctrl+V to paste<br>" +
                "Supported: JPG, PNG, GIF, BMP</span>" +
                "</center></html>", SwingConstants.CENTER);
        dropLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dropZonePanel.add(dropLabel, BorderLayout.CENTER);

        // File list
        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        fileList.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane fileScrollPane = new JScrollPane(fileList);
        fileScrollPane.setPreferredSize(new Dimension(350, 150));

        // Clear selection button
        JButton btnClearSelection = new JButton("Clear Selection");
        btnClearSelection.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnClearSelection.addActionListener(e -> clearSelectedFiles());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(btnClearSelection);

        dropPanel.add(dropZonePanel, BorderLayout.NORTH);
        dropPanel.add(fileScrollPane, BorderLayout.CENTER);
        dropPanel.add(buttonPanel, BorderLayout.SOUTH);

        return dropPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(new Color(240, 242, 245));

        btnSelectFiles = createStyledButton(" Обрати зображення", new Color(66, 133, 244));
        btnSelectFiles.addActionListener(e -> selectFiles());
        buttonPanel.add(btnSelectFiles);

        btnSelectOutput = createStyledButton(" Обрати папку", new Color(251, 188, 5));
        btnSelectOutput.addActionListener(e -> selectOutputDirectory());
        buttonPanel.add(btnSelectOutput);

        btnProcess = createStyledButton(" Обробити", new Color(52, 168, 83));
        btnProcess.setEnabled(false);
        btnProcess.addActionListener(e -> processImages());
        buttonPanel.add(btnProcess);

        btnClear = createStyledButton("🗑 Очистити", new Color(234, 67, 53));
        btnClear.addActionListener(e -> clearLog());
        buttonPanel.add(btnClear);

        return buttonPanel;
    }

    private JPanel createStatusPanel() {
        JPanel statusPanel = new JPanel(new BorderLayout(10, 10));
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        lblStatus = new JLabel("⏸️ Готово до роботи");
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setForeground(new Color(100, 100, 100));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        progressBar.setPreferredSize(new Dimension(0, 25));
        progressBar.setForeground(new Color(66, 133, 244));

        statusPanel.add(lblStatus, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);

        return statusPanel;
    }

    private void createLogPanel() {
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtLog.setBackground(new Color(30, 30, 30));
        txtLog.setForeground(new Color(200, 200, 200));
        txtLog.setCaretColor(Color.WHITE);
        scrollPane = new JScrollPane(txtLog);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(100, 100, 100), 1),
                        " Лог обробки",
                        javax.swing.border.TitledBorder.LEFT,
                        javax.swing.border.TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 13),
                        new Color(100, 100, 100)
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return label;
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });

        return button;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinner.setPreferredSize(new Dimension(150, 30));
    }

    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textField.setPreferredSize(new Dimension(150, 30));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    private void setupDragAndDrop() {
        new DropTarget(dropZonePanel, new DropTargetListener() {
            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                dropZonePanel.setBackground(new Color(230, 255, 230));
                dropZonePanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(52, 168, 83), 3, true),
                        BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }

            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                // Можна додати додаткову логіку
            }

            @Override
            public void dropActionChanged(DropTargetDragEvent dtde) {
                // Можна додати додаткову логіку
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                dropZonePanel.setBackground(new Color(245, 255, 245));
                dropZonePanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(52, 168, 83), 2, true),
                        BorderFactory.createEmptyBorder(20, 20, 20, 20)
                ));
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = dtde.getTransferable();

                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        @SuppressWarnings("unchecked")
                        List<File> droppedFiles = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        addFiles(droppedFiles);
                        dtde.dropComplete(true);
                    } else {
                        dtde.dropComplete(false);
                    }
                } catch (Exception e) {
                    txtLog.append("ERROR: Could not process dropped files: " + e.getMessage() + "\n");
                    dtde.dropComplete(false);
                }

                dragExit(null);
            }
        });
    }

    private void setupClipboardPaste() {
        // Налаштування Ctrl+V для всього вікна
        KeyStroke pasteKeyStroke = KeyStroke.getKeyStroke("control V");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(pasteKeyStroke, "paste");
        getRootPane().getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                pasteFilesFromClipboard();
            }
        });
    }

    private void pasteFilesFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable contents = clipboard.getContents(null);

            if (contents != null && contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) contents.getTransferData(DataFlavor.javaFileListFlavor);
                addFiles(files);
            } else {
                txtLog.append("Clipboard does not contain files\n");
            }
        } catch (Exception e) {
            txtLog.append("ERROR: Could not paste from clipboard: " + e.getMessage() + "\n");
        }
    }

    private void addFiles(List<File> files) {
        int addedCount = 0;
        for (File file : files) {
            if (isImageFile(file) && !selectedFiles.contains(file)) {
                selectedFiles.add(file);
                fileListModel.addElement(file.getName());
                addedCount++;
            }
        }

        if (addedCount > 0) {
            lblSelectedFiles.setText(String.valueOf(selectedFiles.size()));
            updateProcessButtonState();
            txtLog.append("Added " + addedCount + " file(s)\n");
        }
    }

    private void clearSelectedFiles() {
        selectedFiles.clear();
        fileListModel.clear();
        lblSelectedFiles.setText("0");
        updateProcessButtonState();
        txtLog.append("Selection cleared\n");
    }

    private void selectFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setDialogTitle("Оберіть зображення для обробки");

        // Встановлення початкової директорії на робочий стіл користувача
        String userHome = System.getProperty("user.home");
        fileChooser.setCurrentDirectory(new File(userHome + "/Desktop"));

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Зображення (JPG, PNG, GIF, BMP)",
                "jpg", "jpeg", "png", "gif", "bmp"
        );
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            List<File> newFiles = new ArrayList<>();

            for (File file : files) {
                if (isImageFile(file)) {
                    newFiles.add(file);
                }
            }

            addFiles(newFiles);

            txtLog.append("Files selected via dialog: " + newFiles.size() + "\n");
        }
    }

    private void selectOutputDirectory() {
        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setDialogTitle("Оберіть папку для збереження результатів");
        dirChooser.setApproveButtonText("Обрати папку");
        dirChooser.setApproveButtonToolTipText("Зберегти оброблені зображення в цю папку");

        // Встановлення початкової директорії на робочий стіл користувача
        String userHome = System.getProperty("user.home");
        dirChooser.setCurrentDirectory(new File(userHome + "/Desktop"));

        int result = dirChooser.showDialog(this, "Обрати");

        if (result == JFileChooser.APPROVE_OPTION) {
            outputDirectory = dirChooser.getSelectedFile();
            txtOutputPath.setText(outputDirectory.getAbsolutePath());
            txtOutputPath.setToolTipText(outputDirectory.getAbsolutePath());
            updateProcessButtonState();

            txtLog.append("Output folder selected:\n");
            txtLog.append("   " + outputDirectory.getAbsolutePath() + "\n\n");
        }
    }

    private void updateProcessButtonState() {
        btnProcess.setEnabled(selectedFiles.size() > 0 && outputDirectory != null);
    }

    private void processImages() {
        if (selectedFiles.isEmpty()) {
            showStyledMessage("Please select images first!", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (outputDirectory == null) {
            showStyledMessage("Please select output folder first!", "Warning",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnProcess.setEnabled(false);
        btnSelectFiles.setEnabled(false);
        btnSelectOutput.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
        lblStatus.setText("Processing...");
        lblStatus.setForeground(new Color(251, 188, 5));

        int width = (Integer) spinnerWidth.getValue();
        int height = (Integer) spinnerHeight.getValue();
        int threads = (Integer) spinnerThreads.getValue();
        String prefix = txtPrefix.getText();

        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                creator = new ImageCreator(threads, prefix, outputDirectory);
                File[] filesArray = selectedFiles.toArray(new File[0]);

                // Симуляція прогресу
                Timer progressTimer = new Timer(100, evt -> {
                    int current = progressBar.getValue();
                    if (current < 95) {
                        progressBar.setValue(current + 1);
                        progressBar.setString(current + 1 + "%");
                    }
                });
                progressTimer.start();

                creator.processImages(filesArray, width, height, prefix);
                creator.shutdown();

                progressTimer.stop();

                long endTime = System.currentTimeMillis();
                double duration = (endTime - startTime) / 1000.0;

                final String message = "\n" + "=".repeat(50) + "\n" +
                        "Processing completed successfully!\n" +
                        "Execution time: " + String.format("%.2f", duration) + " sec\n" +
                        "Processed: " + creator.getProcessedCount() + " files\n" +
                        "Saved to: " + outputDirectory.getName() + "\n" +
                        "=".repeat(50) + "\n\n";

                SwingUtilities.invokeLater(() -> {
                    txtLog.append(message);
                    progressBar.setValue(100);
                    progressBar.setString("100%");
                    progressBar.setForeground(new Color(52, 168, 83));
                    lblStatus.setText("Completed successfully!");
                    lblStatus.setForeground(new Color(52, 168, 83));
                    btnProcess.setEnabled(true);
                    btnSelectFiles.setEnabled(true);
                    btnSelectOutput.setEnabled(true);

                    showStyledMessage(
                            "Processing completed!\n\n" +
                                    "Files processed: " + creator.getProcessedCount() + "\n" +
                                    "Execution time: " + String.format("%.2f", duration) + " sec\n" +
                                    "Saved to: " + outputDirectory.getAbsolutePath(),
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    txtLog.append("\nCRITICAL ERROR: " + e.getMessage() + "\n\n");
                    progressBar.setString("Error!");
                    progressBar.setForeground(new Color(234, 67, 53));
                    lblStatus.setText("Processing error");
                    lblStatus.setForeground(new Color(234, 67, 53));
                    btnProcess.setEnabled(true);
                    btnSelectFiles.setEnabled(true);
                    btnSelectOutput.setEnabled(true);

                    showStyledMessage(
                            "Processing error:\n\n" + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                });
            }
        }).start();
    }

    private void clearLog() {
        txtLog.setText("");
        progressBar.setValue(0);
        progressBar.setString("0%");
        progressBar.setForeground(new Color(66, 133, 244));
        lblStatus.setText("Ready");
        lblStatus.setForeground(new Color(100, 100, 100));
    }

    private void showStyledMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;

        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".gif") ||
                name.endsWith(".bmp");
    }

    private void redirectSystemOut() {
        try {
            PrintStream printStream = new PrintStream(new java.io.OutputStream() {
                private final StringBuilder buffer = new StringBuilder();

                @Override
                public void write(int b) {
                    buffer.append((char) b);
                    if (b == '\n') {
                        flush();
                    }
                }

                @Override
                public void flush() {
                    if (buffer.length() > 0) {
                        final String text = buffer.toString();
                        SwingUtilities.invokeLater(() -> {
                            txtLog.append(text);
                            txtLog.setCaretPosition(txtLog.getDocument().getLength());
                        });
                        buffer.setLength(0);
                    }
                }
            }, true, "UTF-8");

            System.setOut(printStream);
            System.setErr(printStream);
        } catch (Exception e) {
            System.err.println("Could not configure output redirection: " + e.getMessage());
        }
    }
}