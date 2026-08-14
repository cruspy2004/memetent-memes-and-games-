import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;

/**
 * Shrinks the meme GIFs for the packaged build.
 *
 * The three GIFs total about 76MB (hamaster 37MB, chippi 28MB, gigachad 11MB) and dominate
 * the installer size, but they are only ever drawn into a side rail 200-420px wide. Serving
 * a 900px-wide source into a 300px panel is pure download weight.
 *
 * This downscales to the width the rail actually uses and thins the frame rate, writing the
 * results to a separate output directory — the originals stay untouched in git, and only
 * the packaging step consumes the optimized copies.
 *
 * Written against ImageIO rather than ffmpeg/gifsicle so the build has no external
 * dependency to install.
 *
 * Run: java -cp build tools.OptimizeAssets &lt;outputDir&gt;
 */
public final class OptimizeAssets {

  /** Widest the meme rail is ever laid out (see RatioSplit bounds in the game windows). */
  private static final int TARGET_WIDTH = 420;
  /** Frames below this delay get merged, capping the result near this rate. */
  private static final int MIN_DELAY_CENTISECONDS = 7; // ~14fps

  private static final String[] TARGETS = { "hamaster.gif", "chippi.gif", "gigachad2.gif" };

  /** A frame plus how long it should be shown, in centiseconds. */
  private static final class Frame {
    BufferedImage image;
    int delay;
  }

  public static void main(String[] args) throws Exception {
    File outDir = new File(args.length > 0 ? args[0] : "assets-optimized");
    if (!outDir.isDirectory() && !outDir.mkdirs()) {
      throw new IllegalStateException("could not create " + outDir);
    }

    long before = 0;
    long after = 0;

    for (String name : TARGETS) {
      File src = utility.Assets.resolve(name);
      if (!src.isFile()) {
        System.out.println("skip   " + name + " (not found)");
        continue;
      }
      File dst = new File(outDir, name);

      List<Frame> frames = readFrames(src);
      if (frames.isEmpty()) {
        System.out.println("skip   " + name + " (no frames decoded)");
        continue;
      }
      List<Frame> thinned = thin(frames);
      writeGif(dst, thinned);

      before += src.length();
      after += dst.length();
      System.out.printf("ok     %-16s %5.1fMB -> %5.1fMB  (%d frames -> %d)%n",
                        name, mb(src.length()), mb(dst.length()), frames.size(), thinned.size());
    }

    System.out.println();
    System.out.printf("total  %.1fMB -> %.1fMB%n", mb(before), mb(after));
  }

  private static double mb(long bytes) {
    return bytes / (1024.0 * 1024.0);
  }

  // ---------------------------------------------------------------- reading

  /**
   * Decodes every frame, compositing onto a running canvas.
   *
   * GIF frames are frequently sub-rectangles of the logical screen that depend on the
   * previous frame, so reading each one in isolation gives torn fragments. Disposal method
   * has to be honoured for the composite to be correct.
   */
  private static List<Frame> readFrames(File file) throws Exception {
    List<Frame> frames = new ArrayList<Frame>();

    ImageInputStream in = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
    if (!readers.hasNext()) {
      in.close();
      return frames;
    }

    ImageReader reader = readers.next();
    reader.setInput(in, false);

    int canvasW = reader.getWidth(0);
    int canvasH = reader.getHeight(0);
    // The logical screen can be larger than frame 0; prefer it when present.
    IIOMetadataNode streamRoot = safeTree(reader.getStreamMetadata());
    if (streamRoot != null) {
      IIOMetadataNode lsd = child(streamRoot, "LogicalScreenDescriptor");
      if (lsd != null) {
        canvasW = Math.max(canvasW, parseInt(lsd.getAttribute("logicalScreenWidth"), canvasW));
        canvasH = Math.max(canvasH, parseInt(lsd.getAttribute("logicalScreenHeight"), canvasH));
      }
    }

    // Flatten onto an opaque canvas: the rail draws these in COVER mode with no
    // transparency, and dropping the alpha channel avoids GIF transparency edge cases on
    // write and compresses better.
    BufferedImage canvas = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_RGB);
    Graphics2D cg = canvas.createGraphics();
    cg.setColor(Color.BLACK);
    cg.fillRect(0, 0, canvasW, canvasH);

    BufferedImage saved = null;

    int index = 0;
    try {
      while (true) {
        BufferedImage raw;
        try {
          raw = reader.read(index);
        } catch (IndexOutOfBoundsException end) {
          break;
        } catch (Exception decodeError) {
          // Truncated or slightly malformed tail: keep what decoded cleanly.
          System.out.println("       (stopped at frame " + index + ": " + decodeError.getMessage() + ")");
          break;
        }

        IIOMetadataNode frameRoot = safeTree(reader.getImageMetadata(index));
        int left = 0, top = 0, delay = 10;
        String disposal = "none";

        if (frameRoot != null) {
          IIOMetadataNode gce = child(frameRoot, "GraphicControlExtension");
          if (gce != null) {
            delay = parseInt(gce.getAttribute("delayTime"), 10);
            String d = gce.getAttribute("disposalMethod");
            if (d != null && !d.isEmpty()) disposal = d;
          }
          IIOMetadataNode desc = child(frameRoot, "ImageDescriptor");
          if (desc != null) {
            left = parseInt(desc.getAttribute("imageLeftPosition"), 0);
            top = parseInt(desc.getAttribute("imageTopPosition"), 0);
          }
        }
        if (delay <= 0) delay = 10;

        if ("restoreToPrevious".equals(disposal)) {
          saved = copy(canvas);
        }

        cg.drawImage(raw, left, top, null);

        Frame frame = new Frame();
        frame.image = scale(canvas);
        frame.delay = delay;
        frames.add(frame);

        if ("restoreToBackgroundColor".equals(disposal)) {
          cg.setColor(Color.BLACK);
          cg.fillRect(left, top, raw.getWidth(), raw.getHeight());
        } else if ("restoreToPrevious".equals(disposal) && saved != null) {
          cg.drawImage(saved, 0, 0, null);
        }

        index++;
      }
    } finally {
      cg.dispose();
      reader.dispose();
      in.close();
    }

