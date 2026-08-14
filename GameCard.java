import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JPanel;
import javax.swing.Timer;

import utility.Assets;

/**
 * One selectable game on the picker screen.
 *
 * The old picker put a raw {@code JLabel(imageIcon)} at the image's native 406x600 into a
 * fixed 1000x700 GridLayout cell, so the artwork was cropped by the cell. Hovering swapped
 * in a {@code getScaledInstance(900, 900, ...)} copy — non-uniform, so the image distorted,
 * and larger than the cell, so it overflowed and could re-trigger enter/exit against itself.
 * The rescale ran on the EDT on every single mouse-enter, decoding a fresh bitmap each time.
 *
 * Here the image is fitted to the card with its proportions kept, hover is an animated
 * scale drawn from one cached bitmap, and the card sizes itself to whatever cell it gets.
 */
class GameCard extends JPanel {

  private static final long serialVersionUID = 1L;

  /** How much the artwork grows on hover. */
  private static final double HOVER_ZOOM = 0.06;

  private final Image art;
  private final int artW;
  private final int artH;
  private final String title;
  private final String blurb;
  private final Runnable onActivate;

  private boolean hovered;
  /** 0..1, eased toward the hover state so the lift animates instead of snapping. */
  private double hoverAmount;

  GameCard(String imagePath, String title, String blurb, Runnable onActivate) {
    javax.swing.ImageIcon icon = Assets.icon(imagePath);
    this.art = icon.getImage();
    this.artW = Math.max(1, icon.getIconWidth());
    this.artH = Math.max(1, icon.getIconHeight());
    this.title = title;
    this.blurb = blurb;
    this.onActivate = onActivate;

    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    setMinimumSize(new Dimension(240, 260));
    setPreferredSize(new Dimension(420, 520));

    final Timer animator = new Timer(16, null);
    animator.addActionListener(e -> {
      double target = hovered ? 1.0 : 0.0;
      hoverAmount += (target - hoverAmount) * 0.22;
      if (Math.abs(target - hoverAmount) < 0.005) {
        hoverAmount = target;
        animator.stop();
      }
      repaint();
    });

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        hovered = true;
        animator.start();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hovered = false;
        animator.start();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        onActivate.run();
      }
    });
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    Graphics2D g2 = (Graphics2D) g.create();
    Theme.enableQuality(g2);

    int pad = 14;
    int cardX = pad;
    int cardY = pad;
    int cardW = w - pad * 2;
    int cardH = h - pad * 2;
    int arc = 22;

    // Surface, lifting slightly as it is hovered.
    Color surface = blend(Theme.BG_PANEL, Theme.BG_RAISED, hoverAmount);
    g2.setColor(surface);
    g2.fill(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, arc, arc));

    // Artwork, clipped to the card so the hover zoom cannot spill outside it — the failure
    // the old 900x900 swap had.
    java.awt.Shape oldClip = g2.getClip();
    int textBand = (int) Math.max(72, cardH * 0.20);
    int artBoxH = cardH - textBand;
    g2.clip(new RoundRectangle2D.Float(cardX, cardY, cardW, artBoxH, arc, arc));

    double zoom = 1.0 + HOVER_ZOOM * hoverAmount;
    Dimension fitted = Theme.fit(artW, artH, (int) (cardW * zoom), (int) (artBoxH * zoom));
    g2.drawImage(art,
                 cardX + (cardW - fitted.width) / 2,
                 cardY + (artBoxH - fitted.height) / 2,
                 fitted.width, fitted.height, this);
    g2.setClip(oldClip);

    // Text band.
    int textY = cardY + artBoxH;
    g2.setColor(surface);
    g2.fillRect(cardX, textY, cardW, textBand);

    g2.setFont(Theme.scaledFont(Font.BOLD, 21f, cardW * 2));
    FontMetrics fm = g2.getFontMetrics();
    g2.setColor(Theme.TEXT);
    g2.drawString(title, cardX + 18, textY + fm.getAscent() + 12);

    g2.setFont(Theme.scaledFont(Font.PLAIN, 13f, cardW * 2));
    FontMetrics fm2 = g2.getFontMetrics();
    g2.setColor(Theme.TEXT_MUTED);
    g2.drawString(blurb, cardX + 18, textY + fm.getAscent() + fm2.getHeight() + 18);

    // Border, brightening toward the accent on hover.
    g2.setStroke(new java.awt.BasicStroke(1.5f + (float) hoverAmount));
    g2.setColor(blend(Theme.BORDER, Theme.ACCENT, hoverAmount));
    g2.draw(new RoundRectangle2D.Float(cardX, cardY, cardW, cardH, arc, arc));

    g2.dispose();
  }

  private static Color blend(Color a, Color b, double t) {
    t = Theme.clamp(t, 0.0, 1.0);
    return new Color(
        (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
        (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
        (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
  }
}
