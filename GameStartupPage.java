import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * The game picker.
 *
 * The original dropped two native-size images into a fixed 1000x700 {@code GridLayout(1,2)}
 * with no scaling, so the artwork was simply cropped by its cell, and hover swapped in a
 * distorted 900x900 rescale that overflowed. It also showed a JOptionPane
 * ("Opening window for first image.") before every launch, and disposed itself for one game
 * but not the other.
 *
 * Now the cards scale to their cells, the grid re-flows to a single column on narrow
 * windows, and closing a game brings this picker back rather than killing the process.
 */
public class GameStartupPage {

  private static final String BRICK_BREAKER_IMAGE = "Brickbreacker_picture.png";
  private static final String DINO_IMAGE = "Dinogame_pic.png";

  /** Below this width the two cards stack vertically instead of sitting side by side. */
  private static final int STACK_BREAKPOINT = 760;

  private final JFrame frame = new JFrame("Project Memetent — Choose a game");
  private final JPanel grid = new JPanel(new GridLayout(1, 2, 0, 0));

  private Runnable onClose;

  public GameStartupPage() {
    JPanel root = new JPanel(new BorderLayout()) {
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
    root.setOpaque(true);
    root.setBackground(Theme.BG_DEEP);

    grid.setOpaque(false);
    grid.setBorder(BorderFactory.createEmptyBorder(4, 16, 24, 16));

    grid.add(new GameCard(BRICK_BREAKER_IMAGE, "Chippi Chappa Brick Breaker",
                          "Break every brick. Hampter is watching.",
                          new Runnable() { public void run() { launchBrickBreaker(); } }));

    grid.add(new GameCard(DINO_IMAGE, "T-Rex Run",
                          "Jump the cacti. Keep the chad happy.",
                          new Runnable() { public void run() { launchDino(); } }));

    root.add(header(), BorderLayout.NORTH);
    root.add(grid, BorderLayout.CENTER);

    // Re-flow the grid at the breakpoint. This is the picker's responsive behaviour.
    root.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        boolean stacked = e.getComponent().getWidth() < STACK_BREAKPOINT;
        GridLayout layout = (GridLayout) grid.getLayout();
        int wantRows = stacked ? 2 : 1;
        if (layout.getRows() != wantRows) {
          layout.setRows(wantRows);
          layout.setColumns(stacked ? 1 : 2);
          grid.revalidate();
        }
      }
    });

    frame.setContentPane(root);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        if (onClose != null) onClose.run();
      }
    });
    frame.setMinimumSize(new Dimension(420, 480));
    frame.setSize(1060, 720);
    frame.setLocationRelativeTo(null);
  }

  private JPanel header() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(26, 30, 6, 30));

    JLabel title = new JLabel("Choose your game");
    title.setFont(Theme.font(Font.BOLD, 27f));
    title.setForeground(Theme.TEXT);

    JLabel hint = new JLabel("Close a game window to come back here", SwingConstants.RIGHT);
    hint.setFont(Theme.font(Font.PLAIN, 13f));
    hint.setForeground(Theme.TEXT_MUTED);

    panel.add(title, BorderLayout.WEST);
    panel.add(hint, BorderLayout.EAST);
    return panel;
  }

  public GameStartupPage onClose(Runnable action) {
    this.onClose = action;
    return this;
  }

  public void show() {
    frame.setVisible(true);
  }

  /** Hides the picker while a game runs, and restores it when that game closes. */
  private void launch(Runnable opener) {
    frame.setVisible(false);
    opener.run();
  }

  private void launchBrickBreaker() {
    launch(new Runnable() {
      public void run() {
        new BrickBreakerWindow().onClose(new Runnable() {
          public void run() { frame.setVisible(true); }
        }).show();
      }
    });
  }

  private void launchDino() {
    launch(new Runnable() {
      public void run() {
        new UserInterface().onClose(new Runnable() {
          public void run() { frame.setVisible(true); }
        }).createAndShowGUI();
      }
    });
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        AppTheme.install();
        new GameStartupPage().show();
      }
    });
  }
}
