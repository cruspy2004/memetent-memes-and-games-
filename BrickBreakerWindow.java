import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * The brick breaker window.
 *
 * The original packed a 700x700 play field and two full-resolution meme GIFs into a
 * vertical BoxLayout, producing a frame well over 1500px tall that ran off the bottom of
 * most screens — and the GIFs re-flowed the whole column every time one was toggled.
 *
 * Here the field and the meme rail sit side by side at a fixed ratio (see {@link
 * RatioSplit}), the rail collapses out of the way on narrow windows, and the two GIFs
 * share a CardLayout so swapping between them never moves anything.
 */
class BrickBreakerWindow {

  static final String CARD_HAMSTER = "hamster";
  static final String CARD_CHIPPI = "chippi";

  /** Below this window width the meme rail is dropped and the field takes everything. */
  private static final int RAIL_BREAKPOINT = 900;

  private final JFrame frame = new JFrame("Project Memetent — Chippi Chappa Brick Breaker");
  private final Gameplay gameplay = new Gameplay();

  /** Invoked when this window closes, so the launcher can bring the menu back. */
  private Runnable onClose;

  BrickBreakerWindow() {
    MemeRail rail = new MemeRail("NOW PLAYING");
    rail.addCard(CARD_HAMSTER, new GIFPanel("hamaster.gif").withMode(GIFPanel.Mode.COVER));
    rail.addCard(CARD_CHIPPI, new GIFPanel("chippi.gif").withMode(GIFPanel.Mode.COVER));
    gameplay.setMemeRail(rail);

    // 72% field / 28% rail, rail never narrower than 200px nor wider than 420px.
    RatioSplit split = new RatioSplit(0.72, true, 0)
        .collapseBelow(RAIL_BREAKPOINT)
        .secondaryBounds(200, 420);

    JPanel root = new JPanel(split);
    root.setBackground(Theme.BG_DEEP);
    root.add(gameplay, RatioSplit.PRIMARY);
    root.add(rail, RatioSplit.SECONDARY);

    frame.setContentPane(root);
    // DISPOSE, not EXIT: closing the game should return to the picker, not kill the JVM.
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosed(WindowEvent e) {
        gameplay.shutdown(); // otherwise the MP3 keeps playing with no window to stop it
        if (onClose != null) onClose.run();
      }
    });

    frame.setMinimumSize(new Dimension(520, 460));
    frame.pack();
    frame.setLocationRelativeTo(null);
  }

  BrickBreakerWindow onClose(Runnable action) {
    this.onClose = action;
    return this;
  }

  void show() {
    frame.setVisible(true);
    // Focus has to land after the window is realised or the key bindings look dead.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() { gameplay.requestFocusInWindow(); }
    });
  }
}
