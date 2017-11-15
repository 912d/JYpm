package com.github.open96.fxml;

import com.github.open96.download.DownloadManager;
import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.QUEUE_STATUS;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.settings.SettingsManager;
import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Class that represents single playlist entry in ListView that is displayed in main window of application.
 */
public class RootListCellController extends ListCell<Playlist> {

    private static Logger log = LogManager.getLogger(RootListCellController.class.getName());

    //Load elements from fxml file that have id and cast them to objects of their respective types

    @FXML
    private HBox rootHBox;
    @FXML
    private Label playlistNameLabel;
    @FXML
    private Label videoCountLabel;
    @FXML
    private Label currentStatusLabel;
    @FXML
    private MenuButton actionButton;
    @FXML
    private MenuItem deleteItem;
    @FXML
    private MenuItem updateItem;
    @FXML
    private MenuItem openItem;
    @FXML
    private ImageView thumbnailImageView;
    //In this FXMLLoader our listCell layout will be stored
    private FXMLLoader fxmlLoader;

    @Override
    protected void updateItem(Playlist playlist, boolean empty) {
        super.updateItem(playlist, empty);

        //In case of empty object listCell should remain empty too
        if (empty || playlist == null) {
            setText(null);
            setGraphic(null);
        } else {    //Else listCell should be populated with data from Playlist object
            if (fxmlLoader == null) {   //First, check if layout from .fxml has been loaded already and load it if it hasn't
                fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/rootListCell.fxml"));
                fxmlLoader.setController(this);
                try {
                    fxmlLoader.load();
                } catch (IOException e) {
                    log.error("Could not find .fxml file for RootListCellController. Make sure your project/application isn't corrupted", e);
                }

            }
            log.debug("Loading playlist " + playlist.getPlaylistName());

            //Now it's time to load values into their respective fields
            playlistNameLabel.setText(playlist.getPlaylistName());
            videoCountLabel.setText(playlist.getVideoCount() + " videos");

            //Load thumbnail asynchronously from main JavaFX thread
            thumbnailImageView.setImage(null);
            ThreadManager
                    .getInstance()
                    .sendVoidTask(new Thread(() -> {
                        Image thumbnailImage = new Image(playlist.getPlaylistThumbnailUrl());
                        if (ThreadManager.getExecutionPermission()) {
                            Platform.runLater(() -> thumbnailImageView.setImage(thumbnailImage));
                        }
                    }), TASK_TYPE.UI);

            ThreadManager
                    .getInstance()
                    .sendVoidTask(new Thread(() -> {
                        QUEUE_STATUS lastKnownState = QUEUE_STATUS.UNKNOWN;
                        while (ThreadManager.getExecutionPermission()) {
                            //Dirty cheat because JavaFX changes references to objects on listview update, so it is obligatory to make sure we are still operating on same object.
                            if (!playlistNameLabel.getText().equals(playlist.getPlaylistName())) {
                                break;
                            }
                            //Update status label on our ListCell every 1 second
                            try {
                                switch (PlaylistManager
                                        .getInstance()
                                        .getPlaylistByLink(playlist.getPlaylistLink()).getStatus()) {
                                    case QUEUED:
                                        if (lastKnownState != QUEUE_STATUS.QUEUED) {
                                            Platform.runLater(() -> currentStatusLabel.setText("In queue"));
                                            lastKnownState = QUEUE_STATUS.QUEUED;
                                        }
                                        Platform.runLater(() -> updateItem.setDisable(true));
                                        break;
                                    case DOWNLOADING:
                                        Integer currentCount = DownloadManager
                                                .getInstance()
                                                .getDownloadProgress();
                                        if (currentCount != null) {
                                            Platform.runLater(() -> currentStatusLabel.setText("Downloading (" + currentCount + "/" + playlist.getVideoCount() + ")"));
                                        }
                                        lastKnownState = QUEUE_STATUS.DOWNLOADING;
                                        Platform.runLater(() -> updateItem.setDisable(true));
                                        break;
                                    case DOWNLOADED:
                                        if (lastKnownState != QUEUE_STATUS.DOWNLOADED) {
                                            Platform.runLater(() -> currentStatusLabel.setText("Downloaded"));
                                            lastKnownState = QUEUE_STATUS.DOWNLOADED;
                                        }
                                        Platform.runLater(() -> updateItem.setDisable(false));
                                        break;
                                    case FAILED:
                                        if (lastKnownState != QUEUE_STATUS.FAILED) {
                                            Platform.runLater(() -> currentStatusLabel.setText("Error during downloading"));
                                            lastKnownState = QUEUE_STATUS.FAILED;
                                        }
                                        Platform.runLater(() -> updateItem.setDisable(false));
                                        break;
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    log.error("Thread has been interrupted", e);
                                }
                            } catch (NullPointerException e) {
                                //For same reason as cheat on top of this method - we have to catch NullPointerException
                                //in case when user deletes a list and terminate that thread
                                break;
                            }
                        }
                    }), TASK_TYPE.UI);

            //Set button behaviours
            deleteItem.setOnAction(actionEvent -> {
                try {
                    Stage subStage = new Stage();
                    subStage.setTitle("Playlist deletion");
                    subStage.getIcons().add(new Image("/icon/launcher-128-128.png"));
                    FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/dialogWindow.fxml"));
                    Parent root = fxmlLoader.load();
                    DialogWindowController controller = fxmlLoader.getController();

                    String message = "Do you want to delete all files linked to playlist or just a playlist entry?";
                    String positiveButtonText = "Delete all files";
                    String negativeButtonText = "Only delete entry in JYpm";
                    EventHandler<ActionEvent> positiveButtonEventHandler = event ->
                            ThreadManager
                                    .getInstance()
                                    .sendVoidTask(new Thread(() -> {
                                        PlaylistManager
                                                .getInstance()
                                                .getPlaylists().stream()
                                                .filter(playlist1 -> playlist1.getPlaylistLink().equals(playlist.getPlaylistLink()))
                                                .forEach(playlist1 -> {
                                                    Platform.runLater(() ->
                                                            PlaylistManager
                                                                    .getInstance()
                                                                    .getObservablePlaylists()
                                                                    .remove(playlist1));
                                                    ThreadManager
                                                            .getInstance()
                                                            .sendVoidTask(new Thread(() ->
                                                                    PlaylistManager
                                                                            .getInstance()
                                                                            .remove(playlist1, true)), TASK_TYPE.PLAYLIST);
                                                });
                                        Platform.runLater(() -> subStage.close());
                                    }), TASK_TYPE.UI);


                    EventHandler<ActionEvent> negativeButtonEventHandler = event ->
                            ThreadManager
                                    .getInstance()
                                    .sendVoidTask(new Thread(() -> {
                                        PlaylistManager
                                                .getInstance()
                                                .getPlaylists().stream()
                                                .filter(playlist1 -> playlist1.getPlaylistLink().equals(playlist.getPlaylistLink()))
                                                .forEach(playlist1 -> {
                                                    Platform.runLater(() ->
                                                            PlaylistManager
                                                                    .getInstance()
                                                                    .getObservablePlaylists()
                                                                    .remove(playlist1));
                                                    ThreadManager
                                                            .getInstance()
                                                            .sendVoidTask(new Thread(() ->
                                                                    PlaylistManager
                                                                            .getInstance()
                                                                            .remove(playlist1, false)), TASK_TYPE.PLAYLIST);
                                                });
                                        Platform.runLater(() -> subStage.close());
                                    }), TASK_TYPE.UI);


                    controller.setData(message, positiveButtonText, negativeButtonText, positiveButtonEventHandler, negativeButtonEventHandler);
                    Scene scene = new Scene(root);
                    subStage.setScene(scene);
                    subStage.show();
                    subStage.setAlwaysOnTop(true);
                    subStage.requestFocus();
                } catch (IOException e) {
                    log.error(e);
                }
            });

            updateItem.setOnAction(actionEvent -> ThreadManager
                    .getInstance()
                    .sendVoidTask(new Thread(() ->
                            DownloadManager
                                    .getInstance()
                                    .download(playlist)), TASK_TYPE.OTHER));

            openItem.setOnAction(actionEvent -> ThreadManager
                    .getInstance()
                    .sendVoidTask(new Thread(() -> {
                        try {
                            Runtime.getRuntime().exec(SettingsManager
                                    .getInstance()
                                    .getFileManagerCommand() + " .", null, new File(playlist.getPlaylistLocation()));
                        } catch (IOException e) {
                            log.error("Invalid file manager, check your settings", e);
                        }
                    }), TASK_TYPE.OTHER));

            setGraphic(rootHBox);
        }

    }
}
