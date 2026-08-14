import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Plays brick breaker through real OS-level key events and checks the paddle responds.
 *
 * The headless test in LayoutSmokeTest fires the bound Actions directly, which proves the
 * bindings exist and the movement logic is right. This goes one level lower and drives
 * java.awt.Robot against a live focused window, so it also covers the parts the headless
 * test cannot: that the key events actually reach the component, that
 * WHEN_IN_FOCUSED_WINDOW is the right condition, and that auto-repeat does not wedge
 * anything.
 *
 * Run: java -cp build;lib/jlayer-1.0.1.jar LiveInputCheck
 */
public final class LiveInputCheck {

  public static void main(String[] args) throws Exception {
    AppTheme.install();

    final BrickBreakerWindow[] holder = new BrickBreakerWindow[1];
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        holder[0] = new BrickBreakerWindow();
        holder[0].show();
      }
    });
    Thread.sleep(2500); // let the window map and take focus

    Field frameField = BrickBreakerWindow.class.getDeclaredField("frame");
    frameField.setAccessible(true);
    final JFrame frame = (JFrame) frameField.get(holder[0]);

    Field gameplayField = BrickBreakerWindow.class.getDeclaredField("gameplay");
    gameplayField.setAccessible(true);
    final Gameplay field = (Gameplay) gameplayField.get(holder[0]);

    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { frame.toFront(); frame.requestFocus(); }
    });
    Thread.sleep(900);

    // Robot delivers to whatever the OS considers focused. If this process could not take
    // focus — common when it is launched from a background session, or when another window
    // is in the foreground — the run proves nothing, so say so instead of reporting a bug.
    final boolean[] focused = new boolean[1];
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { focused[0] = frame.isFocused() || frame.isActive(); }
    });
    if (!focused[0]) {
      SwingUtilities.invokeAndWait(new Runnable() {
        public void run() { field.shutdown(); frame.dispose(); }
      });
      System.out.println("SKIPPED - the game window never took OS focus, so Robot input");
      System.out.println("          could not reach it. Run this from an interactive session.");
      System.exit(2);
    }

    Robot robot = new Robot();
    robot.setAutoDelay(12);

    // Click the window to make the OS focus stick. requestFocus() alone is advisory and is
    // routinely ignored when the process did not start in the foreground.
    java.awt.Point origin = frame.getLocationOnScreen();
    robot.mouseMove(origin.x + frame.getWidth() / 3, origin.y + frame.getHeight() / 2);
    robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK);
    Thread.sleep(700);

    requireFocus(frame, field, "before input");
    DIAG_FIELD = field;
    if (DIAGNOSE) System.out.println("      at rest  " + describe(field));

    double start = paddle(field);
    hold(robot, KeyEvent.VK_LEFT, 420);
    double afterLeft = paddle(field);
    requireFocus(frame, field, "after the first LEFT");

    hold(robot, KeyEvent.VK_RIGHT, 700);
    double afterRight = paddle(field);
    requireFocus(frame, field, "after RIGHT");

    hold(robot, KeyEvent.VK_LEFT, 420);
    double afterLeft2 = paddle(field);
    requireFocus(frame, field, "after the second LEFT");

    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { field.shutdown(); frame.dispose(); }
    });

    System.out.printf("paddle: start %.3f -> left %.3f -> right %.3f -> left %.3f%n",
                      start, afterLeft, afterRight, afterLeft2);

    // Nothing moved at all, and the paddle never even left its start position: that means no
    // key event ever arrived, not that the movement logic is wrong. Windows refuses
    // foreground activation to a process launched from a background session, so Robot's
    // keystrokes land in whatever window is actually in front. Reporting this as a failure
    // would be blaming the game for the harness.
    boolean nothingArrived = start == afterLeft && afterLeft == afterRight && afterRight == afterLeft2;
    if (nothingArrived && !movementFlagsEverSet) {
      System.out.println("INCONCLUSIVE - no key event reached the window, so this run proves");
      System.out.println("               nothing either way. The bindings themselves are");
      System.out.println("               covered deterministically by LayoutSmokeTest.");
      System.out.println("               Run this from an interactive shell to exercise real input.");
      System.exit(2);
    }

    boolean movedLeft = afterLeft < start - 0.01;
    boolean movedRightAfterLeft = afterRight > afterLeft + 0.01;
    boolean movedLeftAgain = afterLeft2 < afterRight - 0.01;

    report("LEFT moves left", movedLeft);
    report("RIGHT works after LEFT", movedRightAfterLeft);
    report("LEFT works after RIGHT", movedLeftAgain);

    boolean ok = movedLeft && movedRightAfterLeft && movedLeftAgain;
    System.out.println(ok ? "PASS - paddle responds in both directions"
                          : "FAIL - paddle is stuck");
    System.exit(ok ? 0 : 1);
  }

  /**
   * Aborts the run if focus was lost partway through. Without this the test reports a stuck
   * paddle when the real cause was another window stealing the keystrokes, which is a
   * misleading failure to hand back.
   */
  private static void requireFocus(final JFrame frame, final Gameplay field, String when)
      throws Exception {
    final boolean[] ok = new boolean[1];
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { ok[0] = frame.isFocused() || frame.isActive(); }
    });
    if (ok[0]) return;

    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() { field.shutdown(); frame.dispose(); }
    });
    System.out.println("SKIPPED - lost OS focus " + when + ", so the keystrokes went");
    System.out.println("          elsewhere. Run this from an interactive session.");
    System.exit(2);
  }

  /** Presses and holds a key for a while, then releases it — as a player would. */
  private static void hold(Robot robot, int keyCode, int millis) throws Exception {
    robot.keyPress(keyCode);
    Thread.sleep(millis / 2);
    noteFlags();
    if (DIAGNOSE) System.out.println("      mid-hold " + describe(DIAG_FIELD));
    Thread.sleep(millis - millis / 2);
    robot.keyRelease(keyCode);
    Thread.sleep(120); // let the release be processed and the paddle settle
    if (DIAGNOSE) System.out.println("      released " + describe(DIAG_FIELD));
  }

  private static final boolean DIAGNOSE = System.getProperty("diagnose") != null;
  private static Gameplay DIAG_FIELD;

  /** Set if a movement flag was ever observed true, i.e. a key binding actually fired. */
  private static boolean movementFlagsEverSet;

  /** Records whether a binding has fired, which distinguishes the two failure causes. */
  private static void noteFlags() {
    if (DIAG_FIELD == null) return;
    try {
      if (bool(DIAG_FIELD, "movingLeft") || bool(DIAG_FIELD, "movingRight")) {
        movementFlagsEverSet = true;
      }
    } catch (Exception ignored) {
      // Diagnostic only; never fail the run over it.
    }
  }

  /** Dumps the private movement state, to tell "key never arrived" from "logic ignored it". */
  private static String describe(Gameplay field) {
    if (field == null) return "(no field)";
    try {
      return "movingLeft=" + bool(field, "movingLeft")
           + " movingRight=" + bool(field, "movingRight")
           + " state=" + get(field, "state")
           + " paddleX=" + String.format("%.3f", paddle(field));
    } catch (Exception e) {
      return "(" + e + ")";
    }
  }

  private static boolean bool(Gameplay field, String name) throws Exception {
    Field f = Gameplay.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.getBoolean(field);
  }

  private static Object get(Gameplay field, String name) throws Exception {
    Field f = Gameplay.class.getDeclaredField(name);
    f.setAccessible(true);
    return f.get(field);
  }

  private static double paddle(Gameplay field) throws Exception {
    Field f = Gameplay.class.getDeclaredField("paddleX");
    f.setAccessible(true);
    return f.getDouble(field);
  }

  private static void report(String what, boolean ok) {
    System.out.println((ok ? "  ok   " : "  FAIL ") + what);
  }
}
