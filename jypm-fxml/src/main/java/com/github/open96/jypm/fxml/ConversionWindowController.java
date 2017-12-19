package com.github.open96.jypm.fxml;

import com.github.open96.jypm.ffmpeg.FILE_EXTENSION;
import com.github.open96.jypm.ffmpeg.FfmpegManager;
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
import java.util.ResourceBundle;

public class ConversionWindowController implements Initializable {

    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(DetailsWindowController.class.getName());


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
        //Create menu entries for both menu buttons
        for (FILE_EXTENSION f : FILE_EXTENSION.values()) {
            MenuItem menuItem = new MenuItem(f.toString());
            targetExtensionSplitMenuButton.getItems().addAll(menuItem);
        }

        for (Integer bitrate : FfmpegManager.availableBitrates) {
            MenuItem menuItem = new MenuItem(bitrate.toString());
            bitrateSplitMenuButton.getItems().addAll(menuItem);
        }

    }


    public void onConvertButtonClick(ActionEvent actionEvent) {

    }
}
