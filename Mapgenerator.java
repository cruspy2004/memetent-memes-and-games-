//brick breaker

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.RoundRectangle2D;

/**
 * The brick grid.
 *
 * The original hard-coded the whole layout: bricks were {@code 540/col} wide and
 * {@code 150/row} tall and were drawn at a fixed {@code (+80, +50)} offset, so the wall
 * stayed glued to a 700x700 field and drifted out of place the moment the window changed
 * size. Now the grid is told what rectangle it owns and divides that up, so it fills the
 * play area at any resolution.
 */
public class Mapgenerator {

  /** Kept public and named {@code map} because Gameplay iterates it directly. */
  public int map[][];

  private int originX;
  private int originY;
  private int brickWidth;
  private int brickHeight;

  public Mapgenerator(int row, int col) {
    map = new int[row][col];
    for (int i = 0; i < map.length; i++) {
      for (int j = 0; j < map[0].length; j++) {
        map[i][j] = 1;
      }
    }
  }

  public int rows() {
    return map.length;
  }

  public int cols() {
    return map[0].length;
  }

  /**
   * Re-flows the grid into the area the play field currently has for it. Called on every
   * resize, which is what makes the wall responsive.
   */
  public void layout(int areaX, int areaY, int areaW, int areaH) {
    originX = areaX;
    originY = areaY;
    brickWidth = Math.max(1, areaW / cols());
    brickHeight = Math.max(1, areaH / rows());

    // Absorb the integer-division remainder into the left margin so the wall stays
    // optically centred instead of leaving a ragged gap on the right.
    originX += (areaW - brickWidth * cols()) / 2;
  }

  public Rectangle brickBounds(int row, int col) {
    return new Rectangle(originX + col * brickWidth, originY + row * brickHeight,
                         brickWidth, brickHeight);
  }

  public void draw(Graphics2D g) {
    if (brickWidth <= 0 || brickHeight <= 0) return;

    // Inset each brick slightly so the gap between them is real space rather than a
    // same-coloured stroke drawn over the fill, which is how the old version faked it.
    int padX = Math.max(1, brickWidth / 22);
    int padY = Math.max(1, brickHeight / 10);
    int arc = Math.max(4, Math.min(brickWidth, brickHeight) / 4);

    for (int i = 0; i < map.length; i++) {
      for (int j = 0; j < map[0].length; j++) {
        if (map[i][j] <= 0) continue;

        Rectangle r = brickBounds(i, j);
        int x = r.x + padX;
        int y = r.y + padY;
        int w = r.width - padX * 2;
        int h = r.height - padY * 2;
        if (w <= 0 || h <= 0) continue;

        Color top = rowColor(i, 1.12f);
        Color bottom = rowColor(i, 0.86f);

        g.setPaint(new java.awt.GradientPaint(x, y, top, x, y + h, bottom));
        g.fill(new RoundRectangle2D.Float(x, y, w, h, arc, arc));

        // A light top edge reads as a bevel and keeps adjacent rows from merging.
        g.setStroke(new BasicStroke(Math.max(1f, h / 16f)));
        g.setColor(new Color(255, 255, 255, 46));
        g.draw(new RoundRectangle2D.Float(x, y, w, h, arc, arc));
      }
    }
  }

  /** Rows run through a warm-to-cool ramp so the wall has depth instead of being flat black. */
  private Color rowColor(int row, float brightness) {
    float hue = 0.02f + (row / (float) Math.max(1, rows())) * 0.62f;
    Color base = Color.getHSBColor(hue, 0.62f, 0.86f);
    return new Color(
        Theme.clamp((int) (base.getRed() * brightness), 0, 255),
        Theme.clamp((int) (base.getGreen() * brightness), 0, 255),
        Theme.clamp((int) (base.getBlue() * brightness), 0, 255));
  }

  public void setBrickValue(int value, int row, int col) {
    map[row][col] = value;
  }
}
