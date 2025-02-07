import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GameStartupPage extends JFrame {

    public GameStartupPage() {
        setTitle("Game Startup Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2)); // Divide the frame into two equal parts
        getContentPane().setBackground(Color.BLACK); // Set the background of the content pane to black

        // Load images
        String[] imagePaths = {
                "C:\\Users\\haadh\\Downloads\\Project_memetent\\Brickbreacker_picture.png",
                "C:\\Users\\haadh\\Downloads\\Project_memetent\\Dinogame_pic.png"
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
        if (imagePath.equals("C:\\Users\\haadh\\Downloads\\Project_memetent\\Brickbreacker_picture.png")) {
            // Open window associated with the first image
            JOptionPane.showMessageDialog(this, "Opening window for first image.");
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    // Launch the Main class
                    String hamPath = "hamaster.gif"; // Replace with the actual path to your GIF
                    GIFPanel hamPanel = new GIFPanel(hamPath);

                    String chPath = "chippi.gif"; // Replace with the actual path to your GIF
                    GIFPanel chPanel = new GIFPanel(chPath);

                    Main main = new Main(hamPanel, chPanel);
                    main.showFrame();
                }
            });
            dispose(); // Close the current frame
        } else if (imagePath.equals("C:\\Users\\haadh\\Downloads\\Project_memetent\\Dinogame_pic.png")) {
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
