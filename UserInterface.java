import javax.swing.*;
import java.awt.*;

public class UserInterface {
  JFrame mainWindow = new JFrame("T-Rex Run");

  public static int WIDTH = 1200;
  public static int HEIGHT = 800; // Adjusted for a more typical aspect ratio

  public void createAndShowGUI() {
    mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Get the content pane and set GridBagLayout
    Container contentPane = mainWindow.getContentPane();
    GridBagLayout layout = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    contentPane.setLayout(layout);

    // Create and add the GamePanel
    GamePanel gamePanel = new GamePanel(WIDTH * 2 / 3, HEIGHT); // Adjust width to 2/3 of the total width
    gamePanel.addKeyListener(gamePanel);
    gamePanel.setFocusable(true);

    // Create and add the GIFPanel
    String gifPath = "gigachad2.gif"; // Replace with the actual path to your GIF
    GIFPanel gifPanel = new GIFPanel(gifPath);
    gifPanel.setPreferredSize(new Dimension(WIDTH / 3, HEIGHT)); // Set the preferred size for the GIF panel
    gamePanel.setGIFPanel(gifPanel);

    // Center the gamePanel horizontally and vertically
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.insets = new Insets(0, (int)(WIDTH / 6), 0, 0); // Center the game panel by adding left margin
    contentPane.add(gamePanel, gbc);

    // Position the gifPanel to the right of the gamePanel
    gbc.gridx = 1;
    gbc.gridy = 0;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 0.0;
    gbc.weighty = 1.0;
    gbc.insets = new Insets(0, 0, 0, 0); // No additional margin needed
    contentPane.add(gifPanel, gbc);

    mainWindow.pack(); // Adjust the window size based on the components' preferred sizes
    mainWindow.setExtendedState(JFrame.MAXIMIZED_BOTH);
    mainWindow.setResizable(true);
    mainWindow.setVisible(true);
  }

  public static void main(String[] args) {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        new UserInterface().createAndShowGUI();
      }
    });
  }
}
