import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

// Клас для представлення задачі обробки зображення
class ImageTask {
    private final File sourceFile;
    private final int targetWidth;
    private final int targetHeight;
    private final String outputPrefix;

    public ImageTask(File sourceFile, int targetWidth, int targetHeight, String outputPrefix) {
        this.sourceFile = sourceFile;
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.outputPrefix = outputPrefix;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public String getOutputPrefix() {
        return outputPrefix;
    }
}

// Клас для зміни розміру зображень
class ImageResizer implements Callable<File> {
    private final ImageTask task;
    private final BlockingQueue<File> renameQueue;

    public ImageResizer(ImageTask task, BlockingQueue<File> renameQueue) {
        this.task = task;
        this.renameQueue = renameQueue;
    }

    @Override
    public File call() throws Exception {
        try {
            System.out.println("Обробка: " + task.getSourceFile().getName() +
                    " [Потік: " + Thread.currentThread().getName() + "]");

            BufferedImage originalImage = ImageIO.read(task.getSourceFile());

            // Створення зображення з новим розміром
            BufferedImage resizedImage = new BufferedImage(
                    task.getTargetWidth(),
                    task.getTargetHeight(),
                    BufferedImage.TYPE_INT_RGB
            );

            Graphics2D g = resizedImage.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(originalImage, 0, 0, task.getTargetWidth(),
                    task.getTargetHeight(), null);
            g.dispose();

            // Збереження тимчасового файлу
            String tempFileName = "temp_" + task.getSourceFile().getName();
            File outputFile = new File(task.getSourceFile().getParent(), tempFileName);

            String format = getImageFormat(task.getSourceFile().getName());
            ImageIO.write(resizedImage, format, outputFile);

            System.out.println("Зміна розміру завершена: " + outputFile.getName());

            // Додавання до черги перейменування
            renameQueue.put(outputFile);

            return outputFile;
        } catch (IOException e) {
            System.err.println("Помилка обробки " + task.getSourceFile().getName() +
                    ": " + e.getMessage());
            throw e;
        }
    }

    private String getImageFormat(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "jpg";
    }
}

// Клас для перейменування файлів
class ImageRenamer implements Runnable {
    private final BlockingQueue<File> renameQueue;
    private final String prefix;
    private final AtomicInteger counter;
    private volatile boolean running = true;

    public ImageRenamer(BlockingQueue<File> renameQueue, String prefix, AtomicInteger counter) {
        this.renameQueue = renameQueue;
        this.prefix = prefix;
        this.counter = counter;
    }

