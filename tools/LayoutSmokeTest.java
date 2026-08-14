import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import components.Dino;

/**
 * Headless check that every screen lays out and paints cleanly across a wide range of
 * window sizes.
 *
 * This is the regression guard for the bug class the rewrite was about: geometry captured
 * once at construction and never revisited. A screen that paints fine at its design size
 * and throws — or draws outside its own bounds — at 480x360 or 2560x1440 fails here.
 *
 * Run: java -cp build;lib/jlayer-1.0.1.jar -Djava.awt.headless=true LayoutSmokeTest
 */
public class LayoutSmokeTest {

  /** Deliberately spans phone-ish, laptop, and 1440p projector proportions. */
  private static final int[][] SIZES = {
      { 480, 360 }, { 640, 480 }, { 800, 600 }, { 1024, 768 },
      { 1280, 720 }, { 1440, 900 }, { 1920, 1080 }, { 2560, 1440 },
      { 700, 1000 }, // portrait, to catch width/height mix-ups
  };

  private static int failures;
  private static int checks;

  public static void main(String[] args) throws Exception {
    AppTheme.install();

    for (int[] size : SIZES) {
      int w = size[0];
      int h = size[1];

      checkBrickBreaker(w, h);
      checkDino(w, h);
      checkRatioSplit(w, h);
    }

    checkPaddleControls();
    checkDinoGameplay();

    System.out.println();
    System.out.println(checks + " checks across " + SIZES.length + " window sizes, "
                       + failures + " failure(s).");
    if (failures > 0) System.exit(1);
    System.out.println("Layout smoke test PASSED");
  }

  // ------------------------------------------------------------- screens

  private static void checkBrickBreaker(int w, int h) {
    Gameplay field = new Gameplay();
    try {
      paintAt(field, w, h);

      // The play field must keep every moving part inside itself at any size.
      java.awt.Rectangle bounds = new java.awt.Rectangle(0, 0, w, h);
      expect("brick breaker " + w + "x" + h + ": paddle inside field",
             bounds.contains(invokeRect(field, "paddleRect")));
      expect("brick breaker " + w + "x" + h + ": ball inside field",
             bounds.intersects(invokeRect(field, "ballRect")));
      expect("brick breaker " + w + "x" + h + ": brick area inside field",
             bounds.contains(invokeRect(field, "brickArea")));
    } catch (Exception e) {
      fail("brick breaker " + w + "x" + h + " threw " + e);
    } finally {
      field.shutdown();
    }
  }

  private static void checkDino(int w, int h) {
    GamePanel panel = new GamePanel();
    try {
      paintAt(panel, w, h);

      // The ground line is the value that used to go stale; assert it tracked the resize.
      int groundY = components.Ground.GROUND_Y;
      expect("dino " + w + "x" + h + ": ground line inside panel",
             groundY > 0 && groundY < h);
      expect("dino " + w + "x" + h + ": ground line in lower half",
             groundY > h / 2);
    } catch (Exception e) {
      fail("dino " + w + "x" + h + " threw " + e);
    } finally {
      panel.shutdown();
    }
  }

  private static void checkRatioSplit(int w, int h) {
    RatioSplit split = new RatioSplit(0.72, true, 0)
        .collapseBelow(900)
        .secondaryBounds(200, 420);

    JPanel root = new JPanel(split);
    JPanel primary = new JPanel();
    JPanel secondary = new JPanel();
    root.add(primary, RatioSplit.PRIMARY);
    root.add(secondary, RatioSplit.SECONDARY);

    paintAt(root, w, h);

    if (w < 900) {
      expect("split " + w + "x" + h + ": rail collapsed below breakpoint",
             !secondary.isVisible() && primary.getWidth() == w);
    } else {
      expect("split " + w + "x" + h + ": panes fill the width exactly",
             primary.getWidth() + secondary.getWidth() == w);
      expect("split " + w + "x" + h + ": rail within its clamps",
             secondary.getWidth() >= 200 && secondary.getWidth() <= 420);
      expect("split " + w + "x" + h + ": no overlap",
             primary.getWidth() > 0 && secondary.getX() == primary.getWidth());
    }
  }

