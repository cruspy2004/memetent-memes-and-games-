import javax.swing.SwingUtilities;

/**
 * Standalone entry point for brick breaker, kept so the game can still be launched on its
 * own rather than only through the menu.
 *
 * The layout that used to live here now sits in {@link BrickBreakerWindow}; this class is
 * just the {@code main}.
 */
public class Main {

  public static void main(String[] args) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        AppTheme.install();
        new BrickBreakerWindow().show();
      }
    });
  }
}
