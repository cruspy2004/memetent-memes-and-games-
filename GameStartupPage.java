import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameStartupPage extends JFrame {

    // Relative to the working directory, so the game runs from wherever it is checked out.
    private static final String BRICK_BREAKER_IMAGE = "Brickbreacker_picture.png";
    private static final String DINO_IMAGE = "Dinogame_pic.png";

    public GameStartupPage() {
        setTitle("Game Startup Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2)); // Divide the frame into two equal parts
        getContentPane().setBackground(Color.BLACK); // Set the background of the content pane to black

        // Load images
        String[] imagePaths = {
                BRICK_BREAKER_IMAGE,
                DINO_IMAGE
        };

        // Create panels for each image
        for (String path : imagePaths) {
            JPanel panel = createGamePanel(path);
            add(panel);
        }

        setVisible(true);
    }

    private JPanel createGamePanel(String imagePath) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.BLACK); // Set the background of the panel to black

        // Load the image
        ImageIcon imageIcon = new ImageIcon(imagePath);
        JLabel imageLabel = new JLabel(imageIcon);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setBackground(Color.BLACK); // Set the background of the label to black
        imageLabel.setOpaque(true);

        imageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Handle mouse entered event (e.g., zoom out the image)
                ImageIcon shrunkIcon = new ImageIcon(imageIcon.getImage().getScaledInstance(900, 900, Image.SCALE_SMOOTH));
                imageLabel.setIcon(shrunkIcon);
                panel.setComponentZOrder(imageLabel, 0); // Bring the hovered image to the front
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Handle mouse exited event (e.g., revert to original size)
                imageLabel.setIcon(imageIcon);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Show dialog when image is clicked
                openGameWindow(imagePath);
            }
        });

        panel.add(imageLabel, BorderLayout.CENTER);
        return panel;
    }

    private void openGameWindow(String imagePath) {
        // Logic to open different window based on the clicked image
        if (imagePath.equals(BRICK_BREAKER_IMAGE)) {
            // Open window associated with the first image
            JOptionPane.showMessageDialog(this, "Opening window for first image.");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    new BrickBreakerWindow().show();
                }
            });
            dispose(); // Close the current frame
        } else if (imagePath.equals(DINO_IMAGE)) {
            // Open window associated with the second image
            JOptionPane.showMessageDialog(this, "Opening window for second image.");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Create and show the UserInterface
                    new UserInterface().createAndShowGUI();
                }
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GameStartupPage();
            }
        });
    }
}
