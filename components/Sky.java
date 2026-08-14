package components;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import utility.Assets;

/**
 * Sun and drifting clouds above the ground line.
 *
 * The play area is roughly three-quarters sky, and with nothing in it the game read as an
 * unfinished screen with a character stranded along the bottom edge. This fills that space
 * using {@code images/Sun.png}, which was already in the repo and never drawn by any code.
 *
 * Clouds are the same sprite drawn dim, large and soft — cheap, and it gives the sky depth
 * without new artwork. They scroll slower than the ground, so the parallax sells the motion.
 */
public class Sky {

  /** Clouds move at this fraction of the ground speed. */
  private static final double PARALLAX = 0.18;
  private static final double SPEED = 500.0;

  private static final class Cloud {
    double x;
    double y;      // fraction of the sky band
    double scale;  // relative to the sun sprite
    float alpha;
  }

  private final BufferedImage sun;
  private final Cloud[] clouds;

  private int panelWidth;
  private int panelHeight;

  public Sky(int panelWidth, int panelHeight) {
    sun = Assets.image("images/Sun.png");

    // Fixed arrangement rather than random, so the sky looks composed instead of noisy.
    double[][] spec = {
        { 0.24, 0.30, 1.30, 0.16f }, // clear of the sun, which sits at x 0.12
        { 0.33, 0.46, 0.90, 0.11f },
        { 0.55, 0.20, 1.55, 0.13f },
        { 0.80, 0.52, 1.05, 0.10f },
        { 1.16, 0.36, 1.20, 0.12f },
    };
    clouds = new Cloud[spec.length];
    for (int i = 0; i < spec.length; i++) {
      Cloud c = new Cloud();
      c.x = spec[i][0];
      c.y = spec[i][1];
      c.scale = spec[i][2];
      c.alpha = (float) spec[i][3];
      clouds[i] = c;
    }

    resize(panelWidth, panelHeight);
  }

  public void resize(int panelWidth, int panelHeight) {
    this.panelWidth = Math.max(1, panelWidth);
    this.panelHeight = Math.max(1, panelHeight);
  }

  public void update(double dt) {
    double shift = SPEED * Ground.SCALE * PARALLAX * dt / panelWidth;
    for (Cloud c : clouds) {
      c.x -= shift;
      if (c.x < -0.4) c.x += 1.8; // recycle off the right edge
    }
  }

  public void create(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

    int skyBand = Ground.GROUND_Y;

    // Sun, kept near its native resolution and parked left of the score readout. Upscaling
    // a 40px sprite much further just turns it into visible blocks.
    int sunSize = (int) Math.round(40 * Math.min(2.0, Ground.SCALE));
    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.45f));
    g2.drawImage(sun, (int) (panelWidth * 0.12), (int) (skyBand * 0.18), sunSize, sunSize, null);

    // Clouds are drawn rather than blitted. The first attempt stretched the sun sprite into
    // a cloud shape, which at 3x read as grey rectangles -- worse than an empty sky.
    for (Cloud c : clouds) {
      int w = (int) Math.round(34 * Ground.SCALE * c.scale);
      int x = (int) Math.round(c.x * panelWidth);
      int y = (int) Math.round(c.y * skyBand);
      drawCloud(g2, x, y, w, c.alpha);
    }

    g2.dispose();
  }

  /**
   * A small flat cloud: a rounded base with two bumps.
   *
   * Kept deliberately small and dim. Earlier attempts scaled it by the full sprite factor,
   * which produced blobs a third of the screen wide that crowded the score readout.
   */
  private void drawCloud(Graphics2D g2, int x, int y, int w, float alpha) {
    g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, alpha));
    g2.setColor(new java.awt.Color(226, 234, 250));

    int base = Math.max(4, (int) (w * 0.24)); // base slab height
    int lobe = Math.max(6, (int) (w * 0.30)); // bump diameter

    g2.fillRoundRect(x, y, w, base, base, base);
    g2.fillOval(x + (int) (w * 0.18), y - lobe / 2, lobe, lobe);
    g2.fillOval(x + (int) (w * 0.52), y - (int) (lobe * 0.66), (int) (lobe * 0.8), (int) (lobe * 0.8));
  }
}
