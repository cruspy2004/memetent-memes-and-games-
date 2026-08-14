import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * The T-Rex run window.
 *
 * The original centred the play field with {@code gbc.insets = new Insets(0, WIDTH / 6, 0,
 * 0)} — a hard-coded 200px left margin that is only correct at exactly one window size, and
 * which pushed the field off-centre the moment the frame was maximised (which it did
 * immediately, via {@code setExtendedState(MAXIMIZED_BOTH)}). It also gave the GIF a fixed
 * {@code WIDTH / 3} preferred size that GIFPanel then ignored.
 *
 * Both panes are now sized by {@link RatioSplit}, so the split is proportional at every
 * window size and the rail collapses out of the way when the window is narrow.
 */
public class UserInterface {

  static final String CARD_CHAD = "chad";
  static final String CARD_GONE = "gone";

  private static final int RAIL_BREAKPOINT = 880;

  private final JFrame mainWindow = new JFrame("Project Memetent — T-Rex Run");
  private final GamePanel gamePanel = new GamePanel();

  private Runnable onClose;

  UserInterface onClose(Runnable action) {
    this.onClose = action;
    return this;
  }

  public void createAndShowGUI() {
    final MemeRail rail = new MemeRail("HYPE METER");
    rail.addCard(CARD_CHAD, new GIFPanel("gigachad2.gif").withMode(GIFPanel.Mode.COVER));
    rail.addCard(CARD_GONE, disappointedCard());
    rail.show(CARD_CHAD, "Keep running.");

    gamePanel.onDeath(new Runnable() {
      public void run() { rail.show(CARD_GONE, "He left."); }
    });
    gamePanel.onRestart(new Runnable() {
      public void run() { rail.show(CARD_CHAD, "Keep running."); }
    });

    RatioSplit split = new RatioSplit(0.74, true, 0)
        .collapseBelow(RAIL_BREAKPOINT)
        .secondaryBounds(200, 400);

    JPanel root = new JPanel(split);
    root.setBackground(Theme.BG_DEEP);
    root.add(gamePanel, RatioSplit.PRIMARY);
    root.add(rail, RatioSplit.SECONDARY);

    mainWindow.setContentPane(root);
    mainWindow.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    mainWindow.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        gamePanel.shutdown();
        if (onClose != null) onClose.run();
      }
    });

    mainWindow.setMinimumSize(new Dimension(520, 420));
    mainWindow.pack();
    mainWindow.setLocationRelativeTo(null);
    mainWindow.setVisible(true);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() { gamePanel.requestFocusInWindow(); }
    });
  }

  /** Stand-in shown when the run ends; the rail keeps its width so nothing re-flows. */
  private JPanel disappointedCard() {
    JPanel panel = new JPanel(new java.awt.GridBagLayout());
    panel.setOpaque(false);
    JLabel label = new JLabel("💀", SwingConstants.CENTER);
    label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 72));
    label.setForeground(Theme.TEXT_MUTED);
    panel.add(label);
    return panel;
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        AppTheme.install();
        new UserInterface().createAndShowGUI();
      }
    });
  }
}
