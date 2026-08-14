//dinorun

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import components.Dino;
import components.Ground;
import components.Obstacles;
import jaco.mp3.player.MP3Player;
import utility.Assets;

/**
 * T-Rex run play field.
 *
 * The layout problem here was that the panel's size was captured into {@code static int
 * WIDTH/HEIGHT} at construction and never revisited, while the frame was subsequently
 * packed and maximised. Ground, dino and cacti were all positioned from those stale values
 * and the score was drawn at the real {@code getWidth()/2}, so the scene disagreed with
 * itself. A ComponentListener now re-lays-out the world on every resize.
 *
 * The animation loop also moved from a raw Thread calling {@code repaint()} to a Swing
 * Timer. The old loop mutated game state off the EDT while the paint pass read it, and
 * every press of space that reached the {@code animator == null || !running} branch started
 * another thread.
 */
class GamePanel extends JPanel implements ActionListener {

  private enum State { READY, RUNNING, DEAD }

  private static final int TICK_MS = 16; // ~60fps

  private final Ground ground;
  private final Dino dino;
  private final Obstacles obstacles;
  private final Timer timer;

  private final MP3Player player = new MP3Player(Assets.audio("gigachad.mp3"));

  private State state = State.READY;
  private double distance;
  private int score;
  private int highScore;

  private long lastTick;

  /** Notified on death/restart so the window can react (swap the meme rail). */
  private Runnable onDeath;
  private Runnable onRestart;

  GamePanel() {
    setOpaque(true);
    setBackground(Theme.BG_DEEP);
    setFocusable(true);
    setFocusTraversalKeysEnabled(false);

    // A floor and a hint. The original called setSize() — which a layout manager promptly
    // overrode — and never set a preferred size, so pack() computed 0x0 for this panel.
    setMinimumSize(new Dimension(480, 320));
    setPreferredSize(new Dimension(900, 620));

    ground = new Ground(900, 620);
    dino = new Dino();
    obstacles = new Obstacles(900);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        relayout();
      }
    });

    installKeyBindings();

    timer = new Timer(TICK_MS, this);
    lastTick = System.nanoTime();
    timer.start();
  }

  /** Re-derives every ground-relative position. The heart of the responsive fix. */
  private void relayout() {
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;
    ground.resize(w, h);   // sets Ground.GROUND_Y, which the other two read
    dino.resize(w, h);
    obstacles.resize(w);
  }

  GamePanel onDeath(Runnable r) {
    this.onDeath = r;
    return this;
  }

  GamePanel onRestart(Runnable r) {
    this.onRestart = r;
    return this;
  }

  private void installKeyBindings() {
    // Both SPACE and UP, and via key bindings rather than a KeyListener — the original
    // relied on keyTyped, which arrow keys never produce, and on the panel holding focus.
    bind("SPACE");
    bind("UP");
    bind("W");
  }

  private void bind(String stroke) {
    String key = "dino:" + stroke;
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke), key);
    getActionMap().put(key, new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { primaryAction(); }
    });
  }

  private void primaryAction() {
    switch (state) {
      case READY:
        start();
        break;
      case RUNNING:
        dino.jump();
        break;
      case DEAD:
        restart();
        break;
    }
  }

  private void start() {
    relayout();
    state = State.RUNNING;
    distance = 0;
    score = 0;
    dino.startRunning();
    obstacles.reset();
    player.setRepeat(true);
    player.play();
  }

  private void restart() {
    start();
    if (onRestart != null) onRestart.run();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    long now = System.nanoTime();
    double dt = Math.min((now - lastTick) / 1_000_000_000.0, 0.05);
    lastTick = now;

    if (state == State.RUNNING) {
      ground.update(dt);
      obstacles.update(dt);
      dino.update(dt);

      distance += dt * 100;
      score = (int) distance;

      if (obstacles.hasCollided(dino.getBounds(getWidth()))) {
        die();
      }
    }

    repaint();
  }

  private void die() {
    state = State.DEAD;
    dino.die();
    player.stop();
    highScore = Math.max(highScore, score);
    if (onDeath != null) onDeath.run();
  }

  /** Stops the loop and the audio. Called when the window closes. */
  void shutdown() {
    timer.stop();
    player.stop();
  }

  // ---------------------------------------------------------------- paint

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) return;

    Graphics2D g2 = (Graphics2D) g.create();
    Theme.enableQuality(g2);

    // Sky.
    g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(32, 38, 54), 0, h, new Color(18, 21, 30)));
    g2.fillRect(0, 0, w, h);

    ground.create(g2);
    obstacles.create(g2);
    dino.create(g2, w);

    drawHud(g2, w, h);

    if (state == State.READY) {
      drawBanner(g2, w, h, Theme.ACCENT, "T-Rex Run", "Press SPACE or ↑ to start");
    } else if (state == State.DEAD) {
      drawBanner(g2, w, h, Theme.DANGER, "Game Over", "Press SPACE to run again");
    }

    g2.dispose();
  }

  private void drawHud(Graphics2D g2, int w, int h) {
    int pad = (int) (w * 0.04);

    g2.setFont(Theme.scaledFont(Font.BOLD, 12f, w));
    FontMetrics small = g2.getFontMetrics();
    g2.setColor(Theme.TEXT_MUTED);

    String hiLabel = "HI";
    String hiValue = pad(highScore);
    String scoreValue = pad(score);

    g2.setFont(Theme.scaledMono(Font.BOLD, 22f, w));
    FontMetrics fm = g2.getFontMetrics();

    int right = w - pad;
    g2.setColor(Theme.TEXT);
    g2.drawString(scoreValue, right - fm.stringWidth(scoreValue), pad + fm.getAscent());

    int hiX = right - fm.stringWidth(scoreValue) - fm.stringWidth(hiValue) - small.stringWidth(hiLabel) - 28;
    g2.setColor(Theme.TEXT_MUTED);
    g2.drawString(hiValue, hiX + small.stringWidth(hiLabel) + 8, pad + fm.getAscent());

    g2.setFont(Theme.scaledFont(Font.BOLD, 12f, w));
    g2.drawString(hiLabel, hiX, pad + fm.getAscent());
  }

  /** Fixed-width score, so the HUD does not shuffle as digits roll over. */
  private String pad(int value) {
    String s = String.valueOf(value);
    while (s.length() < 5) s = "0" + s;
    return s;
  }

  private void drawBanner(Graphics2D g2, int w, int h, Color accent, String title, String hint) {
    Theme.drawScrim(g2, w, h, 120);

    g2.setFont(Theme.scaledFont(Font.BOLD, 48f, w));
    FontMetrics fm = g2.getFontMetrics();
    g2.setColor(accent);
    Theme.drawCentered(g2, title, w / 2, h / 2);

    g2.setFont(Theme.scaledFont(Font.PLAIN, 18f, w));
    g2.setColor(Theme.TEXT);
    Theme.drawCentered(g2, hint, w / 2, h / 2 + fm.getHeight() / 2 + (int) (h * 0.05));
  }
}
