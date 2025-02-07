/* package project_memetent.Brick_Breacker;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class MediaPlayerPanel extends JPanel {
    // Update with the correct video file path and ensure it is an MP4 file
    private static final String VIDEO_URL = "Recording 2024-05-22 021808.mp4";

    public MediaPlayerPanel() {
        // Initialize JavaFX runtime
        JFXPanel fxPanel = new JFXPanel();
        this.setLayout(new BorderLayout());
        this.add(fxPanel, BorderLayout.SOUTH);
        this.setVisible(true);


        Platform.runLater(() -> {
            try {
                // Load video media
                File videoFile = new File(VIDEO_URL);
                if (!videoFile.exists()) {
                    throw new RuntimeException("Video file not found: " + VIDEO_URL);
                }

                Media videoMedia = new Media(videoFile.toURI().toString());
                MediaPlayer videoPlayer = new MediaPlayer(videoMedia);
                MediaView mediaView = new MediaView(videoPlayer);

                // Add the MediaView to the scene
                StackPane root = new StackPane();
                root.getChildren().add(mediaView);
                Scene scene = new Scene(root);


                fxPanel.setScene(scene);

                videoPlayer.setAutoPlay(true);
                videoPlayer.setOnError(() -> {
                    System.err.println("Error occurred: " + videoPlayer.getError().getMessage());
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
*/