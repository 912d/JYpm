package com.github.open96.fxml;

import com.github.open96.download.DownloadManager;
import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.settings.SettingsManager;
import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
            ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
                while (ThreadManager.getExecutionPermission()) {
                    try {
                        if (SettingsManager.getInstance().checkInternetConnection()) {
                            Platform.runLater(() -> thumbnailImageView.setImage(new Image(playlist.getPlaylistThumbnailUrl())));
                            break;
                        }
                        Thread.sleep(1000 * 3); //3 seconds
                    } catch (InterruptedException e) {
                        log.error("Thread has been interrupted", e);
                    }
                }
            }), TASK_TYPE.UI);


            ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
                while (ThreadManager.getExecutionPermission()) {
                    //Dirty cheat because JavaFX changes references to objects on listview update, so it is obligatory to make sure we are still operating on same object.
                    if (!playlistNameLabel.getText().equals(playlist.getPlaylistName())) {
                        break;
                    }
                    //Update status label on our ListCell every 1 second
                    try {
                        switch (PlaylistManager.getInstance().getPlaylistByLink(playlist.getPlaylistLink()).getStatus()) {
                            case QUEUED:
                                Platform.runLater(() -> currentStatusLabel.setText("In queue"));
                                break;
                            case DOWNLOADING:
                                Integer currentCount = DownloadManager.getInstance().getDownloadProgress();
                                if (currentCount != null) {
                                    Platform.runLater(() -> currentStatusLabel.setText("Downloading (" + currentCount + "/" + playlist.getVideoCount() + ")"));
                                }
                                break;
                            case DOWNLOADED:
                                Platform.runLater(() -> currentStatusLabel.setText("Downloaded"));
                                break;
                            case FAILED:
                                Platform.runLater(() -> currentStatusLabel.setText("Error during downloading"));
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
                log.trace("Proceeding to remove " + playlist.getPlaylistName());
                PlaylistManager.getInstance().getPlaylists().stream()
                        .filter(playlist1 -> playlist1.getPlaylistLink().equals(playlist.getPlaylistLink()))
                        .forEach(playlist1 -> {
                            PlaylistManager.getInstance().getObservablePlaylists().remove(playlist1);
                            PlaylistManager.getInstance().remove(playlist1);
                        });
            });

            updateItem.setOnAction(actionEvent -> {
                DownloadManager.getInstance().download(playlist);
            });

            openItem.setOnAction(actionEvent -> {
                Thread locationOpener = new Thread(() -> {
                    try {
                        Runtime.getRuntime().exec(SettingsManager.getInstance().getFileManagerCommand() + " .", null, new File(playlist.getPlaylistLocation()));
                    } catch (IOException e) {
                        log.error("Invalid file manager, check your settings", e);
                    }
                });
                locationOpener.start();
            });

            setGraphic(rootHBox);
        }

    }
}