    return frames;
  }

  private static BufferedImage copy(BufferedImage src) {
    BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
    Graphics2D g = out.createGraphics();
    g.drawImage(src, 0, 0, null);
    g.dispose();
    return out;
  }

  private static BufferedImage scale(BufferedImage src) {
    if (src.getWidth() <= TARGET_WIDTH) return copy(src);

    int w = TARGET_WIDTH;
    int h = Math.max(1, (int) Math.round(src.getHeight() * (TARGET_WIDTH / (double) src.getWidth())));

    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = out.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.drawImage(src, 0, 0, w, h, null);
    g.dispose();
    return out;
  }

  /** Merges very short frames, rolling their delay into the frame that is kept. */
  private static List<Frame> thin(List<Frame> frames) {
    List<Frame> out = new ArrayList<Frame>();
    int carried = 0;

    for (Frame f : frames) {
      carried += f.delay;
      if (carried >= MIN_DELAY_CENTISECONDS) {
        Frame kept = new Frame();
        kept.image = f.image;
        kept.delay = carried;
        out.add(kept);
        carried = 0;
      }
    }

    if (out.isEmpty() && !frames.isEmpty()) out.add(frames.get(0));
    // Any leftover time is added to the last surviving frame so the loop length holds.
    if (carried > 0 && !out.isEmpty()) out.get(out.size() - 1).delay += carried;
    return out;
  }

  // ---------------------------------------------------------------- writing

  private static void writeGif(File dst, List<Frame> frames) throws Exception {
    ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
    FileImageOutputStream out = new FileImageOutputStream(dst);
    writer.setOutput(out);

    ImageWriteParam param = writer.getDefaultWriteParam();
    IIOMetadata meta = writer.getDefaultImageMetadata(
        javax.imageio.ImageTypeSpecifier.createFromRenderedImage(frames.get(0).image), param);

    writer.prepareWriteSequence(null);

    for (int i = 0; i < frames.size(); i++) {
      Frame f = frames.get(i);
      IIOMetadata frameMeta = writer.getDefaultImageMetadata(
          javax.imageio.ImageTypeSpecifier.createFromRenderedImage(f.image), param);
      configureFrame(frameMeta, f.delay, i == 0);
      writer.writeToSequence(new IIOImage(f.image, null, frameMeta), param);
    }

    writer.endWriteSequence();
    out.close();
    writer.dispose();
  }

  /** Sets the per-frame delay, and on the first frame the Netscape infinite-loop block. */
  private static void configureFrame(IIOMetadata meta, int delayCentis, boolean first)
      throws Exception {
    String format = meta.getNativeMetadataFormatName();
    IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);

    IIOMetadataNode gce = child(root, "GraphicControlExtension");
    if (gce == null) {
      gce = new IIOMetadataNode("GraphicControlExtension");
      root.appendChild(gce);
    }
    gce.setAttribute("disposalMethod", "none");
    gce.setAttribute("userInputFlag", "FALSE");
    gce.setAttribute("transparentColorFlag", "FALSE");
    gce.setAttribute("transparentColorIndex", "0");
    gce.setAttribute("delayTime", Integer.toString(Math.max(1, delayCentis)));

    if (first) {
      IIOMetadataNode appExtensions = child(root, "ApplicationExtensions");
      if (appExtensions == null) {
        appExtensions = new IIOMetadataNode("ApplicationExtensions");
        root.appendChild(appExtensions);
      }
      IIOMetadataNode netscape = new IIOMetadataNode("ApplicationExtension");
      netscape.setAttribute("applicationID", "NETSCAPE");
      netscape.setAttribute("authenticationCode", "2.0");
      netscape.setUserObject(new byte[] { 0x1, 0, 0 }); // loop forever
      appExtensions.appendChild(netscape);
    }

    meta.setFromTree(format, root);
  }

  // ---------------------------------------------------------------- helpers

  private static IIOMetadataNode safeTree(IIOMetadata meta) {
    if (meta == null) return null;
    try {
      return (IIOMetadataNode) meta.getAsTree(meta.getNativeMetadataFormatName());
    } catch (Exception e) {
      return null;
    }
  }

  private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
    if (parent == null) return null;
    for (int i = 0; i < parent.getLength(); i++) {
      org.w3c.dom.Node n = parent.item(i);
      if (n instanceof IIOMetadataNode && name.equalsIgnoreCase(n.getNodeName())) {
        return (IIOMetadataNode) n;
      }
    }
    return null;
  }

  private static int parseInt(String value, int fallback) {
    try {
      return (value == null || value.isEmpty()) ? fallback : Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
