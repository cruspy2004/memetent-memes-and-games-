import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The side rail that reacts to what is happening in the game.
 *
 * The GIFs sit in a CardLayout instead of being toggled with {@code setVisible()}. The old
 * code called {@code setVisible(false)} on siblings inside a BoxLayout, so every brick that
 * broke removed a component from the flow and made the game panel jump and resize
 * mid-volley. A card swap changes nothing about the layout — only what is painted.
 */
class MemeRail extends JPanel {

  private final CardLayout cards = new CardLayout();
  private final JPanel deck = new JPanel(cards);
  private final JLabel caption = new JLabel("", SwingConstants.CENTER);
  private final JLabel title = new JLabel("", SwingConstants.CENTER);

  private String current;

  MemeRail(String railTitle) {
    setLayout(new BorderLayout(0, 0));
    setOpaque(true);
    setBackground(Theme.BG_PANEL);
    setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER));

    title.setText(railTitle);
    title.setForeground(Theme.TEXT_MUTED);
    title.setFont(Theme.font(java.awt.Font.BOLD, 12f));
    title.setBorder(BorderFactory.createEmptyBorder(14, 12, 10, 12));

    deck.setOpaque(false);

    caption.setForeground(Theme.TEXT);
    caption.setFont(Theme.font(java.awt.Font.BOLD, 15f));
    caption.setBorder(BorderFactory.createEmptyBorder(10, 12, 16, 12));

    add(title, BorderLayout.NORTH);
    add(deck, BorderLayout.CENTER);
    add(caption, BorderLayout.SOUTH);

    // A floor only — the RatioSplit that owns us decides the real width.
    setMinimumSize(new Dimension(180, 200));
    setPreferredSize(new Dimension(320, 480));
  }

  /** Registers a card under a key, to be shown later with {@link #show}. */
  MemeRail addCard(String key, JComponent panel) {
    panel.setOpaque(false);
    deck.add(panel, key);
    if (current == null) current = key;
    return this;
  }

  /** Flips to a card. A no-op when that card is already showing, so this is cheap to spam. */
  void show(String key, String captionText) {
    if (key.equals(current)) {
      caption.setText(captionText);
      return;
    }
    current = key;
    cards.show(deck, key);
    caption.setText(captionText);
  }

  String current() {
    return current;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    Theme.enableQuality(g2);
    // A soft vertical wash so the rail reads as a distinct surface from the play field.
    g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.BG_PANEL, 0, getHeight(), Theme.BG_DEEP));
    g2.fillRect(0, 0, getWidth(), getHeight());
    g2.dispose();
  }
}
