package components;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import utility.Assets;

/**
 * The player character.
 *
 * Rewritten around the live ground line rather than values captured at construction. The
 * old version cached {@code dinoTopY} and {@code topPoint} from {@code Ground.GROUND_Y} in
 * its constructor, so once the window was maximised the dino floated relative to a ground
 * that had moved.
 *
 * The jump is now a plain gravity arc. The old one was a hand-rolled state machine of three
 * overlapping {@code if}s inside a {@code case} that fell through into {@code case DIE}, and
 * {@code jump()} reset {@code dinoTop} to the ground on every call — so pressing space
 * mid-air teleported the dino down to the ground and restarted the hop.
 *
 * State is per-instance; it used to be {@code static}, which meant a second game in the same
 * JVM inherited the previous run's pose.
 */
public class Dino {

  public static final int STAND_STILL = 1, RUNNING = 2, JUMPING = 3, DIE = 4;

  /** Jump apex as a fraction of panel height, so the hop scales with the window. */
  private static final double JUMP_HEIGHT_FRACTION = 0.20;
  /** Time to the top of the arc, in seconds. Sets the feel; gravity is derived from it. */
  private static final double TIME_TO_APEX = 0.34;
  /** Seconds between footfalls while running. */
  private static final double STEP_INTERVAL = 0.09;

  private final BufferedImage standing;
  private final BufferedImage leftFootDino;
  private final BufferedImage rightFootDino;
  private final BufferedImage deadDino;

  private int state = STAND_STILL;

  /** Feet-on-ground Y for the sprite's top edge. */
  private int restingY;
  /** Current top edge, as a double so the arc stays smooth. */
  private double y;
  /** Vertical velocity in px/s; negative is upward. */
  private double vy;
  private double gravity;
  private double jumpVelocity;

  private double stepClock;
  private boolean leftFoot;

  public Dino() {
    standing = Assets.image("images/Dino-stand.png");
    leftFootDino = Assets.image("images/Dino-left-up.png");
    rightFootDino = Assets.image("images/Dino-right-up.png");
    deadDino = Assets.image("images/Dino-big-eyes.png");

    resize(800, 600);
    y = restingY;
  }

  /**
   * Recomputes ground-relative geometry. Called on every panel resize, and the reason the
   * dino now stays planted on the ground line at any window size.
   */
  public void resize(int panelWidth, int panelHeight) {
    boolean onGround = state != JUMPING;

    restingY = Ground.GROUND_Y - standing.getHeight() + 5;

    // Solve the arc: apex height h in time t gives v = 2h/t and g = 2h/t^2.
    double apex = Math.max(60.0, panelHeight * JUMP_HEIGHT_FRACTION);
    jumpVelocity = -2.0 * apex / TIME_TO_APEX;
    gravity = 2.0 * apex / (TIME_TO_APEX * TIME_TO_APEX);

    if (onGround) y = restingY;
  }

  /** Horizontal position, held at a fraction of the width so it scales with the panel. */
  public int x(int panelWidth) {
    return Math.max(40, (int) (panelWidth * 0.12));
  }

  public void update(double dt) {
    if (state == JUMPING) {
      vy += gravity * dt;
      y += vy * dt;
      if (y >= restingY) {
        y = restingY;
        vy = 0;
        state = RUNNING;
      }
    } else if (state == RUNNING) {
      y = restingY;
      stepClock += dt;
      if (stepClock >= STEP_INTERVAL) {
        stepClock = 0;
        leftFoot = !leftFoot;
      }
    } else if (state == STAND_STILL) {
      y = restingY;
    }
  }

  public void create(Graphics g, int panelWidth) {
    int drawX = x(panelWidth);
    int drawY = (int) Math.round(y);

    BufferedImage sprite;
    switch (state) {
      case RUNNING:
        // Throttled by stepClock. The original swapped feet on every repaint, i.e. 50 times
        // a second, which read as a blur rather than a run cycle.
        sprite = leftFoot ? leftFootDino : rightFootDino;
        break;
      case JUMPING:
        sprite = standing;
        break;
      case DIE:
        sprite = deadDino;
        break;
      default:
        sprite = standing;
        break;
    }

    g.drawImage(sprite, drawX, drawY, null);
  }

  public Rectangle getBounds(int panelWidth) {
    // Inset slightly: the sprite has transparent margins, and colliding on the bounding box
    // made near-misses register as hits.
    int inset = 4;
    return new Rectangle(x(panelWidth) + inset, (int) Math.round(y) + inset,
                         standing.getWidth() - inset * 2, standing.getHeight() - inset * 2);
  }

  public void startRunning() {
    state = RUNNING;
    y = restingY;
    vy = 0;
  }

  public void jump() {
    if (state == JUMPING || state == DIE) return; // no mid-air re-jump
    state = JUMPING;
    vy = jumpVelocity;
  }

  public void die() {
    state = DIE;
  }

  public void reset() {
    state = RUNNING;
    y = restingY;
    vy = 0;
    stepClock = 0;
  }

  public int getState() {
    return state;
  }
}
