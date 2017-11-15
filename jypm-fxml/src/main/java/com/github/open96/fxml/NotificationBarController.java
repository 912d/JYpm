package com.github.open96.fxml;

import com.github.open96.download.DownloadManager;
import com.github.open96.internetconnection.ConnectionChecker;
import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import com.github.open96.youtubedl.EXECUTABLE_STATE;
import com.github.open96.youtubedl.YoutubeDlManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.concurrent.RejectedExecutionException;

public class NotificationBarController implements Initializable {

    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(NotificationBarController.class.getName());
    //Launcher icon
    private static final Image LAUNCHER_ICON = new Image("/icon/launcher-128-128.png");

    @FXML
    GridPane rootPane;

    @FXML
    Button syncButton;

    @FXML
    Button settingsButton;

    @FXML
    Button addPlaylistButton;

    @FXML
    Label notificationText;

    @FXML
    Button detailsButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Start a thread that handles displaying messages on bottom bar of main application window
        startNotifierThread();
    }


    /**
     * Creates small window dialog that will guide user through playlist addition process
     */
    public void onAddPlaylistButtonClick(ActionEvent actionEvent) {
        //Create new stage and set its title
        Stage subStage = new Stage();
        subStage.setTitle("Add new playlist");

        //Make window unable to be resized
        subStage.setResizable(true);

        //Set window icon
        subStage.getIcons().add(LAUNCHER_ICON);

        try {
            Parent window = FXMLLoader.load(getClass().getResource("/fxml/addPlaylistWindow.fxml"));

            //Create a scene, add FXML layout to it.
            Scene scene = new Scene(window);

            //Finally, show the window to user.
            subStage.setScene(scene);
        } catch (IOException e) {
            LOG.error("Some .fxml files are corrupt or could not be loaded", e);
        }

        subStage.show();
        subStage.setAlwaysOnTop(true);

        //Also request focus on that stage
        subStage.requestFocus();
    }


    /**
     * Shows settings window to user
     */
    public void onSettingsButtonClick(ActionEvent actionEvent) {
        //Create new stage and set its title
        Stage subStage = new Stage();
        subStage.setTitle("Settings");

        //Make window unable to be resized
        subStage.setResizable(true);

        //Set window icon
        subStage.getIcons().add(LAUNCHER_ICON);

        try {
            Parent window = FXMLLoader.load(getClass().getResource("/fxml/settingsWindow.fxml"));

            //Create a scene, add FXML layout to it.
            Scene scene = new Scene(window);

            //Finally, show the window to user.
            subStage.setScene(scene);
        } catch (IOException e) {
            LOG.error("Some .fxml files are corrupt or could not be loaded", e);
        }

        subStage.show();
        subStage.setAlwaysOnTop(true);

        //Also request focus on that stage
        subStage.requestFocus();
    }

    /**
     * Shows details window to user
     */
    public void onDetailsButtonClick(ActionEvent event) {
        //Create new stage and set its title
        Stage subStage = new Stage();
        subStage.setTitle("Details");

        //Make window unable to be resized
        subStage.setResizable(true);

        //Set window icon
        subStage.getIcons().add(LAUNCHER_ICON);

        try {
            Parent window = FXMLLoader.load(getClass().getResource("/fxml/detailsWindow.fxml"));

            //Create a scene, add FXML layout to it.
            Scene scene = new Scene(window);

            //Override close button behaviour
            subStage.setOnCloseRequest(windowEvent -> {
                subStage.close();
                new Thread(() -> {
                    DetailsWindowController.threadKiller = true;
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    DetailsWindowController.threadKiller = false;
                }).start();
            });

            //Finally, show the window to user.
            subStage.setScene(scene);
        } catch (IOException e) {
            LOG.error("Some .fxml files are corrupt or could not be loaded", e);
        }

        subStage.show();

        //Also request focus on that stage
        subStage.requestFocus();
    }


    /**
     * Forces all playlists to be redownloaded.
     */
    public void onSyncButtonClick(ActionEvent actionEvent) {
        PlaylistManager
                .getInstance()
                .getPlaylists()
                .forEach(playlist -> DownloadManager
                        .getInstance()
                        .download(playlist));
    }

    private void startNotifierThread() {
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    while (ThreadManager.getExecutionPermission()) {
                        try {
                            Platform.runLater(() -> notificationText.setText(""));
                            if (!ConnectionChecker
                                    .getInstance()
                                    .checkInternetConnection()) {
                                Platform.runLater(() -> notificationText.setText("Waiting for internet connection..."));
                            }
                            if (YoutubeDlManager
                                    .getInstance()
                                    .getExecutableState() == EXECUTABLE_STATE.NOT_READY) {
                                Platform.runLater(() -> notificationText.setText("Looking for youtube-dl executable..."));
                            }
                            int queued = 0;
                            boolean isDownloadInProgress = false;
                            for (Playlist p : PlaylistManager
                                    .getInstance()
                                    .getPlaylists()) {
                                switch (p.getStatus()) {
                                    case DOWNLOADING:
                                        isDownloadInProgress = true;
                                        break;
                                    case QUEUED:
                                        queued++;
                                        break;
                                }
                            }
                            if (isDownloadInProgress) {
                                if (queued == 0) {
                                    Platform.runLater(() -> notificationText.setText("Downloading"));
                                } else {
                                    int finalQueued = queued;
                                    Platform.runLater(() -> notificationText.setText("Downloading (" + finalQueued + " in queue)"));
                                }
                            }
                            Thread.sleep(1000);
                        } catch (RejectedExecutionException e) {
                            break;
                        } catch (InterruptedException e) {
                            LOG.error("Thread has been interrupted", e);
                        }
                    }
                }), TASK_TYPE.UI);
    }

}
