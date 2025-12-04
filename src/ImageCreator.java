import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Головний клас для управління обробкою зображень
 */
public class ImageCreator {
    private final ExecutorService resizeExecutor;
    private final Thread renameThread;
    private final BlockingQueue<File> renameQueue;
    private final ImageRenamer renamer;
    private final AtomicInteger processedCount;
    private final AtomicInteger fileCounter;
    private final File outputDirectory;

    public ImageCreator(int numResizeThreads, String outputPrefix, File outputDirectory) {
        this.resizeExecutor = Executors.newFixedThreadPool(numResizeThreads);
        this.renameQueue = new LinkedBlockingQueue<>();
        this.processedCount = new AtomicInteger(0);
        this.fileCounter = new AtomicInteger(0);
        this.outputDirectory = outputDirectory;

        this.renamer = new ImageRenamer(renameQueue, outputPrefix, fileCounter);
        this.renameThread = new Thread(renamer, "RenameThread");
        this.renameThread.start();
    }

    public void processImages(File[] imageFiles, int targetWidth, int targetHeight,
                              String outputPrefix) {
        System.out.println("Starting processing of " + imageFiles.length + " images...");
        System.out.println("Output directory: " + outputDirectory.getAbsolutePath());

        CompletionService<File> completionService = new ExecutorCompletionService<>(resizeExecutor);
        int submittedTasks = 0;

        for (File file : imageFiles) {
            if (isImageFile(file)) {
                ImageTask task = new ImageTask(file, targetWidth, targetHeight, outputPrefix);
                completionService.submit(new ImageResizer(task, renameQueue, outputDirectory));
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
                System.err.println("ERROR executing task: " + e.getMessage());
            }
        }

        System.out.println("All images processed. Waiting for rename completion...");
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
            System.err.println("WARNING: Error waiting for rename thread completion");
        }

        System.out.println("\n" + "=".repeat(40));
        System.out.println("STATISTICS");
        System.out.println("=".repeat(40));
        System.out.println("Images processed: " + processedCount.get());
        System.out.println("Files renamed: " + fileCounter.get());
        System.out.println("Saved to: " + outputDirectory.getAbsolutePath());
        System.out.println("=".repeat(40));
        System.out.println("Processing completed!");
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