    @Override
    public void run() {
        System.out.println("Потік перейменування запущено [" + Thread.currentThread().getName() + "]");

        while (running || !renameQueue.isEmpty()) {
            try {
                File tempFile = renameQueue.poll(500, TimeUnit.MILLISECONDS);

                if (tempFile != null) {
                    int num = counter.incrementAndGet();
                    String extension = getFileExtension(tempFile.getName());
                    String newFileName = prefix + "_" + String.format("%04d", num) + extension;
                    File renamedFile = new File(tempFile.getParent(), newFileName);

                    if (tempFile.renameTo(renamedFile)) {
                        System.out.println("Перейменовано: " + tempFile.getName() +
                                " -> " + renamedFile.getName());
                    } else {
                        System.err.println("Не вдалося перейменувати: " + tempFile.getName());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Потік перейменування завершено");
    }

    public void stop() {
        running = false;
    }

    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return ".jpg";
    }
}

// Головний клас для управління обробкою зображень
class ImageCreator {
    private final ExecutorService resizeExecutor;
    private final Thread renameThread;
    private final BlockingQueue<File> renameQueue;
    private final ImageRenamer renamer;
    private final AtomicInteger processedCount;
    private final AtomicInteger fileCounter;

    public ImageCreator(int numResizeThreads, String outputPrefix) {
        this.resizeExecutor = Executors.newFixedThreadPool(numResizeThreads);
        this.renameQueue = new LinkedBlockingQueue<>();
        this.processedCount = new AtomicInteger(0);
        this.fileCounter = new AtomicInteger(0);

        this.renamer = new ImageRenamer(renameQueue, outputPrefix, fileCounter);
        this.renameThread = new Thread(renamer, "RenameThread");
        this.renameThread.start();
    }

    public void processImages(File[] imageFiles, int targetWidth, int targetHeight,
                              String outputPrefix) {
        System.out.println("Початок обробки " + imageFiles.length + " зображень...");

        CompletionService<File> completionService = new ExecutorCompletionService<>(resizeExecutor);
        int submittedTasks = 0;

        for (File file : imageFiles) {
            if (isImageFile(file)) {
                ImageTask task = new ImageTask(file, targetWidth, targetHeight, outputPrefix);
                completionService.submit(new ImageResizer(task, renameQueue));
                submittedTasks++;
            }
        }

        // Очікування завершення всіх задач
        for (int i = 0; i < submittedTasks; i++) {
            try {
                Future<File> future = completionService.take();
                future.get();
                processedCount.incrementAndGet();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Помилка виконання задачі: " + e.getMessage());
            }
        }

        System.out.println("Всі зображення оброблені. Очікування завершення перейменування...");
    }

    public void shutdown() {
        resizeExecutor.shutdown();

        try {
            if (!resizeExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                resizeExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            resizeExecutor.shutdownNow();
        }

        // Зупинка потоку перейменування
        renamer.stop();

        try {
            renameThread.join(5000);
        } catch (InterruptedException e) {
            System.err.println("Помилка очікування завершення потоку перейменування");
        }

        System.out.println("\n=== СТАТИСТИКА ===");
        System.out.println("Оброблено зображень: " + processedCount.get());
        System.out.println("Перейменовано файлів: " + fileCounter.get());
        System.out.println("Обробку завершено!");
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;

        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".gif") ||
                name.endsWith(".bmp");
    }

    public int getProcessedCount() {
        return processedCount.get();
    }
}

// GUI клас
class ImageCreatorGUI extends javax.swing.JFrame {
    private javax.swing.JButton btnSelectFiles;
    private javax.swing.JButton btnProcess;
    private javax.swing.JButton btnClear;
    private javax.swing.JSpinner spinnerWidth;
    private javax.swing.JSpinner spinnerHeight;
    private javax.swing.JSpinner spinnerThreads;
    private javax.swing.JTextField txtPrefix;
    private javax.swing.JTextArea txtLog;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JLabel lblSelectedFiles;
    private javax.swing.JProgressBar progressBar;

    private java.util.List<File> selectedFiles = new java.util.ArrayList<>();
    private ImageCreator creator;

    public ImageCreatorGUI() {
        initComponents();
        redirectSystemOut();
    }

    private void initComponents() {
        setTitle("ImageCreator - Багатопотоковий творець мініатюр");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setLayout(new java.awt.BorderLayout(10, 10));

        // Панель налаштувань
        javax.swing.JPanel settingsPanel = new javax.swing.JPanel();
        settingsPanel.setLayout(new java.awt.GridLayout(5, 2, 10, 10));
        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Налаштування"));

        settingsPanel.add(new javax.swing.JLabel("Ширина (px):"));
        spinnerWidth = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(200, 50, 2000, 10));
        settingsPanel.add(spinnerWidth);

        settingsPanel.add(new javax.swing.JLabel("Висота (px):"));
        spinnerHeight = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(200, 50, 2000, 10));
        settingsPanel.add(spinnerHeight);

        settingsPanel.add(new javax.swing.JLabel("Кількість потоків:"));
        spinnerThreads = new javax.swing.JSpinner(new javax.swing.SpinnerNumberModel(4, 1, 16, 1));
        settingsPanel.add(spinnerThreads);

        settingsPanel.add(new javax.swing.JLabel("Префікс файлів:"));
        txtPrefix = new javax.swing.JTextField("thumbnail");
        settingsPanel.add(txtPrefix);

        settingsPanel.add(new javax.swing.JLabel("Обрано файлів:"));
        lblSelectedFiles = new javax.swing.JLabel("0");
        lblSelectedFiles.setForeground(java.awt.Color.BLUE);
        settingsPanel.add(lblSelectedFiles);

        // Панель кнопок
        javax.swing.JPanel buttonPanel = new javax.swing.JPanel();
        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 10, 10));

        btnSelectFiles = new javax.swing.JButton("Обрати зображення");
        btnSelectFiles.addActionListener(e -> selectFiles());
        buttonPanel.add(btnSelectFiles);

        btnProcess = new javax.swing.JButton("Обробити");
        btnProcess.setEnabled(false);
        btnProcess.addActionListener(e -> processImages());
        buttonPanel.add(btnProcess);

        btnClear = new javax.swing.JButton("Очистити");
        btnClear.addActionListener(e -> clearLog());
        buttonPanel.add(btnClear);

        // Прогрес бар
        progressBar = new javax.swing.JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Готово до роботи");

