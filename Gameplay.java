//brick breacker

import jaco.mp3.player.MP3Player;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import utility.Assets;

/**
 * Brick breaker play field.
 *
 * Two things drove the rewrite:
 *
 * 1. Everything on screen is now derived from the panel's live width and height instead of
 *    the 700x700 constants the original assumed. Bricks, paddle, ball and text all re-flow
 *    on resize rather than clipping or stranding themselves off-screen.
 *
 * 2. Ball and paddle positions are stored as fractions of the field (0..1) rather than as
 *    pixels. A resize then moves them proportionally and gameplay speed stays consistent
 *    across window sizes, which pixel coordinates cannot do.
 *
 * Rendering moved from {@code paint()} to {@code paintComponent()}, and win/lose detection
 * moved out of the paint pass — the original mutated game state and started audio from
 * inside painting, which fires an unpredictable number of times per frame.
 */
public class Gameplay extends JPanel implements ActionListener {

  private enum State { READY, PLAYING, WON, LOST }

  private static final int ROWS = 4;
  private static final int COLS = 12;
  private static final int TICK_MS = 16; // ~60fps

  /** Ball speed in field-heights per second — matches the original's feel, resolution-free. */
  private static final double BALL_SPEED = 0.64;
  /** Paddle travel in field-widths per second while an arrow key is held. */
  private static final double PADDLE_SPEED = 1.15;
  /** Steepest bounce off the paddle edge, measured from vertical. */
  private static final double MAX_BOUNCE_ANGLE = Math.toRadians(60);

  private final Random random = new Random();
  private final Timer timer;

  private State state = State.READY;
  private int score;
  private int bricksLeft;

  // Ball centre and velocity, as fractions of the field.
  private double ballX = 0.30;
  private double ballY = 0.62;
  private double ballVx;
  private double ballVy;

  // Paddle centre, as a fraction of the field width.
  private double paddleX = 0.5;
  private boolean movingLeft;
  private boolean movingRight;

  private Mapgenerator map;
  private MemeRail rail;

  private final MP3Player hamsound = new MP3Player(Assets.audio("hamaster.mp3"));
  private final MP3Player chisound = new MP3Player(Assets.audio("chippi.mp3"));

  /** Wall-clock of the last tick, so movement is time-based rather than tick-count based. */
  private long lastTick;
  /** When the celebratory GIF should flip back to the idle one. */
  private long chippiUntil;

  public Gameplay() {
    setOpaque(true);
    setBackground(Theme.BG_DEEP);
    setFocusable(true);
    setFocusTraversalKeysEnabled(false);

    // A floor, not a fixed size. The window is free to make the field bigger.
    setMinimumSize(new java.awt.Dimension(420, 380));
    setPreferredSize(new java.awt.Dimension(820, 680));

    resetRound();
    installKeyBindings();

    timer = new Timer(TICK_MS, this);
    lastTick = System.nanoTime();
    timer.start();

    hamsound.setRepeat(true);
    hamsound.play();
  }

  void setMemeRail(MemeRail rail) {
    this.rail = rail;
    showHamster();
  }

  /**
   * Key bindings rather than a KeyListener. The original added a KeyListener to the panel
   * and depended on it holding focus; anything else taking focus left the paddle dead.
   * WHEN_IN_FOCUSED_WINDOW bindings keep working regardless.
   *
   * Tracking pressed/released state also gives smooth held-key movement instead of the
   * original's 20px jump per key event.
   */
  private void installKeyBindings() {
    Runnable leftDown = new Runnable() { public void run() { movingLeft = true; } };
    Runnable leftUp = new Runnable() { public void run() { movingLeft = false; } };
    Runnable rightDown = new Runnable() { public void run() { movingRight = true; } };
    Runnable rightUp = new Runnable() { public void run() { movingRight = false; } };
    Runnable serve = new Runnable() { public void run() { launchOrRestart(); } };

    for (int code : new int[] { KeyEvent.VK_LEFT, KeyEvent.VK_A }) {
      bind(code, false, leftDown);
      bind(code, true, leftUp);
    }
    for (int code : new int[] { KeyEvent.VK_RIGHT, KeyEvent.VK_D }) {
      bind(code, false, rightDown);
      bind(code, true, rightUp);
    }

    bind(KeyEvent.VK_ENTER, false, serve);
    bind(KeyEvent.VK_SPACE, false, serve);
  }

