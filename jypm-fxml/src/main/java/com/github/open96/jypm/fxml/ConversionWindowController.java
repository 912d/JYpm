package com.github.open96.jypm.fxml;

import com.github.open96.jypm.ffmpeg.FILE_EXTENSION;
import com.github.open96.jypm.ffmpeg.FfmpegManager;
import com.github.open96.jypm.playlist.PLAYLIST_STATUS;
import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import com.github.open96.jypm.tray.TrayIcon;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ConversionWindowController implements Initializable {

    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(DetailsWindowController.class.getName());

    //Variables needed to start conversion process
    private Playlist playlist;


    @FXML
    GridPane rootPane;
    @FXML
    SplitMenuButton targetExtensionSplitMenuButton;
    @FXML
    SplitMenuButton bitrateSplitMenuButton;
    @FXML
    TextField customCommandTextField;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Disable command textfield and bitrate menubutton by default
        customCommandTextField.setDisable(true);
        bitrateSplitMenuButton.setDisable(true);

        //Create menu entries for both menu buttons
        for (FILE_EXTENSION f : FILE_EXTENSION.values()) {
            MenuItem menuItem = new MenuItem(f.toString());
            menuItem.setOnAction(actionEvent -> {
                targetExtensionSplitMenuButton.setText(menuItem.getText());
                customCommandTextField.setDisable(true);
                if (menuItem.getText().equals("MP3")) {
                    bitrateSplitMenuButton.setDisable(false);
                } else {
                    bitrateSplitMenuButton.setDisable(true);
                }
            });
            targetExtensionSplitMenuButton.getItems().addAll(menuItem);
        }

        for (Integer bitrate : FfmpegManager.availableBitrates) {
            MenuItem menuItem = new MenuItem(bitrate.toString());
            menuItem.setOnAction(actionEvent -> {
                bitrateSplitMenuButton.setText(menuItem.getText());
            });
            bitrateSplitMenuButton.getItems().addAll(menuItem);
        }

        //Also add "custom" option for targetExtensionSplitMenuButton
        MenuItem customCommandMenuItem = new MenuItem("Custom");
        customCommandMenuItem.setOnAction(actionEvent -> {
            targetExtensionSplitMenuButton.setText(customCommandMenuItem.getText());
            customCommandTextField.setDisable(false);
            bitrateSplitMenuButton.setDisable(true);
        });
        //For now i will leave it unavailable
        //targetExtensionSplitMenuButton.getItems().addAll(customCommandMenuItem);

    }


    public void onConvertButtonClick(ActionEvent actionEvent) {
        //Cast target extension to enum
        String targetExtension = targetExtensionSplitMenuButton.getText();
        FILE_EXTENSION extension = null;
        for (FILE_EXTENSION f : FILE_EXTENSION.values()) {
            if (f.toString().toLowerCase().equals(targetExtension.toLowerCase())) {
                extension = f;
            }
        }

        //Cast bitrate to Integer
        String targetBitrate = bitrateSplitMenuButton.getText();
        Integer bitrate = null;
        if (extension == FILE_EXTENSION.MP3) {
            for (Integer i : FfmpegManager.availableBitrates) {
                if (i.equals(Integer.valueOf(targetBitrate))) {
                    bitrate = i;
                }
            }
        }

        //Now do some validation and issue conversion task
        if (extension != FILE_EXTENSION.MP3) {
            bitrate = 0;
        }
        if (extension == FILE_EXTENSION.MP4 || (extension == FILE_EXTENSION.MP3 && bitrate != null)) {
            triggerConversion(extension, bitrate);
            //Close conversion window
            rootPane.getScene().getWindow().hide();
        }
    }


    private void triggerConversion(FILE_EXTENSION fileExtension, Integer bitrate) {
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            if (playlist.getStatus() == PLAYLIST_STATUS.DOWNLOADED) {
                //Start conversion
                playlist.setStatus(PLAYLIST_STATUS.CONVERTING);
                List<Boolean> conversionProgress = FfmpegManager
                        .getInstance()
                        .convertDirectory(playlist.getPlaylistLocation(), fileExtension, bitrate);
                //Wait until all videos have been converted
                int convertedVideos = 0;
                while (convertedVideos != conversionProgress.size()) {
                    try {
                        convertedVideos = conversionProgress
                                .stream()
                                .filter(conversionState -> conversionState.equals(Boolean.TRUE))
                                .toArray()
                                .length;
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LOG.error("Thread sleep has been interrupted!");
                    }
                }
                //Indicate that playlist has finished it's conversion
                playlist.setStatus(PLAYLIST_STATUS.DOWNLOADED);
            }
            //Display notification from tray
            if (TrayIcon.isTrayWorking()) {
                TrayIcon.getInstance().displayNotification("Conversion finished",
                        playlist.getPlaylistName() + " has been converted");
            }
        }), TASK_TYPE.CONVERSION);
    }


    public void setData(Playlist playlist) {
        this.playlist = playlist;
    }
}
