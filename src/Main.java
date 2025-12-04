import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Головний клас програми ImageCreator
 * Точка входу для запуску додатку
 */
public class Main {

    public static void main(String[] args) {
        // Встановлення UTF-8 кодування для консолі
        setUTF8Encoding();

        // Встановлення системного вигляду для Windows
        setSystemLookAndFeel();

        // Запуск GUI в Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("Starting ImageCreator...");
                System.out.println("Java version: " + System.getProperty("java.version"));
                System.out.println("OS: " + System.getProperty("os.name"));
                System.out.println("=" .repeat(50) + "\n");

                ImageCreatorGUI gui = new ImageCreatorGUI();
                gui.setVisible(true);

            } catch (Exception e) {
                System.err.println("ERROR starting application: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Встановлює системний Look and Feel для Windows
     */
    private static void setSystemLookAndFeel() {
        try {
            // Використовуємо системний стиль Windows
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Альтернативно, для Windows можна явно вказати:
            // UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");

        } catch (ClassNotFoundException | InstantiationException |
                 IllegalAccessException | UnsupportedLookAndFeelException e) {
            System.err.println("WARNING: Could not set system look and feel: " + e.getMessage());
            System.err.println("Using default Java style");
        }
    }

    /**
     * Встановлює UTF-8 кодування для правильного відображення тексту
     */
    private static void setUTF8Encoding() {
        try {
            // Встановлення UTF-8 для System.out і System.err
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
            System.setErr(new java.io.PrintStream(System.err, true, "UTF-8"));

            // Встановлення системного кодування
            System.setProperty("file.encoding", "UTF-8");
            System.setProperty("sun.jnu.encoding", "UTF-8");

        } catch (Exception e) {
            System.err.println("Could not set UTF-8 encoding: " + e.getMessage());
        }
    }
}