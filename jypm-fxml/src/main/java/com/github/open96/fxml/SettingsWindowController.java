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
    Label notificationLabel;
    @FXML
    Button saveSettingsButton;
    @FXML
    TextField fileManagerCommandTextField;
    @FXML
    CheckBox notificationCheckBox;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Load data from SettingsManager if it exists
        String youtubeDlStringLocation = SettingsManager.getInstance().getYoutubeDlExecutable();
        if (!youtubeDlStringLocation.equals("")) {
            executableVersionLabel.setText(SettingsManager.getInstance().getYoutubeDlVersion());
        } else {
            executableVersionLabel.setText("None");
        }
        if (SettingsManager.getInstance().getOS() == OS_TYPE.WINDOWS) {
            fileManagerCommandTextField.setText("explorer");
            fileManagerCommandTextField.setDisable(true);
        } else {
            if (!SettingsManager.getInstance().getFileManagerCommand().equals("")) {
                fileManagerCommandTextField.setText(SettingsManager.getInstance().getFileManagerCommand());
            } else {
                fileManagerCommandTextField.setText("");
            }
        }
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


}
