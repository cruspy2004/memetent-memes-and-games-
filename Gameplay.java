//brick breacker

import jaco.mp3.player.MP3Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.Random;

public class Gameplay extends JPanel implements KeyListener, ActionListener {
    Random random = new Random();
    private boolean play = false;
    private int score = 0;
    private int totalBricks = 48;
    private Timer timer;
    private Timer visibilityTimer; // Timer for visibility changes
    private int delay = 5;
    private int playerX = 310;
    private int ballposX = 120;
    private int ballposY = 350;
    private int ballXdir = -1;
    private int ballYdir = -2;
    private Mapgenerator map;
    private GIFPanel hamPanel;
    private GIFPanel chiPanel;
    MP3Player hamsound = new MP3Player(new File("hamaster.mp3"));
    MP3Player chisound = new MP3Player(new File("chippi.mp3"));

    public Gameplay() {
        map = new Mapgenerator(4, 12);
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        timer = new Timer(delay, this);
        timer.start();
        setPreferredSize(new Dimension(700, 700));
        hamsound.play();
        // Set preferred size to ensure it fits within the frame
    }

    public void setHamPanel(GIFPanel hamPanel) {
        this.hamPanel = hamPanel;
        hamPanel.setVisible(true);
    }

    public void setChiPanel(GIFPanel chiPanel) {
        this.chiPanel = chiPanel;
        chiPanel.setVisible(false);
    }

    public void paint(Graphics g) {
        super.paint(g); // Call super.paint to ensure the panel is properly rendered

        // background
        g.setColor(new Color(52, 59, 64));
        g.fillRect(1, 1, getWidth(), getHeight());

        // drawing map
        map.draw((Graphics2D) g);

        // borders
        g.setColor(Color.gray);
        g.fillRect(0, 0, 3, getHeight());
        g.fillRect(0, 0, getWidth(), 3);
        g.fillRect(getWidth() - 3, 0, 3, getHeight());

        // the scores
        g.setColor(Color.black);
        g.setFont(new Font("serif", Font.BOLD, 25));
        g.drawString("" + score, getWidth() - 100, 30);

        // the paddle
        g.setColor(Color.black);
        g.fillRect(playerX, getHeight() - 50, 100, 8);

        // the ball
        g.setColor(Color.black);
        g.fillOval(ballposX, ballposY, 20, 20);

        // when you won the game
        if (totalBricks <= 0) {
            play = false;
            ballXdir = 0;
            ballYdir = 0;
            g.setColor(Color.ORANGE);
            g.setFont(new Font("serif", Font.BOLD, 30));
            g.drawString("You Won", getWidth() / 2 - 75, getHeight() / 2);

            g.setColor(Color.ORANGE);
            g.setFont(new Font("serif", Font.BOLD, 20));
            g.drawString("Press (Enter) to Restart", getWidth() / 2 - 100, getHeight() / 2 + 50);
            hamPanel.setVisible(false);
            hamsound.stop();
            chiPanel.setVisible(true);
            chisound.play();

        }

        // when you lose the game
        if (ballposY > getHeight() - 30) {
            play = false;
            ballXdir = 0;
            ballYdir = 0;
            g.setColor(Color.RED);
            g.setFont(new Font("serif", Font.BOLD, 30));
            g.drawString("Game Over, Scores: " + score, getWidth() / 2 - 150, getHeight() / 2);

            g.setColor(Color.RED);
            g.setFont(new Font("serif", Font.BOLD, 20));
            g.drawString("Press (Enter) to Restart", getWidth() / 2 - 100, getHeight() / 2 + 50);
            hamPanel.setVisible(true);
            chiPanel.setVisible(false);
            chisound.pause();
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            if (playerX >= getWidth() - 110) {
                playerX = 0;
            } else {
                moveRight();
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            if (playerX < 10) {
                playerX = getWidth() - 110;
            } else {
                moveLeft();
            }
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (!play) {
                play = true;
                ballposX = random.nextInt(getWidth() - 20);
                ballposY = random.nextInt(300) + 300;
                ballXdir = -1;
                ballYdir = -2;
                playerX = getWidth() / 2 - 50;
                score = 0;
                totalBricks = 48;
                map = new Mapgenerator(4, 12);

                repaint();
            }
        }
    }

    public void keyReleased(KeyEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public void moveRight() {
        play = true;
        playerX += 20; // Increase movement speed
    }

    public void moveLeft() {
        play = true;
        playerX -= 20; // Increase movement speed
    }

    public void actionPerformed(ActionEvent e) {
        timer.start();
        if (play) {
            if (new Rectangle(ballposX, ballposY, 20, 20).intersects(new Rectangle(playerX, getHeight() - 50, 100, 8))) {
                ballYdir = -ballYdir;
                ballXdir = ballXdir + 1;
            }

            // check map collision with the ball
            A:
            for (int i = 0; i < map.map.length; i++) {
                for (int j = 0; j < map.map[0].length; j++) {
                    if (map.map[i][j] > 0) {
                        int brickX = j * map.brickWidth + 80;
                        int brickY = i * map.brickHeight + 50;
                        int brickWidth = map.brickWidth;
                        int brickHeight = map.brickHeight;

                        Rectangle rect = new Rectangle(brickX, brickY, brickWidth, brickHeight);
                        Rectangle ballRect = new Rectangle(ballposX, ballposY, 20, 20);
                        Rectangle brickRect = rect;

                        if (ballRect.intersects(brickRect)) {
                            map.setBrickValue(0, i, j);
                            score += 5;
                            totalBricks--;
                            showChiPanel(); // Show chiPanel and hide hamPanel for 2 seconds
                            // when ball hit right or left of brick
                            if (ballposX + 19 <= brickRect.x || ballposX + 1 >= brickRect.x + brickRect.width) {
                                ballXdir = -ballXdir;
                            }
                            // when ball hits top or bottom of brick
                            else {
                                ballYdir = -ballYdir;
                            }

                            break A;
                        }
                    }
                }
            }

            ballposX += ballXdir;
            ballposY += ballYdir;

            if (ballposX < 0) {
                ballXdir = -ballXdir;
            }
            if (ballposY < 0) {
                ballYdir = -ballYdir;
            }
            if (ballposX > getWidth() - 20) {
                ballXdir = -ballXdir;
            }

            repaint();
        }
    }

    private void showChiPanel() {

        hamPanel.setVisible(false);
        hamsound.stop();
        chiPanel.setVisible(true);
        chisound.play();

        if (visibilityTimer != null) {
            visibilityTimer.stop();
        }

        visibilityTimer = new Timer(2000, new ActionListener() { // 2 seconds delay
            @Override
            public void actionPerformed(ActionEvent e) {

                chiPanel.setVisible(false);
                chisound.pause();
                hamPanel.setVisible(true);
                hamsound.play();
                visibilityTimer.stop();
            }
        });
        visibilityTimer.setRepeats(false); // Execute only once
        visibilityTimer.start();
    }

    public void setTimerDelay(int newDelay) {
        delay = newDelay;
        timer.setDelay(delay);
    }
}
