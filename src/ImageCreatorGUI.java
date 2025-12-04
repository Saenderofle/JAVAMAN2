import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
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

    private List<File> selectedFiles = new ArrayList<>();
    private File outputDirectory = null;
    private ImageCreator creator;

    public ImageCreatorGUI() {
        initComponents();
        redirectSystemOut();
    }

    private void initComponents() {
        setTitle("🎨 ImageCreator - Багатопотоковий творець мініатюр");
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

        // Панель кнопок
        JPanel buttonPanel = createButtonPanel();

        // Статус панель
        JPanel statusPanel = createStatusPanel();

        // Лог панель
        createLogPanel();

        // Компонування
        mainPanel.add(settingsPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.WEST);
        add(scrollPane, BorderLayout.CENTER);

        setSize(1100, 650);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private JPanel createSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(66, 133, 244), 2),
                        "⚙️ Налаштування",
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
        settingsPanel.add(createStyledLabel("📏 Ширина (px):"), gbc);
        gbc.gridx = 1;
        spinnerWidth = new JSpinner(new SpinnerNumberModel(200, 50, 2000, 10));
        styleSpinner(spinnerWidth);
        settingsPanel.add(spinnerWidth, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        settingsPanel.add(createStyledLabel("📐 Висота (px):"), gbc);
        gbc.gridx = 1;
        spinnerHeight = new JSpinner(new SpinnerNumberModel(200, 50, 2000, 10));
        styleSpinner(spinnerHeight);
        settingsPanel.add(spinnerHeight, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        settingsPanel.add(createStyledLabel("🧵 Кількість потоків:"), gbc);
        gbc.gridx = 1;
        spinnerThreads = new JSpinner(new SpinnerNumberModel(4, 1, 16, 1));
        styleSpinner(spinnerThreads);
        settingsPanel.add(spinnerThreads, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        settingsPanel.add(createStyledLabel("🏷️ Префікс файлів:"), gbc);
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
        filesPanel.add(createStyledLabel("📁 Обрано файлів:"));
        lblSelectedFiles = new JLabel("0");
        lblSelectedFiles.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSelectedFiles.setForeground(new Color(52, 168, 83));
        filesPanel.add(lblSelectedFiles);

        JPanel outputPanel = new JPanel(new BorderLayout(5, 0));
        outputPanel.setBackground(Color.WHITE);
        outputPanel.add(createStyledLabel("💾 Зберегти в:"), BorderLayout.WEST);
        txtOutputPath = new JTextField("Не обрано");
        txtOutputPath.setEditable(false);
        styleTextField(txtOutputPath);
        txtOutputPath.setBackground(new Color(245, 245, 245));
        outputPanel.add(txtOutputPath, BorderLayout.CENTER);

        infoPanel.add(filesPanel);
        infoPanel.add(outputPanel);

        return infoPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(new Color(240, 242, 245));

        btnSelectFiles = createStyledButton("📂 Обрати зображення", new Color(66, 133, 244));
        btnSelectFiles.addActionListener(e -> selectFiles());
        buttonPanel.add(btnSelectFiles);

        btnSelectOutput = createStyledButton("📁 Обрати папку", new Color(251, 188, 5));
        btnSelectOutput.addActionListener(e -> selectOutputDirectory());
        buttonPanel.add(btnSelectOutput);

        btnProcess = createStyledButton("🚀 Обробити", new Color(52, 168, 83));
        btnProcess.setEnabled(false);
        btnProcess.addActionListener(e -> processImages());
        buttonPanel.add(btnProcess);

        btnClear = createStyledButton("🗑️ Очистити", new Color(234, 67, 53));
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
                        "📋 Лог обробки",
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
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(180, 40));
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
            selectedFiles.clear();

            for (File file : files) {
                if (isImageFile(file)) {
                    selectedFiles.add(file);
                }
            }

            lblSelectedFiles.setText(String.valueOf(selectedFiles.size()));
            updateProcessButtonState();

            txtLog.append("📂 Обрано файлів: " + selectedFiles.size() + "\n");
            for (File file : selectedFiles) {
                txtLog.append("   • " + file.getName() + "\n");
            }
            txtLog.append("\n");
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

            txtLog.append("💾 Обрано папку для збереження:\n");
            txtLog.append("   " + outputDirectory.getAbsolutePath() + "\n\n");
        }
    }

    private void updateProcessButtonState() {
        btnProcess.setEnabled(selectedFiles.size() > 0 && outputDirectory != null);
    }

    private void processImages() {
        if (selectedFiles.isEmpty()) {
            showStyledMessage("⚠️ Спочатку оберіть зображення!", "Попередження",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (outputDirectory == null) {
            showStyledMessage("⚠️ Спочатку оберіть папку для збереження!", "Попередження",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnProcess.setEnabled(false);
        btnSelectFiles.setEnabled(false);
        btnSelectOutput.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
        lblStatus.setText("⚙️ Обробка...");
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
                        "🎉 Обробку завершено успішно!\n" +
                        "⏱️  Час виконання: " + String.format("%.2f", duration) + " сек\n" +
                        "✅ Оброблено: " + creator.getProcessedCount() + " файлів\n" +
                        "📁 Збережено в: " + outputDirectory.getName() + "\n" +
                        "=".repeat(50) + "\n\n";

                SwingUtilities.invokeLater(() -> {
                    txtLog.append(message);
                    progressBar.setValue(100);
                    progressBar.setString("100% ✓");
                    progressBar.setForeground(new Color(52, 168, 83));
                    lblStatus.setText("✅ Завершено успішно!");
                    lblStatus.setForeground(new Color(52, 168, 83));
                    btnProcess.setEnabled(true);
                    btnSelectFiles.setEnabled(true);
                    btnSelectOutput.setEnabled(true);

                    showStyledMessage(
                            "🎉 Обробку завершено!\n\n" +
                                    "✅ Оброблено файлів: " + creator.getProcessedCount() + "\n" +
                                    "⏱️ Час виконання: " + String.format("%.2f", duration) + " сек\n" +
                                    "📁 Збережено в: " + outputDirectory.getAbsolutePath(),
                            "Успіх",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    txtLog.append("\n❌ КРИТИЧНА ПОМИЛКА: " + e.getMessage() + "\n\n");
                    progressBar.setString("Помилка!");
                    progressBar.setForeground(new Color(234, 67, 53));
                    lblStatus.setText("❌ Помилка обробки");
                    lblStatus.setForeground(new Color(234, 67, 53));
                    btnProcess.setEnabled(true);
                    btnSelectFiles.setEnabled(true);
                    btnSelectOutput.setEnabled(true);

                    showStyledMessage(
                            "❌ Помилка обробки:\n\n" + e.getMessage(),
                            "Помилка",
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
        lblStatus.setText("⏸️ Готово до роботи");
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
        PrintStream printStream = new PrintStream(new java.io.OutputStream() {
            @Override
            public void write(int b) {
                txtLog.append(String.valueOf((char) b));
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            }
        });
        System.setOut(printStream);
        System.setErr(printStream);
    }
}