  /**
   * Binds one key event.
   *
   * Built from a key code rather than a descriptor string on purpose. The string form of
   * {@code KeyStroke.getKeyStroke} needs the keyword in lowercase — "released LEFT" — and
   * returns *null* for anything it cannot parse, which {@code InputMap.put} then discards
   * without complaint. Written as "RELEASED LEFT" it silently registered nothing, so the
   * paddle's movement flag was set on key-down and never cleared: pressing the other arrow
   * left both flags true and the paddle stopped dead.
   */
  private void bind(int keyCode, boolean onRelease, final Runnable action) {
    KeyStroke ks = KeyStroke.getKeyStroke(keyCode, 0, onRelease);
    if (ks == null) throw new IllegalStateException("unbindable key code " + keyCode);

    String key = "gameplay:" + keyCode + (onRelease ? ":up" : ":down");
    getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ks, key);
    getActionMap().put(key, new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { action.run(); }
    });
  }

  private void launchOrRestart() {
    if (state == State.WON || state == State.LOST) {
      resetRound();
      showHamster();
      return;
    }
    if (state == State.READY) {
      state = State.PLAYING;
      // Serve upward at a slight angle, side chosen at random so rounds differ.
      double angle = Math.toRadians(28 + random.nextInt(24));
      double dir = random.nextBoolean() ? 1 : -1;
      ballVx = Math.sin(angle) * BALL_SPEED * dir;
      ballVy = -Math.cos(angle) * BALL_SPEED;
    }
  }

  private void resetRound() {
    state = State.READY;
    score = 0;
    map = new Mapgenerator(ROWS, COLS);
    bricksLeft = ROWS * COLS;
    paddleX = 0.5;
    ballX = 0.5;
    ballY = 0.60;
    ballVx = 0;
    ballVy = 0;
    movingLeft = false;
    movingRight = false;
  }

  // ---------------------------------------------------------------- geometry
  // Every dimension below is a fraction of the current panel, which is what makes the
  // field responsive. Clamps keep things usable at extreme window sizes.

  private Rectangle brickArea() {
    int w = getWidth();
    int h = getHeight();
    int x = (int) (w * 0.06);
    int y = (int) (h * 0.17);
    return new Rectangle(x, y, w - x * 2, (int) (h * 0.30));
  }

  private int paddleWidth() {
    return Theme.clamp((int) (getWidth() * 0.16), 70, 300);
  }

  private int paddleHeight() {
    return Theme.clamp((int) (getHeight() * 0.020), 9, 20);
  }

  private int paddleTop() {
    return getHeight() - (int) (getHeight() * 0.085);
  }

  private int ballSize() {
    return Theme.clamp((int) (Math.min(getWidth(), getHeight()) * 0.030), 12, 30);
  }

  private Rectangle paddleRect() {
    int pw = paddleWidth();
    int px = (int) Math.round(paddleX * getWidth()) - pw / 2;
    px = Theme.clamp(px, 0, Math.max(0, getWidth() - pw));
    return new Rectangle(px, paddleTop(), pw, paddleHeight());
  }

  private Rectangle ballRect() {
    int d = ballSize();
    return new Rectangle((int) Math.round(ballX * getWidth()) - d / 2,
                         (int) Math.round(ballY * getHeight()) - d / 2, d, d);
  }

  // ---------------------------------------------------------------- update

  @Override
  public void actionPerformed(ActionEvent e) {
    long now = System.nanoTime();
    double dt = Math.min((now - lastTick) / 1_000_000_000.0, 0.05); // clamp after a stall
    lastTick = now;

    if (getWidth() <= 0 || getHeight() <= 0) return;

    map.layout(brickArea().x, brickArea().y, brickArea().width, brickArea().height);

    updatePaddle(dt);
    if (state == State.PLAYING) updateBall(dt);
    if (chippiUntil > 0 && System.currentTimeMillis() > chippiUntil) showHamster();

    repaint();
  }

  private void updatePaddle(double dt) {
    if (movingLeft == movingRight) return; // neither, or both cancelling out

    double halfPaddle = paddleWidth() / (2.0 * getWidth());
    paddleX += (movingRight ? 1 : -1) * PADDLE_SPEED * dt;
    // Clamp at the walls. The original wrapped the paddle to the opposite edge here, which
    // read as a teleport bug rather than a mechanic.
    paddleX = Theme.clamp(paddleX, halfPaddle, 1.0 - halfPaddle);

    // A held arrow key also serves the ball, so the game starts the moment you engage.
    if (state == State.READY) launchOrRestart();
  }

  private void updateBall(double dt) {
    // The field is not square, so a fraction-space velocity has to be converted through the
    // aspect ratio to keep the ball's on-screen path straight.
    double aspect = getWidth() / (double) Math.max(1, getHeight());
    ballX += (ballVx / aspect) * dt;
    ballY += ballVy * dt;

    double halfW = ballSize() / (2.0 * getWidth());
    double halfH = ballSize() / (2.0 * getHeight());

    // Side and top walls.
    if (ballX - halfW <= 0) {
      ballX = halfW;
      ballVx = Math.abs(ballVx);
    } else if (ballX + halfW >= 1) {
      ballX = 1 - halfW;
      ballVx = -Math.abs(ballVx);
    }
    if (ballY - halfH <= 0) {
      ballY = halfH;
      ballVy = Math.abs(ballVy);
    }

    bouncePaddle();
    hitBrick();

    // Below the paddle: round over.
    if (ballY - halfH > 1.0) {
      state = State.LOST;
      ballVx = 0;
      ballVy = 0;
      chisound.stop();
      chippiUntil = 0;
      showHamster();
    }
  }

  private void bouncePaddle() {
    if (ballVy <= 0) return; // travelling upward, nothing to hit

    Rectangle ball = ballRect();
    Rectangle paddle = paddleRect();
    if (!ball.intersects(paddle)) return;

    // Classic breakout deflection: where the ball lands across the paddle sets the exit
    // angle. The original just flipped Y and did `ballXdir = ballXdir + 1`, an accumulator
    // that drifts to a horizontal crawl or runs away entirely.
    double hit = ((ball.getCenterX() - paddle.getCenterX()) / (paddle.width / 2.0));
    hit = Theme.clamp(hit, -1.0, 1.0);
    double angle = hit * MAX_BOUNCE_ANGLE;

    ballVx = Math.sin(angle) * BALL_SPEED;
    ballVy = -Math.cos(angle) * BALL_SPEED;

    // Lift the ball clear so it cannot register a second hit next tick.
    ballY = (paddle.y - ball.height / 2.0) / getHeight();
  }

  private void hitBrick() {
    Rectangle ball = ballRect();

    for (int i = 0; i < map.rows(); i++) {
      for (int j = 0; j < map.cols(); j++) {
        if (map.map[i][j] <= 0) continue;

        Rectangle brick = map.brickBounds(i, j);
        if (!ball.intersects(brick)) continue;

        map.setBrickValue(0, i, j);
        score += 5;
        bricksLeft--;
        celebrate();

        // Deflect along whichever axis the ball entered from, judged by how deep the
        // overlap is on each side.
        Rectangle overlap = ball.intersection(brick);
        if (overlap.width < overlap.height) ballVx = -ballVx;
        else ballVy = -ballVy;

        if (bricksLeft <= 0) {
          state = State.WON;
          ballVx = 0;
          ballVy = 0;
          celebrate();
          chippiUntil = 0; // hold the win GIF until the next round
        }
        return; // one brick per tick, as before
      }
    }
  }

  private void celebrate() {
    if (rail == null) return;
    rail.show(BrickBreakerWindow.CARD_CHIPPI, "Chippi Chippa!");
    hamsound.pause();
    chisound.play();
    chippiUntil = System.currentTimeMillis() + 2000;
  }

  private void showHamster() {
    chippiUntil = 0;
    if (rail == null) return;
    if (!BrickBreakerWindow.CARD_HAMSTER.equals(rail.current())) {
      chisound.pause();
      hamsound.play();
    }
    rail.show(BrickBreakerWindow.CARD_HAMSTER, "Hampter is watching.");
  }

  /** Stops the timer and all audio. Called when the window closes. */
  void shutdown() {
    timer.stop();
    hamsound.stop();
    chisound.stop();
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

    // Backdrop.
    g2.setPaint(new java.awt.GradientPaint(0, 0, new Color(30, 35, 50), 0, h, Theme.BG_DEEP));
    g2.fillRect(0, 0, w, h);

    drawHud(g2, w, h);
    map.draw(g2);
    drawPaddle(g2);
    drawBall(g2);
    drawFrame(g2, w, h);

    if (state == State.READY) drawBanner(g2, w, h, Theme.ACCENT, "Ready", "Press SPACE or use ← → to start");
    if (state == State.WON) drawBanner(g2, w, h, Theme.SUCCESS, "You Won!", "Press ENTER to play again");
    if (state == State.LOST) drawBanner(g2, w, h, Theme.DANGER, "Game Over", "Press ENTER to play again");

    g2.dispose();
  }

  /** Score and progress in a header band, in a colour that actually contrasts the field. */
  private void drawHud(Graphics2D g2, int w, int h) {
    int pad = (int) (w * 0.06);
    int baseline = (int) (h * 0.095);

    g2.setFont(Theme.scaledFont(Font.BOLD, 13f, w));
    g2.setColor(Theme.TEXT_MUTED);
    g2.drawString("SCORE", pad, baseline - g2.getFontMetrics().getHeight());

    g2.setFont(Theme.scaledMono(Font.BOLD, 30f, w));
    g2.setColor(Theme.TEXT); // was Color.black on a near-black field — invisible
    g2.drawString(String.valueOf(score), pad, baseline + g2.getFontMetrics().getAscent() / 2);

    String bricks = bricksLeft + " / " + (ROWS * COLS);
    g2.setFont(Theme.scaledFont(Font.BOLD, 13f, w));
    FontMetrics fm = g2.getFontMetrics();
    g2.setColor(Theme.TEXT_MUTED);
    g2.drawString("BRICKS", w - pad - fm.stringWidth("BRICKS"), baseline - fm.getHeight());

    g2.setFont(Theme.scaledMono(Font.BOLD, 30f, w));
    fm = g2.getFontMetrics();
    g2.setColor(Theme.ACCENT);
    g2.drawString(bricks, w - pad - fm.stringWidth(bricks), baseline + fm.getAscent() / 2);
  }

  private void drawPaddle(Graphics2D g2) {
    Rectangle p = paddleRect();
    int arc = p.height;
    g2.setPaint(new java.awt.GradientPaint(p.x, p.y, Theme.TEXT, p.x, p.y + p.height, new Color(170, 185, 210)));
    g2.fill(new RoundRectangle2D.Float(p.x, p.y, p.width, p.height, arc, arc));
  }

  private void drawBall(Graphics2D g2) {
    Rectangle b = ballRect();
    // A soft halo so the ball stays findable against the brick wall.
    g2.setColor(new Color(126, 166, 255, 60));
    int glow = b.width / 2;
    g2.fill(new Ellipse2D.Float(b.x - glow / 2f, b.y - glow / 2f, b.width + glow, b.height + glow));

    g2.setPaint(new java.awt.GradientPaint(b.x, b.y, Color.WHITE, b.x, b.y + b.height, Theme.ACCENT));
    g2.fill(new Ellipse2D.Float(b.x, b.y, b.width, b.height));
  }

  /** A thin inner border, replacing the three grey bars the original drew at fixed pixels. */
  private void drawFrame(Graphics2D g2, int w, int h) {
    g2.setStroke(new java.awt.BasicStroke(2f));
    g2.setColor(Theme.BORDER);
    g2.drawRect(1, 1, w - 3, h - 3);
  }

  /**
   * Centred overlay text. Measured with FontMetrics rather than the original's guessed
   * pixel offsets, so it stays centred at any window size and any score length.
   */
  private void drawBanner(Graphics2D g2, int w, int h, Color accent, String title, String hint) {
    Theme.drawScrim(g2, w, h, 130);

    g2.setFont(Theme.scaledFont(Font.BOLD, 52f, w));
    g2.setColor(accent);
    FontMetrics fm = g2.getFontMetrics();
    Theme.drawCentered(g2, title, w / 2, h / 2);

    g2.setFont(Theme.scaledFont(Font.PLAIN, 19f, w));
    g2.setColor(Theme.TEXT);
    Theme.drawCentered(g2, hint, w / 2, h / 2 + fm.getHeight() / 2 + (int) (h * 0.05));
  }
}
