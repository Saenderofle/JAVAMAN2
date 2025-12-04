import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Клас для перейменування файлів
 */
public class ImageRenamer implements Runnable {
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
        System.out.println("Rename thread started [" + Thread.currentThread().getName() + "]");

        while (running || !renameQueue.isEmpty()) {
            try {
                File tempFile = renameQueue.poll(500, TimeUnit.MILLISECONDS);

                if (tempFile != null) {
                    int num = counter.incrementAndGet();
                    String extension = getFileExtension(tempFile.getName());
                    String newFileName = prefix + "_" + String.format("%04d", num) + extension;
                    File renamedFile = new File(tempFile.getParent(), newFileName);

                    if (tempFile.renameTo(renamedFile)) {
                        System.out.println("Renamed: " + tempFile.getName() +
                                " -> " + renamedFile.getName());
                    } else {
                        System.err.println("WARNING: Failed to rename: " + tempFile.getName());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("Rename thread finished");
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