  /**
   * Drives the real key bindings and checks the paddle moves both ways, in either order.
   *
   * Regression test for a shipped bug: the release bindings were registered as
   * "RELEASED LEFT", which KeyStroke.getKeyStroke(String) cannot parse (the keyword must be
   * lowercase). It returns null for anything unparseable and InputMap.put discards a null
   * without complaint, so the bindings silently did not exist. The movement flag was then
   * set on key-down and never cleared, and pressing the opposite arrow left both flags true
   * — which the "both held cancel out" guard reads as "stop", freezing the paddle.
   *
   * Asserting on the descriptor strings would not have caught it; only pressing the keys
   * and watching the paddle does.
   */
  private static void checkPaddleControls() {
    Gameplay field = new Gameplay();
    try {
      paintAt(field, 1000, 700);

      press(field, java.awt.event.KeyEvent.VK_LEFT, false);
      double afterLeft = tickAndReadPaddle(field, 12);
      expect("controls: LEFT moves the paddle left", afterLeft < 0.5);

      press(field, java.awt.event.KeyEvent.VK_LEFT, true); // release
      press(field, java.awt.event.KeyEvent.VK_RIGHT, false);
      double afterRight = tickAndReadPaddle(field, 24);
      expect("controls: RIGHT still works after LEFT (the shipped bug)", afterRight > afterLeft);

      press(field, java.awt.event.KeyEvent.VK_RIGHT, true);
      press(field, java.awt.event.KeyEvent.VK_LEFT, false);
      double backLeft = tickAndReadPaddle(field, 24);
      expect("controls: LEFT still works after RIGHT", backLeft < afterRight);

      // Both held should cancel out rather than drift.
      press(field, java.awt.event.KeyEvent.VK_RIGHT, false);
      double bothA = tickAndReadPaddle(field, 4);
      double bothB = tickAndReadPaddle(field, 8);
      expect("controls: both arrows held cancel out", Math.abs(bothA - bothB) < 1e-9);
    } catch (Exception e) {
      fail("paddle controls threw " + e);
    } finally {
      field.shutdown();
    }
  }

  /**
   * Plays a round of T-Rex run: start, jump, land, collide, restart.
   *
   * Covers the parts of the dino rewrite the layout checks do not touch — the jump was
   * replaced with a gravity arc, and collision boxes are now derived from Ground.SCALE. Both
   * are easy to get wrong in a way that still paints correctly.
   *
   * Movement is time-based, so this ticks with real sleeps rather than in a tight loop:
   * spinning the timer without elapsed time yields a dt near zero and nothing moves.
   */
  private static void checkDinoGameplay() {
    GamePanel panel = new GamePanel();
    try {
      paintAt(panel, 1200, 700);
      expect("dino run: starts in READY", "READY".equals(stateOf(panel)));

      press(panel, java.awt.event.KeyEvent.VK_SPACE, false);
      expect("dino run: SPACE starts the run", "RUNNING".equals(stateOf(panel)));

      Dino dino = dinoOf(panel);
      tick(panel, 6);
      int groundY = dino.getBounds(1200).y;

      // Jump: the dino must leave the ground and come back to it.
      press(panel, java.awt.event.KeyEvent.VK_SPACE, false);
      tick(panel, 6);
      int airborneY = dino.getBounds(1200).y;
      expect("dino run: jump lifts it off the ground", airborneY < groundY);

      // A second press mid-air must be ignored. The old jump() reset the dino to the ground
      // on every call, so pressing again in flight teleported it down and restarted the hop.
      //
      // Checking only "still above the ground" is not enough to catch that: the teleport is
      // immediately followed by an upward step, so the dino is a few pixels up again by the
      // next frame and a naive check passes. It has to assert the dino is still *clearly*
      // airborne, near where it already was.
      int clearance = (groundY - airborneY) / 2;
      press(panel, java.awt.event.KeyEvent.VK_SPACE, false);
      tick(panel, 1);
      expect("dino run: mid-air press does not restart the hop",
             dino.getBounds(1200).y < groundY - clearance);

      // Let the arc complete.
      int settle = 0;
      while (dino.getBounds(1200).y < groundY && settle < 120) {
        tick(panel, 1);
        settle++;
      }
      expect("dino run: it lands again", dino.getBounds(1200).y >= groundY);

      // Run on without jumping until a cactus catches it. This is the collision path, and
      // it only works if the scaled obstacle boxes line up with the scaled dino box.
      int guard = 0;
      while (!"DEAD".equals(stateOf(panel)) && guard < 400) {
        tick(panel, 1);
        guard++;
      }
      expect("dino run: a cactus ends the run", "DEAD".equals(stateOf(panel)));

      // And restart works.
      press(panel, java.awt.event.KeyEvent.VK_SPACE, false);
      expect("dino run: SPACE restarts after death", "RUNNING".equals(stateOf(panel)));
    } catch (Exception e) {
      fail("dino gameplay threw " + e);
    } finally {
      panel.shutdown();
    }
  }

