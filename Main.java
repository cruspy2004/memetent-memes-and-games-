import javax.swing.*;
import java.awt.*;

public class Main {
    JFrame frame;
    Gameplay gameplay;
    GIFPanel hamPanel;
    GIFPanel chPanel;

    public Main(GIFPanel hamPanel, GIFPanel chPanel) {
        frame = new JFrame();
        gameplay = new Gameplay();
        gameplay.setPreferredSize(new Dimension(700, 700)); // Set the preferred size of the gameplay panel
        gameplay.setTimerDelay(5);

        this.hamPanel = hamPanel;
        this.chPanel = chPanel;

        hamPanel.setPreferredSize(new Dimension(600, 400));
        gameplay.setHamPanel(hamPanel);

        chPanel.setPreferredSize(new Dimension(600, 400));
        gameplay.setChiPanel(chPanel);

        frame.setTitle("Chippi Chappa brick breaker");
        frame.setLayout(new BorderLayout(10, 10));

        // Create a main panel with BoxLayout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Add panels to the main panel
        mainPanel.add(gameplay);
        mainPanel.add(hamPanel);
        mainPanel.add(chPanel);

        // Add the main panel to the frame
        frame.add(mainPanel, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack(); // Adjusts frame size based on the preferred sizes of added components
        frame.setResizable(true);
    }

    public void showFrame() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Create instances of the GIFPanels
            String hamPath = "hamaster.gif"; // Replace with the actual path to your GIF
            GIFPanel hamPanel = new GIFPanel(hamPath);

            String chPath = "chippi.gif"; // Replace with the actual path to your GIF
            GIFPanel chPanel = new GIFPanel(chPath);

            // Pass the panels to the Main constructor
            Main main = new Main(hamPanel, chPanel);
            main.showFrame();
        });
    }
}
