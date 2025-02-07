

import javax.swing.*;
import java.awt.*;

public class GIFPanel extends JPanel {
    private ImageIcon gifIcon;

    public GIFPanel(String gifPath) {
        gifIcon = new ImageIcon(gifPath);
        JLabel gifLabel = new JLabel(gifIcon);
        setLayout(new BorderLayout());
        add(gifLabel, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(gifIcon.getIconWidth(), gifIcon.getIconHeight());
    }
}
