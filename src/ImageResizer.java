import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

/**
 * Клас для зміни розміру зображень
 */
public class ImageResizer implements Callable<File> {
    private final ImageTask task;
    private final BlockingQueue<File> renameQueue;
    private final File outputDirectory;

    public ImageResizer(ImageTask task, BlockingQueue<File> renameQueue, File outputDirectory) {
        this.task = task;
        this.renameQueue = renameQueue;
        this.outputDirectory = outputDirectory;
    }

    @Override
    public File call() throws Exception {
        try {
            System.out.println("🔄 Обробка: " + task.getSourceFile().getName() +
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
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(originalImage, 0, 0, task.getTargetWidth(),
                    task.getTargetHeight(), null);
            g.dispose();

            // Збереження тимчасового файлу у вибрану директорію
            String tempFileName = "temp_" + task.getSourceFile().getName();
            File outputFile = new File(outputDirectory, tempFileName);

            String format = getImageFormat(task.getSourceFile().getName());
            ImageIO.write(resizedImage, format, outputFile);

            System.out.println("✅ Зміна розміру завершена: " + outputFile.getName());

            // Додавання до черги перейменування
            renameQueue.put(outputFile);

            return outputFile;
        } catch (IOException e) {
            System.err.println("❌ Помилка обробки " + task.getSourceFile().getName() +
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