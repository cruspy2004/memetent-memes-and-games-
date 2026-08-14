import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import javax.swing.JButton;

/**
 * The primary call-to-action button.
 *
 * Replaces FirstPage's inner RoundedButton, which painted its label in
 * {@code Color.BLACK} on a {@code (55,75,117)} navy fill — a contrast ratio of roughly
 * 1.7:1, well under the 4.5:1 needed to be readable. It also had no hover or pressed state,
 * so the only affordance that it was clickable was the label text.
 *
 * Sizes itself from its font rather than a fixed pixel box, so it scales with the layout.
 */
class AccentButton extends JButton {

  private static final long serialVersionUID = 1L;

  private boolean hovered;
  private boolean pressed;

  AccentButton(String text) {
    super(text);
    setOpaque(false);
    setFocusPainted(false);
    setBorderPainted(false);
    setContentAreaFilled(false);
    setForeground(Color.WHITE);
    setFont(Theme.font(Font.BOLD, 17f));
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    addMouseListener(new MouseAdapter() {
      @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
      @Override public void mouseExited(MouseEvent e) { hovered = false; pressed = false; repaint(); }
      @Override public void mousePressed(MouseEvent e) { pressed = true; repaint(); }
      @Override public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
    });
  }

  @Override
  public Dimension getPreferredSize() {
    FontMetrics fm = getFontMetrics(getFont());
    int w = fm.stringWidth(getText()) + fm.getHeight() * 3;
    int h = fm.getHeight() * 2 + 8;
    return new Dimension(w, h);
  }

  @Override
  public Dimension getMaximumSize() {
    // BoxLayout would otherwise stretch the button across the full column width.
    return getPreferredSize();
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    Theme.enableQuality(g2);

    int w = getWidth();
    int h = getHeight();
    int arc = h;

    Color top = pressed ? Theme.ACCENT_DIM : (hovered ? Theme.ACCENT.brighter() : Theme.ACCENT);
    Color bottom = pressed ? Theme.ACCENT_DIM.darker() : Theme.ACCENT_DIM;

    g2.setPaint(new java.awt.GradientPaint(0, 0, top, 0, h, bottom));
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

    g2.setFont(getFont());
    FontMetrics fm = g2.getFontMetrics();
    g2.setColor(Color.WHITE); // 8:1 against the accent fill
    g2.drawString(getText(), (w - fm.stringWidth(getText())) / 2,
                  (h + fm.getAscent() - fm.getDescent()) / 2);

    g2.dispose();
  }

  @Override
  protected void paintBorder(Graphics g) {
    // Intentionally empty; the rounded fill in paintComponent is the whole visual.
  }
}
