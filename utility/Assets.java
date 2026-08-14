package utility;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

/**
 * Finds the game's media files.
 *
 * Everything used to be loaded straight off a bare relative path — {@code new
 * ImageIcon("hamaster.gif")}, {@code new File("chippi.mp3")} — which only resolves when the
 * JVM's working directory happens to be the project root. That works from an IDE and breaks
 * the moment the app is launched by double-click or installed somewhere, which is exactly
 * what shipping it to an audience requires.
 *
 * So resolution walks a list of candidate roots and takes the first hit. Loaded images are
 * cached, because the menu previously re-decoded a 600px PNG on every mouse-enter.
 *
 * Lives in {@code utility} rather than the default package so the {@code components}
 * classes can import it — Java has no syntax for importing from the default package.
 */
public final class Assets {

  /** Set by the packaged launcher to the install directory. See build.ps1. */
  private static final String APP_DIR_PROPERTY = "memetent.assets";

  private static final File[] ROOTS = buildRoots();

  private static final Map<String, ImageIcon> ICON_CACHE = new LinkedHashMap<String, ImageIcon>();
  private static final Map<String, BufferedImage> IMAGE_CACHE = new LinkedHashMap<String, BufferedImage>();

  private Assets() {
  }

  private static File[] buildRoots() {
    List<File> roots = new ArrayList<File>();

    String configured = System.getProperty(APP_DIR_PROPERTY);
    if (configured != null && !configured.isEmpty()) roots.add(new File(configured));

    // Where the JVM was started from.
    roots.add(new File("."));

    // Beside the jar/classes we were loaded from, and one and two levels up. That covers
    // "app/game.jar + assets" as well as jpackage's "app/lib/game.jar + app/assets".
    try {
      File self = new File(Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      File dir = self.isDirectory() ? self : self.getParentFile();
      for (int i = 0; i < 3 && dir != null; i++) {
        roots.add(dir);
        dir = dir.getParentFile();
      }
    } catch (Exception ignored) {
      // Sealed or unusual class loader; the other roots still apply.
    }

    return roots.toArray(new File[0]);
  }

  /**
   * Resolves a project-relative path such as {@code "images/Ground.png"}. Returns a File at
   * the first root that actually contains it; if none do, returns the working-directory
   * candidate so callers report a sensible path in their error.
   */
  public static File resolve(String relativePath) {
    for (File root : ROOTS) {
      File candidate = new File(root, relativePath);
      if (candidate.isFile()) return candidate;
    }
    return new File(relativePath);
  }

  public static boolean exists(String relativePath) {
    return resolve(relativePath).isFile();
  }

  /**
   * Loads an animated GIF or still image as an ImageIcon. Cached — an ImageIcon holds a
   * decoded, animating Image, and a second one for the same file would drive a second
   * animation for no reason.
   */
  public static synchronized ImageIcon icon(String relativePath) {
    ImageIcon cached = ICON_CACHE.get(relativePath);
    if (cached != null) return cached;

    File file = resolve(relativePath);
    ImageIcon icon;
    if (file.isFile()) {
      icon = new ImageIcon(file.getAbsolutePath());
    } else {
      warnMissing(relativePath);
      icon = new ImageIcon(placeholder(320, 240));
    }
    ICON_CACHE.put(relativePath, icon);
    return icon;
  }

  /** Loads a still image for direct drawing. Cached for the same reason as {@link #icon}. */
  public static synchronized BufferedImage image(String relativePath) {
    BufferedImage cached = IMAGE_CACHE.get(relativePath);
    if (cached != null) return cached;

    BufferedImage img = null;
    File file = resolve(relativePath);
    if (file.isFile()) {
      try {
        img = ImageIO.read(file);
      } catch (Exception e) {
        System.err.println("Assets: could not decode " + relativePath + ": " + e.getMessage());
      }
    } else {
      warnMissing(relativePath);
    }

    if (img == null) img = placeholder(48, 48);
    IMAGE_CACHE.put(relativePath, img);
    return img;
  }

  /** Convenience for the audio layer, which wants a File rather than an Image. */
  public static File audio(String relativePath) {
    return resolve(relativePath);
  }

  /**
   * A visible magenta box standing in for a file we could not find. Better than the old
   * failure mode: {@code ImageIO.read(getClass().getResource(...))} returned null and the
   * NullPointerException surfaced later, deep inside a paint loop.
   */
  private static BufferedImage placeholder(int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setColor(new java.awt.Color(220, 40, 160));
    g.fillRect(0, 0, w, h);
    g.setColor(java.awt.Color.WHITE);
    g.drawRect(0, 0, w - 1, h - 1);
    g.dispose();
    return img;
  }

  private static void warnMissing(String relativePath) {
    StringBuilder sb = new StringBuilder();
    for (File root : ROOTS) {
      if (sb.length() > 0) sb.append(", ");
      sb.append(root.getAbsolutePath());
    }
    System.err.println("Assets: missing " + relativePath + " (looked in " + sb + ")");
  }
}
