import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * Shows an animated GIF scaled to the panel rather than at its native pixel size.
 *
 * The previous version wrapped the GIF in a JLabel and pinned
 * {@code getPreferredSize()} to the image's own dimensions. That override silently beat
 * every {@code setPreferredSize(...)} the layouts asked for, which is why the brick
 * breaker window packed to well over a thousand pixels tall — the meme GIFs were being
 * laid out at full resolution no matter what Main.java requested.
 *
 * Drawing the image directly (instead of via a JLabel icon) keeps the animation running:
 * {@code drawImage} with this panel as the ImageObserver gets a callback per frame, which
 * repaints us.
 */
public class GIFPanel extends JPanel {

  /** How the image is sized inside the panel. */
  enum Mode {
    /** Whole image visible, letterboxed if the proportions differ. */
    CONTAIN,
    /** Panel fully covered, overflow cropped. Right for backgrounds and side rails. */
    COVER
  }

  private final Image gif;
  private final int nativeW;
  private final int nativeH;

  private Mode mode = Mode.CONTAIN;

  public GIFPanel(String gifPath) {
    ImageIcon icon = Assets.icon(gifPath);
    gif = icon.getImage();
    nativeW = Math.max(1, icon.getIconWidth());
    nativeH = Math.max(1, icon.getIconHeight());

    setOpaque(true);
    setBackground(Theme.BG_PANEL);

    // A hint, not a mandate — layouts are free to override it, which is the whole point.
    setPreferredSize(defaultPreferredSize());
    setMinimumSize(new Dimension(80, 60));
  }

  /** A modestly sized box with the GIF's proportions, capped so huge source files behave. */
  private Dimension defaultPreferredSize() {
    return Theme.fit(nativeW, nativeH, 420, 360);
  }

  GIFPanel withMode(Mode mode) {
    this.mode = mode;
    repaint();
    return this;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    Graphics2D g2 = (Graphics2D) g.create();
    Theme.enableQuality(g2);

    Dimension d = mode == Mode.COVER
        ? Theme.cover(nativeW, nativeH, w, h)
        : Theme.fit(nativeW, nativeH, w, h);

    // COVER overflows the panel by design, so clip before drawing.
    if (mode == Mode.COVER) g2.setClip(new Rectangle(0, 0, w, h));

    g2.drawImage(gif, (w - d.width) / 2, (h - d.height) / 2, d.width, d.height, this);
    g2.dispose();
  }
}
