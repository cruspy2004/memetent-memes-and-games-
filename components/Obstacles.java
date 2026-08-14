package components;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import utility.Assets;

/**
 * The cactus field.
 *
 * Each obstacle used to store its {@code y} at construction, computed from whatever
 * {@code Ground.GROUND_Y} was at the time. Once the ground moved — which it does on every
 * resize — the cacti stayed at the old height, floating above or sunk below the line. The Y
 * is now derived from the ground at draw and collide time, so they follow it for free.
 *
 * Spacing and speed are also derived from the panel width rather than the fixed
 * {@code obstacleInterval = 200} / {@code movementSpeed = 11} pixel constants, so the game
 * plays the same on a laptop and a projector.
 */
public class Obstacles {

  private static class Obstacle {
    BufferedImage image;
    double x;
  }

  /** Scroll speed in px/s. Matches the old 11px-per-20ms frame. */
  private static final double SPEED = 550.0;

  private final List<BufferedImage> imageList = new ArrayList<BufferedImage>();
  private final List<Obstacle> obList = new ArrayList<Obstacle>();
  private final Random random = new Random();

  private int panelWidth;
  private double spacing;

  public Obstacles(int panelWidth) {
    imageList.add(Assets.image("images/Cactus-1.png"));
    imageList.add(Assets.image("images/Cactus-2.png"));
    imageList.add(Assets.image("images/Cactus-5.png"));
    imageList.add(Assets.image("images/Cactus-1.png"));
    imageList.add(Assets.image("images/Cactus-2.png"));

    resize(panelWidth);
    reset();
  }

  /** Spacing scales with the window so the run stays equally hard at any width. */
  public void resize(int panelWidth) {
    this.panelWidth = Math.max(1, panelWidth);
    this.spacing = Math.max(260, panelWidth * 0.42);
  }

  /** Rebuilds the field off-screen to the right. */
  public void reset() {
    obList.clear();
    double x = panelWidth * 1.2;
    for (BufferedImage bi : imageList) {
      Obstacle ob = new Obstacle();
      ob.image = bi;
      ob.x = x;
      obList.add(ob);
      x += spacing + random.nextInt(120);
    }
  }

  public void update(double dt) {
    // Scales with the scene, matching Ground's scroll so cacti track the ground texture.
    double shift = SPEED * Ground.SCALE * dt;
    for (Obstacle ob : obList) {
      ob.x -= shift;
    }

    // Recycle anything that has left the screen to the far right of the pack. The old
    // version mutated the list while an iterator was open over it, which is only safe
    // because it broke out immediately.
    double furthest = 0;
    for (Obstacle ob : obList) {
      furthest = Math.max(furthest, ob.x);
    }
    for (Obstacle ob : obList) {
      if (ob.x < -ob.image.getWidth() * Ground.SCALE) {
        ob.x = furthest + spacing + random.nextInt(140);
        ob.image = imageList.get(random.nextInt(imageList.size()));
      }
    }
  }

  public void create(Graphics g) {
    for (Obstacle ob : obList) {
      Rectangle box = boundsOf(ob);
      g.drawImage(ob.image, box.x, box.y, box.width, box.height, null);
    }
  }

  /** Position and size at the scene's current scale, derived from the live ground line. */
  private Rectangle boundsOf(Obstacle ob) {
    int w = Math.max(1, (int) Math.round(ob.image.getWidth() * Ground.SCALE));
    int h = Math.max(1, (int) Math.round(ob.image.getHeight() * Ground.SCALE));
    int y = Ground.GROUND_Y - h + (int) Math.round(5 * Ground.SCALE);
    return new Rectangle((int) Math.round(ob.x), y, w, h);
  }

  /**
   * Takes the dino's bounds rather than reaching for {@code Dino.getDino()} statically, so
   * the two classes are no longer coupled through static mutable state.
   */
  public boolean hasCollided(Rectangle dino) {
    for (Obstacle ob : obList) {
      if (dino.intersects(boundsOf(ob))) return true;
    }
    return false;
  }
}
