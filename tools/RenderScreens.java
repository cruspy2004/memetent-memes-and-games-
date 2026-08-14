import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;

/**
 * Renders each screen to a PNG at several window sizes, for reviewing the layout without
 * having to resize four windows by hand.
 *
 * Runs against real component instances and the real paint path, so what lands in the PNG
 * is what the game draws. Frames are constructed but never shown.
 *
 * Run: java -cp build;lib/jlayer-1.0.1.jar RenderScreens screenshots
 */
public final class RenderScreens {

  private static File outDir;

  public static void main(String[] args) throws Exception {
    outDir = new File(args.length > 0 ? args[0] : "screenshots");
    if (!outDir.isDirectory() && !outDir.mkdirs()) throw new IllegalStateException("mkdir failed");

    AppTheme.install();

    // Give the animated GIFs a moment to decode their first frame, or the rails render empty.
    Thread.sleep(1200);

    renderLanding("01-landing-wide", 1280, 760);
    renderLanding("02-landing-narrow", 640, 720);

    renderPicker("03-picker-wide", 1280, 800);
    renderPicker("04-picker-stacked", 620, 860);

    renderBrickBreaker("05-brick-breaker", 1280, 800);
    renderBrickBreaker("06-brick-breaker-small", 760, 560);

    renderDino("07-dino-run", 1280, 800);
    renderDino("08-dino-run-small", 760, 560);

    System.out.println("\nWrote screenshots to " + outDir.getAbsolutePath());

    // AWT's event thread is not a daemon, and constructing the frames started it, so the JVM
    // would otherwise sit here forever after the work is done.
    System.exit(0);
  }

  private static void renderLanding(String name, int w, int h) throws Exception {
    FirstPage page = new FirstPage();
    save(name, page.getContentPane(), w, h);
    page.dispose();
  }

  private static void renderPicker(String name, int w, int h) throws Exception {
    GameStartupPage picker = new GameStartupPage();
    Object frame = field(picker, "frame");
    Container content = (Container) invoke(frame, "getContentPane");
    save(name, content, w, h);
    invoke(frame, "dispose");
  }

  private static void renderBrickBreaker(String name, int w, int h) throws Exception {
    BrickBreakerWindow window = new BrickBreakerWindow();
    Object frame = field(window, "frame");
    Container content = (Container) invoke(frame, "getContentPane");
    Gameplay field = (Gameplay) field(window, "gameplay");

    size(content, w, h);

    // Play a little so the shot shows a real mid-game state rather than the ready banner.
    call(field, "launchOrRestart");
    for (int i = 0; i < 240; i++) field.actionPerformed(null);

    save(name, content, w, h);
    field.shutdown();
    invoke(frame, "dispose");
  }

  private static void renderDino(String name, int w, int h) throws Exception {
    UserInterface ui = new UserInterface();
    // createAndShowGUI builds and shows; build the same tree without showing by reflection
    // is more trouble than it is worth, so drive the panel directly.
    GamePanel panel = new GamePanel();
    MemeRail rail = new MemeRail("HYPE METER");
    rail.addCard(UserInterface.CARD_CHAD,
                 new GIFPanel("gigachad2.gif").withMode(GIFPanel.Mode.COVER));
    rail.show(UserInterface.CARD_CHAD, "Keep running.");

    RatioSplit split = new RatioSplit(0.74, true, 0).collapseBelow(880).secondaryBounds(200, 400);
    javax.swing.JPanel root = new javax.swing.JPanel(split);
    root.setBackground(Theme.BG_DEEP);
    root.add(panel, RatioSplit.PRIMARY);
    root.add(rail, RatioSplit.SECONDARY);

    size(root, w, h);
    call(panel, "start");
    for (int i = 0; i < 90; i++) panel.actionPerformed(null);

    save(name, root, w, h);
    panel.shutdown();
  }

  // ------------------------------------------------------------- harness

  private static void size(Component c, int w, int h) {
    c.setSize(new Dimension(w, h));
    layoutTree(c);
  }

  private static void save(String name, Component c, int w, int h) throws Exception {
    size(c, w, h);

    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = img.createGraphics();
    try {
      c.paint(g);
    } finally {
      g.dispose();
    }

    File out = new File(outDir, name + ".png");
    ImageIO.write(img, "png", out);
    System.out.println("  " + out.getName() + "  " + w + "x" + h);
  }

  private static void layoutTree(Component c) {
    c.doLayout();
    if (c instanceof Container) {
      for (Component child : ((Container) c).getComponents()) layoutTree(child);
    }
  }

  private static Object field(Object target, String name) throws Exception {
    java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(target);
  }

  private static Object invoke(Object target, String name) throws Exception {
    Method m = target.getClass().getMethod(name);
    m.setAccessible(true);
    return m.invoke(target);
  }

  private static void call(Object target, String name) throws Exception {
    Method m = target.getClass().getDeclaredMethod(name);
    m.setAccessible(true);
    m.invoke(target);
  }
}
