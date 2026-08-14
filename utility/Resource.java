package utility;

import java.awt.image.BufferedImage;

/**
 * Kept for the {@code components} classes that already call it. The real work now lives in
 * {@link Assets}.
 *
 * The old implementation was {@code ImageIO.read(getClass().getResource(path))} against
 * paths like {@code "../images/Dino-stand.png"}. A leading {@code ../} on a classpath
 * resource is not portable, and when the lookup failed {@code getResource} returned null,
 * so {@code ImageIO.read} threw an unhelpful NPE from whichever paint call happened to
 * touch the image first. Assets resolves against real directories and substitutes a visible
 * placeholder instead of failing at paint time.
 */
public class Resource {

  public BufferedImage getResourceImage(String path) {
    return Assets.image(normalize(path));
  }

  /** Strips the legacy {@code ../} prefix so old call sites keep resolving. */
  private String normalize(String path) {
    String p = path.replace('\\', '/');
    while (p.startsWith("../") || p.startsWith("./")) {
      p = p.substring(p.indexOf('/') + 1);
    }
    return p;
  }
}
