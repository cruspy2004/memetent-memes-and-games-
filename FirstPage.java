import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FirstPage extends JFrame {

    public FirstPage() {
        setTitle("Game Startup Page");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2)); // Use GridLayout for a seamless split

        // Load image
        String imagePath = "background.png";

        // Create left panel with image
        JPanel leftPanel = createImagePanel(imagePath);
        add(leftPanel);

        // Create right panel with button and labels
        JPanel rightPanel = createButtonPanel();
        add(rightPanel);

        setVisible(true);
    }

    private JPanel createImagePanel(String imagePath) {
        JPanel panel = new JPanel(new BorderLayout());

        // Load and scale the image to fit the panel
        ImageIcon imageIcon = new ImageIcon(imagePath);
        JLabel imageLabel = new JLabel();
        imageLabel.setIcon(new ImageIcon(imageIcon.getImage().getScaledInstance(getWidth() / 2, getHeight(), Image.SCALE_SMOOTH)));
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);

        panel.add(imageLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(54, 59, 71));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); // Use BoxLayout for vertical alignment

        // Add labels for text
        JLabel label1 = new JLabel("Project Memetent");
        label1.setFont(new Font("Arial", Font.BOLD, 30));
        label1.setAlignmentX(Component.CENTER_ALIGNMENT);
        label1.setForeground(Color.BLACK); // Set text color to black

        JLabel label2 = new JLabel("A compilation game.");
        label2.setFont(new Font("Arial", Font.PLAIN, 20));
        label2.setAlignmentX(Component.CENTER_ALIGNMENT);
        label2.setForeground(Color.BLACK); // Set text color to black

        JLabel label3 = new JLabel("Your favourite retro games with memes");
        label3.setFont(new Font("Arial", Font.PLAIN, 20));
        label3.setAlignmentX(Component.CENTER_ALIGNMENT);
        label3.setForeground(Color.BLACK); // Set text color to black

        // Custom Button
        JButton button = new RoundedButton("Play Game");
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Open the respective game
                JOptionPane.showMessageDialog(null, "Opening game...");
                new GameStartupPage();
            }
        });

        // Add components to the panel with spacing
        panel.add(Box.createVerticalGlue());
        panel.add(label1);
        panel.add(Box.createRigidArea(new Dimension(0, 20))); // Space between elements
        panel.add(label2);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(label3);
        panel.add(Box.createRigidArea(new Dimension(0, 40)));
        panel.add(button);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // Custom button class with rounded corners and bluish-silver color
    class RoundedButton extends JButton {
        private static final long serialVersionUID = 1L;

        public RoundedButton(String text) {
            super(text);
            setOpaque(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Set the color and fill the rounded rectangle
            g2.setColor(new Color(55, 75, 117)); // Light bluish-silver color
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);

            // Set the font color and draw the text
            g2.setColor(Color.BLACK);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();
            g2.drawString(getText(), x, y);

            g2.dispose();
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the border with the same color and roundness
            g2.setColor(new Color(173, 216, 230));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 30, 30);

            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FirstPage();
            }
        });
    }
}
