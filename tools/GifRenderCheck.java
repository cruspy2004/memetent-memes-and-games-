import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * Confirms the meme rail actually paints its animated GIF in a live window.
 *
 * Offscreen rendering cannot verify this: an animated GIF advances through ImageObserver
 * callbacks, which only fire for a component attached to a real, displayed window. So this
 * shows a frame briefly, then measures how many distinct colours the rail paints. A blank
 * rail (the failure mode when the GIF does not load, or when a preferred-size override
 * collapses the panel to nothing) yields a handful; a real frame yields thousands.
 */
public final class GifRenderCheck {

  public static void main(String[] args) throws Exception {
    AppTheme.install();

    final BrickBreakerWindow[] holder = new BrickBreakerWindow[1];
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        holder[0] = new BrickBreakerWindow();
        holder[0].show();
      }
    });

    // Let the GIF decoder deliver a few frames.
    Thread.sleep(4000);

    final int[] result = new int[2];
    SwingUtilities.invokeAndWait(new Runnable() {
      public void run() {
        try {
          java.lang.reflect.Field f = BrickBreakerWindow.class.getDeclaredField("frame");
          f.setAccessible(true);
          JFrame frame = (JFrame) f.get(holder[0]);

          Component rail = findRail(frame.getContentPane());
          if (rail == null) throw new IllegalStateException("meme rail not found in the window");

          result[0] = rail.getWidth();

          BufferedImage img = new BufferedImage(Math.max(1, rail.getWidth()),
                                                Math.max(1, rail.getHeight()),
                                                BufferedImage.TYPE_INT_RGB);
          Graphics2D g = img.createGraphics();
          rail.paint(g);
          g.dispose();

          Set<Integer> colours = new HashSet<Integer>();
          for (int y = 0; y < img.getHeight(); y += 2) {
            for (int x = 0; x < img.getWidth(); x += 2) {
              colours.add(img.getRGB(x, y));
            }
          }
          result[1] = colours.size();

          ImageIO.write(img, "png", new java.io.File("screenshots/live-meme-rail.png"));
          frame.dispose();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    });

    System.out.println("rail width  : " + result[0] + "px");
    System.out.println("distinct colours in rail: " + result[1]);

    boolean ok = result[0] > 150 && result[1] > 500;
    System.out.println(ok ? "PASS - the rail is rendering its GIF"
                          : "FAIL - the rail looks blank");
    System.exit(ok ? 0 : 1);
  }

  private static Component findRail(java.awt.Container root) {
    for (Component c : root.getComponents()) {
      if (c instanceof MemeRail) return c;
      if (c instanceof java.awt.Container) {
        Component found = findRail((java.awt.Container) c);
        if (found != null) return found;
      }
    }
    return null;
  }
}