  /** One frame of simulation, with enough real time elapsed for dt to be meaningful. */
  private static void tick(GamePanel panel, int frames) {
    for (int i = 0; i < frames; i++) {
      try {
        Thread.sleep(20);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      panel.actionPerformed(null);
    }
  }

  private static String stateOf(GamePanel panel) throws Exception {
    java.lang.reflect.Field f = GamePanel.class.getDeclaredField("state");
    f.setAccessible(true);
    return String.valueOf(f.get(panel));
  }

  private static Dino dinoOf(GamePanel panel) throws Exception {
    java.lang.reflect.Field f = GamePanel.class.getDeclaredField("dino");
    f.setAccessible(true);
    return (Dino) f.get(panel);
  }

  /** Fires the binding registered for a key, exactly as a real key event would. */
  private static void press(javax.swing.JComponent c, int keyCode, boolean release) {
    javax.swing.KeyStroke ks = javax.swing.KeyStroke.getKeyStroke(keyCode, 0, release);
    Object name = c.getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks);
    if (name == null) {
      fail("controls: no binding registered for key " + keyCode + (release ? " (release)" : " (press)"));
      return;
    }
    javax.swing.Action action = c.getActionMap().get(name);
    if (action == null) {
      fail("controls: no action for " + name);
      return;
    }
    action.actionPerformed(new java.awt.event.ActionEvent(c, 0, String.valueOf(name)));
  }

  private static double tickAndReadPaddle(Gameplay field, int ticks) throws Exception {
    for (int i = 0; i < ticks; i++) field.actionPerformed(null);
    java.lang.reflect.Field f = Gameplay.class.getDeclaredField("paddleX");
    f.setAccessible(true);
    return f.getDouble(field);
  }

  // ------------------------------------------------------------- harness

  /**
   * Sizes, lays out and paints a component without ever creating a native window, so this
   * runs headless in CI.
   */
  private static void paintAt(Component c, int w, int h) {
    c.setSize(new Dimension(w, h));
    layoutTree(c);

    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    try {
      c.paint(g);
    } finally {
      g.dispose();
    }
  }

  /** doLayout() top-down; validate() would need native peers we do not have here. */
  private static void layoutTree(Component c) {
    c.doLayout();
    if (c instanceof Container) {
      for (Component child : ((Container) c).getComponents()) {
        layoutTree(child);
      }
    }
  }

  /** Reads a private geometry helper, so the test checks real internals not a copy of them. */
  private static java.awt.Rectangle invokeRect(Object target, String method) throws Exception {
    java.lang.reflect.Method m = target.getClass().getDeclaredMethod(method);
    m.setAccessible(true);
    return (java.awt.Rectangle) m.invoke(target);
  }

  private static void expect(String what, boolean ok) {
    checks++;
    if (!ok) fail(what);
  }

  private static void fail(String what) {
    failures++;
    System.out.println("  FAIL  " + what);
  }
}
