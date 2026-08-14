package components;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

import utility.Assets;

/**
 * The scrolling ground line.
 *
 * {@code GROUND_Y} used to be computed once, in the constructor, from whatever height the
 * panel had at construction time. The panel was built before the frame was packed and then
 * maximised, so the ground ended up somewhere mid-screen while the score drew at the real
 * centre and the dino stood relative to a stale value — nothing on screen agreed with
 * anything else. It is now recomputed on every resize.
 *
 * Tiling was also a fixed list of three image copies shuffled by an iterator; it is now a
 * single scroll offset drawn across however much width the panel actually has, so the
 * ground reaches the right edge of any monitor instead of stopping after 3600px.
 */
public class Ground {

  /** Y of the ground line in panel pixels. Read by {@link Dino} and {@link Obstacles}. */
  public static int GROUND_Y;

  /** Ground sits this far down the panel, matching the original's 1 - 0.25. */
  private static final double GROUND_FRACTION = 0.75;

  /** Scroll speed in pixels per second, rather than the old per-frame constant. */
  private static final double SPEED = 500.0;

  private final BufferedImage image;

  private int panelWidth;
  private double offset;

  public Ground(int panelWidth, int panelHeight) {
    image = Assets.image("images/Ground.png");
    resize(panelWidth, panelHeight);
  }

  /** Called whenever the panel changes size, which is what keeps the ground aligned. */
  public void resize(int panelWidth, int panelHeight) {
    this.panelWidth = Math.max(1, panelWidth);
    GROUND_Y = (int) Math.round(panelHeight * GROUND_FRACTION);
  }

  public void update(double dt) {
    int w = Math.max(1, image.getWidth());
    offset = (offset + SPEED * dt) % w;
  }

  public void create(Graphics g) {
    int w = Math.max(1, image.getWidth());
    // Start one tile to the left so the scroll never exposes a gap at x=0.
    for (int x = (int) -offset; x < panelWidth; x += w) {
      g.drawImage(image, x, GROUND_Y, null);
    }
  }
}
