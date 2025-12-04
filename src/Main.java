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
                System.out.println("🚀 Запуск ImageCreator...");
                System.out.println("📌 Версія Java: " + System.getProperty("java.version"));
                System.out.println("🖥️  ОС: " + System.getProperty("os.name"));
                System.out.println("=" .repeat(50) + "\n");

                ImageCreatorGUI gui = new ImageCreatorGUI();
                gui.setVisible(true);

            } catch (Exception e) {
                System.err.println("❌ Помилка запуску програми: " + e.getMessage());
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
            System.err.println("⚠️  Не вдалося встановити системний стиль: " + e.getMessage());
            System.err.println("Використовується стандартний стиль Java");
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
            System.err.println("Не вдалося встановити UTF-8 кодування: " + e.getMessage());
        }
    }
}