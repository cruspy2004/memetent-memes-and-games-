import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Splits a container between a primary and a secondary component at a fixed ratio of the
 * available space, and collapses the secondary one entirely below a width breakpoint.
 *
 * This exists because the original layouts sized things in absolute pixels: the brick
 * breaker stacked a 700x700 field and two GIF panels in a BoxLayout (giving a ~1500px tall
 * window), and the dino game faked centring with {@code new Insets(0, WIDTH / 6, 0, 0)} —
 * a 200px margin that is only correct at exactly one window size. A ratio holds at every
 * size, and the breakpoint means a narrow window gives its whole width to the game instead
 * of squeezing both panes into something unplayable.
 */
class RatioSplit implements LayoutManager {

  static final String PRIMARY = "primary";
  static final String SECONDARY = "secondary";

  private final double primaryRatio;
  private final boolean horizontal;
  private final int gap;

  /** Below this container width (or height, when stacked) the secondary pane is hidden. */
  private int collapseBelow = 0;
  /** The secondary pane never renders thinner than this; the primary absorbs the rest. */
  private int secondaryMin = 0;
  /** ...nor wider than this, so a meme rail doesn't dominate an ultrawide monitor. */
  private int secondaryMax = Integer.MAX_VALUE;

  private Component primary;
  private Component secondary;

  RatioSplit(double primaryRatio, boolean horizontal, int gap) {
    this.primaryRatio = Theme.clamp(primaryRatio, 0.1, 0.95);
    this.horizontal = horizontal;
    this.gap = gap;
  }

  RatioSplit collapseBelow(int px) {
    this.collapseBelow = px;
    return this;
  }

  RatioSplit secondaryBounds(int min, int max) {
    this.secondaryMin = min;
    this.secondaryMax = max;
    return this;
  }

  /** True when the last layout pass hid the secondary pane. */
  boolean isCollapsed(Container parent) {
    int extent = horizontal ? parent.getWidth() : parent.getHeight();
    return secondary == null || extent < collapseBelow;
  }

  @Override
  public void addLayoutComponent(String name, Component comp) {
    if (SECONDARY.equals(name)) secondary = comp;
    else primary = comp;
  }

  @Override
  public void removeLayoutComponent(Component comp) {
    if (comp == primary) primary = null;
    if (comp == secondary) secondary = null;
  }

  @Override
  public Dimension preferredLayoutSize(Container parent) {
    return combine(parent, false);
  }

  @Override
  public Dimension minimumLayoutSize(Container parent) {
    return combine(parent, true);
  }

  private Dimension combine(Container parent, boolean minimum) {
    Insets in = parent.getInsets();
    Dimension a = size(primary, minimum);
    Dimension b = size(secondary, minimum);

    int along, across;
    if (horizontal) {
      along = a.width + b.width + (b.width > 0 ? gap : 0);
      across = Math.max(a.height, b.height);
    } else {
      along = a.height + b.height + (b.height > 0 ? gap : 0);
      across = Math.max(a.width, b.width);
    }

    return horizontal
        ? new Dimension(along + in.left + in.right, across + in.top + in.bottom)
        : new Dimension(across + in.left + in.right, along + in.top + in.bottom);
  }

  private Dimension size(Component c, boolean minimum) {
    if (c == null || !c.isVisible()) return new Dimension(0, 0);
    return minimum ? c.getMinimumSize() : c.getPreferredSize();
  }

  @Override
  public void layoutContainer(Container parent) {
    synchronized (parent.getTreeLock()) {
      Insets in = parent.getInsets();
      int x = in.left;
      int y = in.top;
      int w = parent.getWidth() - in.left - in.right;
      int h = parent.getHeight() - in.top - in.bottom;
      if (w <= 0 || h <= 0) return;

      int extent = horizontal ? w : h;
      boolean collapsed = secondary == null || extent < collapseBelow;

      if (secondary != null) secondary.setVisible(!collapsed);

      if (collapsed) {
        if (primary != null) primary.setBounds(x, y, w, h);
        return;
      }

      // Size the secondary pane from the leftover share, then clamp it, then give the
      // primary pane everything that is actually left. Clamping second keeps the primary
      // pane honest when the clamp bites.
      int usable = extent - gap;
      int secondaryExtent = (int) Math.round(usable * (1.0 - primaryRatio));
      secondaryExtent = Theme.clamp(secondaryExtent, Math.min(secondaryMin, usable / 2),
                                    Math.min(secondaryMax, usable));
      int primaryExtent = usable - secondaryExtent;

      if (horizontal) {
        primary.setBounds(x, y, primaryExtent, h);
        secondary.setBounds(x + primaryExtent + gap, y, secondaryExtent, h);
      } else {
        primary.setBounds(x, y, w, primaryExtent);
        secondary.setBounds(x, y + primaryExtent + gap, w, secondaryExtent);
      }
    }
  }
}
