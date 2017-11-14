package com.github.open96.fxml;

import com.github.open96.fxml.util.UpdateWindow;
import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.QUEUE_STATUS;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.settings.OS_TYPE;
import com.github.open96.settings.SettingsManager;
import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import com.github.open96.updater.Updater;
import com.github.open96.youtubedl.EXECUTABLE_STATE;
import com.github.open96.youtubedl.YoutubeDlManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ResourceBundle;

public class SettingsWindowController implements Initializable {

    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(SettingsWindowController.class.getName());
    @FXML
    GridPane rootPane;
    @FXML
    Label fileManagerCommandLabel;
    @FXML
    Label executableVersionLabel;
    @FXML
    Label runtimeVersionLabel;
    @FXML
    Label notificationLabel;
    @FXML
    Button saveSettingsButton;
    @FXML
    Button restoreDefaultsButton;
    @FXML
    Button visitGitHubButton;
    @FXML
    Button updateYTDLButton;
    @FXML
    TextField fileManagerCommandTextField;
    @FXML
    CheckBox notificationCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Load data from SettingsManager if it exists
        String youtubeDlStringLocation = SettingsManager
                .getInstance()
                .getYoutubeDlExecutable();
        String runtimeVersion = SettingsManager
                .getInstance()
                .getRuntimeVersion();
        if (!runtimeVersion.equals("")) {
            runtimeVersionLabel.setText(runtimeVersion);
        }
        if (!youtubeDlStringLocation.equals("")) {
            executableVersionLabel.setText(SettingsManager
                    .getInstance()
                    .getYoutubeDlVersion());
        }
        fileManagerCommandTextField.setText(SettingsManager
                .getInstance()
                .getFileManagerCommand());
        if (SettingsManager
                .getInstance()
                .getNotificationPolicy()) {
            notificationCheckBox.setSelected(true);
        }

        //Prevent user from enforcing youtube-dl from updating while download is in progress
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    while (ThreadManager.getExecutionPermission()) {
                        boolean isDownloadInProgress = false;
                        for (Playlist p : PlaylistManager
                                .getInstance()
                                .getPlaylists()) {
                            if (p.getStatus() == QUEUE_STATUS.QUEUED || p.getStatus() == QUEUE_STATUS.DOWNLOADING) {
                                isDownloadInProgress = true;
                                break;
                            }
                        }
                        if (isDownloadInProgress) {
                            Platform.runLater(() -> updateYTDLButton.setDisable(true));
                        } else {
                            Platform.runLater(() -> updateYTDLButton.setDisable(false));
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            log.error("Thread has been interrupted.", e);
                        }
                    }
                }), TASK_TYPE.UI);
    }

    /**
     * Save data permanently and hide the window.
     */
    public void onSaveSettingsButtonClick(ActionEvent actionEvent) {
        log.debug("User changed settings from UI");
        SettingsManager
                .getInstance()
                .setFileManagerCommand(fileManagerCommandTextField.getText());
        SettingsManager
                .getInstance()
                .setNotificationPolicy(notificationCheckBox.isSelected());
        rootPane.getScene().getWindow().hide();
    }

    /**
     * Show user default settings
     */
    public void onRestoreDefaultsButtonClick(ActionEvent actionEvent) {
        if (SettingsManager
                .getInstance()
                .getOS() == OS_TYPE.WINDOWS) {
            fileManagerCommandTextField.setText("explorer");
        } else if (SettingsManager
                .getInstance()
                .getOS() == OS_TYPE.OPEN_SOURCE_UNIX) {
            fileManagerCommandTextField.setText("xdg-open");
        }
        notificationCheckBox.setSelected(true);
    }

    /**
     * Open github page of JYpm
     */
    public void onVisitGitHubButtonClick(ActionEvent actionEvent) {
        String githubURL = "https://github.com/Open96/JYpm";
        if (Desktop.isDesktopSupported()) {
            try {
                if (SettingsManager
                        .getInstance()
                        .getOS() == OS_TYPE.OPEN_SOURCE_UNIX) {
                    Runtime.getRuntime().exec("xdg-open " + githubURL, null);
                } else if (SettingsManager
                        .getInstance()
                        .getOS() == OS_TYPE.WINDOWS) {
                    Desktop.getDesktop().browse(new URI(githubURL));
                }

            } catch (IOException | URISyntaxException e) {
                log.error(e);
            } catch (UnsupportedOperationException e) {
                log.error("Browsing is not supported on this system");
            }
        }
    }

    public void onUpdateAppButtonClick(ActionEvent actionEvent) {
        Updater
                .getInstance()
                .refresh();
        new UpdateWindow().runUpdater();
    }


    public void onUpdateYTDLButtonClick(ActionEvent actionEvent) {
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    YoutubeDlManager
                            .getInstance()
                            .deletePreviousVersionIfExists();
                    YoutubeDlManager
                            .getInstance()
                            .downloadYoutubeDl();
                    Platform.runLater(() -> executableVersionLabel.setText("Downloading..."));
                    while (YoutubeDlManager
                            .getInstance()
                            .getExecutableState() == EXECUTABLE_STATE.NOT_READY) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            log.error("Thread has been interrupted.", e);
                        }
                    }
                    if (executableVersionLabel != null && executableVersionLabel.getText() != null) {
                        Platform.runLater(() -> executableVersionLabel.setText(SettingsManager
                                .getInstance()
                                .getYoutubeDlVersion()));
                    }
                }), TASK_TYPE.UI);
    }

}
