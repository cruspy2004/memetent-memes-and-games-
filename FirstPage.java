import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The landing screen.
 *
 * Two things were wrong. All three labels were drawn in {@code Color.BLACK} on a
 * {@code (54,59,71)} dark slate panel — around 1.4:1 contrast, i.e. invisible. And the hero
 * image was scaled exactly once, in {@code createImagePanel}, using {@code getWidth() / 2}
 * and {@code getHeight()} read off the frame before it had been laid out, then never
 * rescaled; resizing the window left a fixed-size bitmap in a stretched panel.
 *
 * Now the image scales continuously with its panel and the type is light-on-dark.
 */
public class FirstPage extends JFrame {

  /** Below this width the hero image is dropped and the copy takes the full window. */
  private static final int HERO_BREAKPOINT = 820;

  public FirstPage() {
    super("Project Memetent");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    RatioSplit split = new RatioSplit(0.52, true, 0)
        .collapseBelow(HERO_BREAKPOINT)
        .secondaryBounds(340, 640);

    JPanel root = new JPanel(split);
    root.setBackground(Theme.BG_DEEP);
    root.add(createHeroPanel(), RatioSplit.PRIMARY);
    root.add(createCopyPanel(), RatioSplit.SECONDARY);

    setContentPane(root);
    setMinimumSize(new Dimension(460, 420));
    setSize(1060, 620);
    setLocationRelativeTo(null);
  }

  /**
   * GIFPanel is reused here for a still PNG — it is really "an image scaled to its panel",
   * and it already does the cover-fit and quality hints this needs.
   */
  private JPanel createHeroPanel() {
    final GIFPanel hero = new GIFPanel("background.png").withMode(GIFPanel.Mode.COVER);

    // A scrim over the photo so it reads as a backdrop rather than competing with the copy.
    JPanel wrapper = new JPanel(new java.awt.BorderLayout()) {
      private static final long serialVersionUID = 1L;

      @Override
      protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(0, 0, 0, 40),
                                               getWidth(), getHeight(), new Color(0, 0, 0, 150)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
      }
    };
    wrapper.setBackground(Theme.BG_DEEP);
    wrapper.add(hero, java.awt.BorderLayout.CENTER);
    return wrapper;
  }

  private JPanel createCopyPanel() {
    JPanel panel = new JPanel() {
      private static final long serialVersionUID = 1L;

      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.enableQuality(g2);
        g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.BG_PANEL, 0, getHeight(), Theme.BG_DEEP));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
      }
    };
    panel.setOpaque(true);
    panel.setBackground(Theme.BG_PANEL);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(40, 44, 40, 44));

    JLabel eyebrow = label("A COMPILATION GAME", Theme.font(Font.BOLD, 13f), Theme.ACCENT);
    JLabel title = label("Project Memetent", Theme.font(Font.BOLD, 40f), Theme.TEXT);
    JLabel subtitle = label("Your favourite retro games, with memes.",
                            Theme.font(Font.PLAIN, 18f), Theme.TEXT_MUTED);

    AccentButton play = new AccentButton("Play Game");
    play.setAlignmentX(Component.CENTER_ALIGNMENT);
    play.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // The original popped a "Opening game..." JOptionPane here, an extra click between
        // the audience and the game for no information.
        openPicker();
      }
    });

    panel.add(Box.createVerticalGlue());
    panel.add(eyebrow);
    panel.add(Box.createRigidArea(new Dimension(0, 14)));
    panel.add(title);
    panel.add(Box.createRigidArea(new Dimension(0, 12)));
    panel.add(subtitle);
    panel.add(Box.createRigidArea(new Dimension(0, 36)));
    panel.add(play);
    panel.add(Box.createVerticalGlue());

    return panel;
  }

  private JLabel label(String text, Font font, Color colour) {
    JLabel l = new JLabel(text);
    l.setFont(font);
    l.setForeground(colour);
    l.setAlignmentX(Component.CENTER_ALIGNMENT);
    return l;
  }

  private void openPicker() {
    setVisible(false);
    new GameStartupPage().onClose(new Runnable() {
      public void run() { setVisible(true); }
    }).show();
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        AppTheme.install();
        new FirstPage().setVisible(true);
      }
    });
  }
}
