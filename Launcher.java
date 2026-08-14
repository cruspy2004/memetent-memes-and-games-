import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import utility.Assets;

/**
 * Single entry point for the packaged application.
 *
 * jpackage needs one main class, and the individual screens each having their own
 * {@code main} is convenient in an IDE but ambiguous to a build. This is that one class.
 *
 * It also fails loudly and legibly when media is missing. Previously a missing file surfaced
 * as a NullPointerException from inside a paint call, or as an invisible image with no
 * explanation — neither of which is something an audience member can report usefully.
 */
public final class Launcher {

  /** Everything the games actually load. Also the manifest the packaging step stages. */
  private static final String[] REQUIRED = {
      "background.png",
      "Brickbreacker_picture.png",
      "Dinogame_pic.png",
      "hamaster.gif", "chippi.gif", "gigachad2.gif",
      "hamaster.mp3", "chippi.mp3", "gigachad.mp3",
      "images/Ground.png", "images/Sun.png",
      "images/Dino-stand.png", "images/Dino-left-up.png",
      "images/Dino-right-up.png", "images/Dino-big-eyes.png",
      "images/Cactus-1.png", "images/Cactus-2.png", "images/Cactus-5.png",
  };

  private Launcher() {
  }

  public static void main(String[] args) {
    // `--verify-assets` checks the install without opening a window. Used by build.ps1 to
    // prove the packaged layout resolves, and useful for diagnosing a bad install in the
    // field, where "it doesn't start" is otherwise all the report you get.
    if (args.length > 0 && "--verify-assets".equals(args[0])) {
      String problems = findMissing();
      if (problems == null) {
        System.out.println("All " + REQUIRED.length + " media files resolved.");
        for (String path : REQUIRED) {
          System.out.println("  " + Assets.resolve(path).getAbsolutePath());
        }
        System.exit(0);
      }
      System.err.println("Missing media:\n" + problems);
      System.exit(1);
    }

    final String missing = findMissing();

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        AppTheme.install();

        if (missing != null) {
          JOptionPane.showMessageDialog(null,
              "Project Memetent could not find its media files:\n\n" + missing
                  + "\nThe game folder is probably incomplete — try reinstalling.",
              "Missing media", JOptionPane.ERROR_MESSAGE);
          return;
        }

        new FirstPage().setVisible(true);
      }
    });
  }

  /** Returns a newline-separated list of missing files, or null when everything resolves. */
  private static String findMissing() {
    StringBuilder sb = new StringBuilder();
    for (String path : REQUIRED) {
      if (!Assets.exists(path)) sb.append("  • ").append(path).append('\n');
    }
    return sb.length() == 0 ? null : sb.toString();
  }
}
