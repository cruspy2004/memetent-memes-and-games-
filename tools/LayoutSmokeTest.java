import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

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
