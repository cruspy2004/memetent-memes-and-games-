package jaco.mp3.player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import javazoom.jl.player.Player;

/**
 * Drop-in replacement for the jaco-mp3-player-0.9.4 class the game was written
 * against. That jar is no longer distributed anywhere, so this reimplements the
 * slice of its API the game uses (play/pause/stop over a playlist of files) on
 * top of JLayer, which is on Maven Central.
 *
 * Playback runs on a daemon thread so it never keeps the JVM alive.
 */
public class MP3Player {

  private final List<File> playList = new ArrayList<File>();

  private Thread playbackThread;
  private Player player;

  private boolean playing;
  private boolean repeat;
  /** Frame the current track was paused at, so resume picks up where it left off. */
  private int pausedFrame;

  public MP3Player() {
  }

  public MP3Player(File file) {
    addToPlayList(file);
  }

  public void addToPlayList(File file) {
    synchronized (playList) {
      playList.add(file);
    }
  }

  public List<File> getPlayList() {
    return playList;
  }

  public void setRepeat(boolean repeat) {
    this.repeat = repeat;
  }

  public boolean isRepeat() {
    return repeat;
  }

  public boolean isPlaying() {
    return playing;
  }

  /** Starts, or resumes after a pause. A second call while playing is a no-op. */
  public synchronized void play() {
    if (playing) return;
    playing = true;

    final int startFrame = pausedFrame;
    playbackThread = new Thread(new Runnable() {
      public void run() {
        stream(startFrame);
      }
    }, "MP3Player");
    playbackThread.setDaemon(true);
    playbackThread.start();
  }

  /** Halts playback, remembering the position so a later play() resumes it. */
  public synchronized void pause() {
    if (!playing) return;
    playing = false;
    if (player != null) {
      pausedFrame += player.getPosition() / 26; // ~26ms per MPEG frame
      player.close();
      player = null;
    }
  }

  /** Halts playback and rewinds to the start of the playlist. */
  public synchronized void stop() {
    playing = false;
    pausedFrame = 0;
    if (player != null) {
      player.close();
      player = null;
    }
  }

  private void stream(int startFrame) {
    do {
      List<File> snapshot;
      synchronized (playList) {
        snapshot = new ArrayList<File>(playList);
      }

      for (File file : snapshot) {
        if (!playing) return;
        if (!file.exists()) {
          System.err.println("MP3Player: file not found: " + file);
          continue;
        }

        BufferedInputStream in = null;
        try {
          in = new BufferedInputStream(new FileInputStream(file));
          Player p = new Player(in);
          synchronized (this) {
            if (!playing) return;
            player = p;
          }
          if (startFrame > 0) {
            p.play(startFrame); // skip past what was already heard
            startFrame = 0;
          }
          p.play(); // blocks until the track ends or close() is called
        } catch (Exception e) {
          // A decode error on one clip shouldn't take the game down.
          System.err.println("MP3Player: could not play " + file + ": " + e.getMessage());
        } finally {
          if (in != null) {
            try { in.close(); } catch (Exception ignored) { }
          }
        }

        if (!playing) return; // closed by pause()/stop()
      }
    } while (repeat && playing);

    synchronized (this) {
      playing = false;
      pausedFrame = 0;
    }
  }
}
