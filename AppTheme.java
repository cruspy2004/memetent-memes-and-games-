import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;

/**
 * Process-wide UI setup, applied once before the first window is built.
 *
 * Swing's default cross-platform look and feel renders text without antialiasing and
 * ignores the desktop's DPI scaling, which is what made the original windows look coarse on
 * a modern display. Installing the system look and feel plus the text-antialiasing hints
 * fixes both, and costs nothing.
 */
final class AppTheme {

  private static boolean installed;

  private AppTheme() {
  }

  static synchronized void install() {
    if (installed) return;
    installed = true;

    // Must be set before any Swing class initialises to have an effect.
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
    System.setProperty("sun.java2d.uiScale.enabled", "true");

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      // Any look and feel will do; the games paint themselves regardless.
      System.err.println("AppTheme: falling back to the default look and feel (" + e.getMessage() + ")");
    }

    // Dialogs and frames pick up our palette rather than the platform grey.
    UIManager.put("OptionPane.background", Theme.BG_PANEL);
    UIManager.put("OptionPane.messageForeground", Theme.TEXT);
    UIManager.put("Panel.background", Theme.BG_PANEL);
    UIManager.put("ToolTip.background", Theme.BG_RAISED);
    UIManager.put("ToolTip.foreground", Theme.TEXT);

    JFrame.setDefaultLookAndFeelDecorated(false);
    JDialog.setDefaultLookAndFeelDecorated(false);
  }
}
