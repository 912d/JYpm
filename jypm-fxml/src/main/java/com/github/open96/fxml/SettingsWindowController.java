package com.github.open96.fxml;

import com.github.open96.settings.OS_TYPE;
import com.github.open96.settings.SettingsManager;
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
    TextField fileManagerCommandTextField;
    @FXML
    CheckBox notificationCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Load data from SettingsManager if it exists
        String youtubeDlStringLocation = SettingsManager.getInstance().getYoutubeDlExecutable();
        String runtimeVersion = SettingsManager.getInstance().getRuntimeVersion();
        if (!runtimeVersion.equals("")) {
            runtimeVersionLabel.setText(runtimeVersion);
        }
        if (!youtubeDlStringLocation.equals("")) {
            executableVersionLabel.setText(SettingsManager.getInstance().getYoutubeDlVersion());
        }
        fileManagerCommandTextField.setText(SettingsManager.getInstance().getFileManagerCommand());
        if (SettingsManager.getInstance().getNotificationPolicy()) {
            notificationCheckBox.setSelected(true);
        }
    }

    /**
     * Save data permanently and hide the window.
     */
    public void onSaveSettingsButtonClick(ActionEvent actionEvent) {
        log.debug("User changed settings from UI");
        SettingsManager.getInstance().setFileManagerCommand(fileManagerCommandTextField.getText());
        SettingsManager.getInstance().setNotificationPolicy(notificationCheckBox.isSelected());
        rootPane.getScene().getWindow().hide();
    }

    /**
     * Show user default settings
     */
    public void onRestoreDefaultsButtonClick(ActionEvent actionEvent) {
        if (SettingsManager.getInstance().getOS() == OS_TYPE.WINDOWS) {
            fileManagerCommandTextField.setText("explorer");
        } else if (SettingsManager.getInstance().getOS() == OS_TYPE.OPEN_SOURCE_UNIX) {
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
                if (SettingsManager.getInstance().getOS() == OS_TYPE.OPEN_SOURCE_UNIX) {
                    Runtime.getRuntime().exec("xdg-open " + githubURL, null);
                } else if (SettingsManager.getInstance().getOS() == OS_TYPE.WINDOWS) {
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

    }


    public void onUpdateYTDLButtonClick(ActionEvent actionEvent) {

    }

}
