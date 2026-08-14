import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;

/**
 * One palette and one type scale shared by every screen, so the menu and both games read
 * as the same product.
 *
 * It also holds the two helpers the responsive layouts lean on: fonts that grow with the
 * panel they are drawn into, and aspect-preserving image fitting.
 */
final class Theme {

  // Surfaces, from the window background up to a hovered card.
  static final Color BG_DEEP = new Color(14, 16, 22);
  static final Color BG_PANEL = new Color(23, 27, 37);
  static final Color BG_RAISED = new Color(34, 40, 54);
  static final Color BORDER = new Color(56, 64, 82);

  // Ink. Both of these are chosen to clear WCAG AA against BG_PANEL.
  static final Color TEXT = new Color(238, 242, 248);
  static final Color TEXT_MUTED = new Color(154, 163, 182);

  // Accents.
  static final Color ACCENT = new Color(126, 166, 255);
  static final Color ACCENT_DIM = new Color(70, 99, 172);
  static final Color WARN = new Color(255, 186, 92);
  static final Color DANGER = new Color(255, 112, 112);
  static final Color SUCCESS = new Color(118, 224, 162);

  /** Width the type scale was designed against; scaledFont() is relative to this. */
  static final int REFERENCE_WIDTH = 1100;

  private static final String FAMILY = pickFamily();

  private Theme() {
  }

  /** First installed face from our preference list, falling back to the logical sans. */
  private static String pickFamily() {
    String[] preferred = { "Segoe UI", "Inter", "Helvetica Neue", "Roboto", "DejaVu Sans" };
    String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
    for (String want : preferred) {
      for (String have : installed) {
        if (have.equalsIgnoreCase(want)) return have;
      }
    }
    return Font.SANS_SERIF;
  }

  static Font font(int style, float size) {
    return new Font(FAMILY, style, Math.max(9, Math.round(size)));
  }

  /** Monospaced face for scores and other numerics, so digits don't jitter as they change. */
  static Font mono(int style, float size) {
    return new Font(Font.MONOSPACED, style, Math.max(9, Math.round(size)));
  }

  /**
   * Type that tracks the panel it is drawn into, so text keeps the same optical weight
   * whether the window is 800px or 2560px wide. Clamped at both ends so it stays legible
   * on a small laptop and doesn't turn into a billboard on a projector.
   */
  static Font scaledFont(int style, float baseSize, int panelWidth) {
    return font(style, baseSize * scaleFactor(panelWidth));
  }

  static Font scaledMono(int style, float baseSize, int panelWidth) {
    return mono(style, baseSize * scaleFactor(panelWidth));
  }

  static float scaleFactor(int panelWidth) {
    return clamp(panelWidth / (float) REFERENCE_WIDTH, 0.72f, 2.0f);
  }

  static float clamp(float v, float min, float max) {
    return Math.max(min, Math.min(max, v));
  }

  static int clamp(int v, int min, int max) {
    return Math.max(min, Math.min(max, v));
  }

  static double clamp(double v, double min, double max) {
    return Math.max(min, Math.min(max, v));
  }

  /**
   * Largest {@code srcW:srcH} box that fits inside {@code maxW x maxH} — "contain".
   * Used wherever a whole image has to stay visible.
   */
  static Dimension fit(int srcW, int srcH, int maxW, int maxH) {
    if (srcW <= 0 || srcH <= 0 || maxW <= 0 || maxH <= 0) return new Dimension(0, 0);
    double scale = Math.min(maxW / (double) srcW, maxH / (double) srcH);
    return new Dimension(Math.max(1, (int) Math.round(srcW * scale)),
                         Math.max(1, (int) Math.round(srcH * scale)));
  }

  /**
   * Smallest {@code srcW:srcH} box that covers {@code maxW x maxH} — "cover".
   * Used for backgrounds, where filling the panel matters more than showing every pixel.
   */
  static Dimension cover(int srcW, int srcH, int maxW, int maxH) {
    if (srcW <= 0 || srcH <= 0 || maxW <= 0 || maxH <= 0) return new Dimension(0, 0);
    double scale = Math.max(maxW / (double) srcW, maxH / (double) srcH);
    return new Dimension(Math.max(1, (int) Math.round(srcW * scale)),
                         Math.max(1, (int) Math.round(srcH * scale)));
  }

  static void enableQuality(Graphics2D g) {
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
  }

  /**
   * Draws {@code text} horizontally centred on {@code cx}. The old code centred by
   * subtracting guessed constants ("- 75", "- 150"), which drifted off-centre as soon as
   * the score changed width; this measures instead.
   */
  static void drawCentered(Graphics2D g, String text, int cx, int baselineY) {
    FontMetrics fm = g.getFontMetrics();
    g.drawString(text, cx - fm.stringWidth(text) / 2, baselineY);
  }

  /** A translucent scrim, so overlay text reads against whatever is behind it. */
  static void drawScrim(Graphics2D g, int w, int h, int alpha) {
    g.setColor(new Color(0, 0, 0, clamp(alpha, 0, 255)));
    g.fillRect(0, 0, w, h);
  }
}
