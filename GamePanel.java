//dinorun

import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import components.Ground;
import components.Dino;
import components.Obstacles;
import jaco.mp3.player.MP3Player;

class GamePanel extends JPanel implements KeyListener, Runnable {
  private GIFPanel gifPanel;
  public static int WIDTH;
  public static int HEIGHT;
  private Thread animator;

  private boolean running = false;
  private boolean gameOver = false;

  Ground ground;
  Dino dino;
  static Obstacles obstacles;

  private int score;
  MP3Player player = new MP3Player(new File("gigachad.mp3"));
  public GamePanel(int WIDTH, int HEIGHT) {
    this.WIDTH = WIDTH;
    this.HEIGHT = HEIGHT;

    ground = new Ground(HEIGHT);
    dino = new Dino();
    obstacles = new Obstacles((int)(WIDTH * 1.5));

    score = 0;

    setSize(WIDTH, HEIGHT);
    setVisible(true);
  }

  public void paint(Graphics g) {
    super.paint(g);
    g.setFont(new Font("Courier New", Font.BOLD, 25));
    g.drawString(Integer.toString(score), getWidth()/2 - 5, 100);
    ground.create(g);
    dino.create(g);
    obstacles.create(g);
  }

  public void run() {
    running = true;
    player.play();
    while(running) {
      updateGame();
      repaint();

      try {
        Thread.sleep(20);
      } catch(InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void updateGame() {
    score += 1;

    ground.update();
    //dino.update();
    obstacles.update();

    if(obstacles.hasCollided()) {
      dino.die();
      player.stop();
      repaint();
      gifPanel.setVisible(false);
      running = false;
      gameOver = true;
      System.out.println("collide");
    }
    // game complete condition
  }

  public void reset() {
    score = 0;
    player.play();
    System.out.println("reset");
    obstacles.resume();
    gifPanel.setVisible(true);
    gameOver = false;
  }

  public void keyTyped(KeyEvent e) {
    // System.out.println(e);
    if(e.getKeyChar() == ' ') {
      if(gameOver) reset();
      if (animator == null || !running) {
        System.out.println("Game starts");
        animator = new Thread(this);
        animator.start();
        dino.startRunning();
      } else {
        dino.jump();
      }
    }
  }

  public void keyPressed(KeyEvent e) {
    // System.out.println("keyPressed: "+e);
  }

  public void keyReleased(KeyEvent e) {
    // System.out.println("keyReleased: "+e);
  }
  public void setGIFPanel(GIFPanel gifPanel) {
    this.gifPanel = gifPanel;
  }

}