        // Лог панель
        txtLog = new javax.swing.JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        scrollPane = new javax.swing.JScrollPane(txtLog);
        scrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Лог обробки"));

        // Додавання компонентів
        add(settingsPanel, java.awt.BorderLayout.NORTH);
        add(buttonPanel, java.awt.BorderLayout.CENTER);
        add(progressBar, java.awt.BorderLayout.SOUTH);
        add(scrollPane, java.awt.BorderLayout.EAST);

        scrollPane.setPreferredSize(new java.awt.Dimension(400, 300));
        setSize(800, 500);
        setLocationRelativeTo(null);
    }

    private void selectFiles() {
        javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);

        // Фільтр для зображень
        javax.swing.filechooser.FileNameExtensionFilter filter =
                new javax.swing.filechooser.FileNameExtensionFilter(
                        "Зображення (JPG, PNG, GIF, BMP)",
                        "jpg", "jpeg", "png", "gif", "bmp"
                );
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);

        if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            selectedFiles.clear();

            for (File file : files) {
                if (isImageFile(file)) {
                    selectedFiles.add(file);
                }
            }

            lblSelectedFiles.setText(String.valueOf(selectedFiles.size()));
            btnProcess.setEnabled(selectedFiles.size() > 0);

            txtLog.append("Обрано файлів: " + selectedFiles.size() + "\n");
            for (File file : selectedFiles) {
                txtLog.append("  - " + file.getName() + "\n");
            }
            txtLog.append("\n");
        }
    }

    private void processImages() {
        if (selectedFiles.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Спочатку оберіть зображення!",
                    "Помилка",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnProcess.setEnabled(false);
        btnSelectFiles.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("Обробка...");

        // Отримання параметрів
        int width = (Integer) spinnerWidth.getValue();
        int height = (Integer) spinnerHeight.getValue();
        int threads = (Integer) spinnerThreads.getValue();
        String prefix = txtPrefix.getText();

        // Запуск обробки в окремому потоці
        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                creator = new ImageCreator(threads, prefix);
                File[] filesArray = selectedFiles.toArray(new File[0]);

                creator.processImages(filesArray, width, height, prefix);
                creator.shutdown();

                long endTime = System.currentTimeMillis();
                double duration = (endTime - startTime) / 1000.0;

                final String message = "\n✓ Обробку завершено!\n" +
                        "Час виконання: " + String.format("%.2f", duration) + " сек\n" +
                        "Оброблено: " + creator.getProcessedCount() + " файлів\n\n";

                javax.swing.SwingUtilities.invokeLater(() -> {
                    txtLog.append(message);
                    progressBar.setValue(100);
                    progressBar.setString("Завершено");
                    btnProcess.setEnabled(true);
                    btnSelectFiles.setEnabled(true);

                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Обробку завершено!\nОброблено файлів: " + creator.getProcessedCount(),
                            "Успіх",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception e) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    txtLog.append("\n✗ ПОМИЛКА: " + e.getMessage() + "\n\n");
                    progressBar.setString("Помилка");
                    btnProcess.setEnabled(true);
                    btnSelectFiles.setEnabled(true);

                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Помилка обробки: " + e.getMessage(),
                            "Помилка",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void clearLog() {
        txtLog.setText("");
        progressBar.setValue(0);
        progressBar.setString("Готово до роботи");
    }

    private boolean isImageFile(File file) {
        if (!file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                name.endsWith(".png") || name.endsWith(".gif") ||
                name.endsWith(".bmp");
    }

    private void redirectSystemOut() {
        java.io.OutputStream out = new java.io.OutputStream() {
            @Override
            public void write(int b) throws java.io.IOException {
                updateLog(String.valueOf((char) b));
            }

            @Override
            public void write(byte[] b, int off, int len) throws java.io.IOException {
                updateLog(new String(b, off, len));
            }

            @Override
            public void write(byte[] b) throws java.io.IOException {
                write(b, 0, b.length);
            }
        };

        System.setOut(new java.io.PrintStream(out, true));
        System.setErr(new java.io.PrintStream(out, true));
    }

    private void updateLog(final String text) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            txtLog.append(text);
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
        });
    }
}

// Головний клас програми
public class Main {
    public static void main(String[] args) {
        // Встановлення Look and Feel
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Запуск GUI
        javax.swing.SwingUtilities.invokeLater(() -> {
            ImageCreatorGUI gui = new ImageCreatorGUI();
            gui.setVisible(true);
        });
    }
}