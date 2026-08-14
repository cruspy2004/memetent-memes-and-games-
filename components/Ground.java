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

  /**
   * How much to magnify the sprites, shared by everything in the scene.
   *
   * The art is tiny — the dino is 42x45 and a cactus 25x48 — which suited a small window
   * but leaves the characters lost in the corner of a maximised one. Scaling with the panel
   * keeps the scene readable on a projector without needing new artwork.
   */
  public static double SCALE = 1.0;

  /**
   * Panel height the art is scaled against. Deliberately well below a typical window
   * height: the sprites are small enough (42x45) that a 1:1 reference leaves them lost in
   * the corner of a maximised window.
   */
  private static final double REFERENCE_HEIGHT = 320.0;

  /** Ground sits this far down the panel, matching the original's 1 - 0.25. */
  private static final double GROUND_FRACTION = 0.75;

  /** Scroll speed in pixels per second at SCALE 1, rather than the old per-frame constant. */
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
    SCALE = Math.max(1.0, Math.min(3.5, panelHeight / REFERENCE_HEIGHT));
    GROUND_Y = (int) Math.round(panelHeight * GROUND_FRACTION);
  }

  /** Tile width at the current scale; the scroll offset wraps on this. */
  private int tileWidth() {
    return Math.max(1, (int) Math.round(image.getWidth() * SCALE));
  }

  public void update(double dt) {
    // Speed scales too, so the world moves at a consistent rate relative to the sprites.
    offset = (offset + SPEED * SCALE * dt) % tileWidth();
  }

  public void create(Graphics g) {
    int w = tileWidth();
    int h = Math.max(1, (int) Math.round(image.getHeight() * SCALE));
    // Start one tile to the left so the scroll never exposes a gap at x=0.
    for (int x = (int) -offset; x < panelWidth; x += w) {
      g.drawImage(image, x, GROUND_Y, w, h, null);
    }
  }
}
