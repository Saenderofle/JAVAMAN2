import java.io.File;

/**
 * Клас для представлення задачі обробки зображення
 */
public class ImageTask {
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