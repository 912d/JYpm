package com.github.open96.fxml;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;

import java.net.URL;
import java.util.ResourceBundle;

public class DialogWindowController implements Initializable {

    @FXML
    GridPane rootPane;
    @FXML
    Button positiveButton;
    @FXML
    Button negativeButton;
    @FXML
    Text messageText;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        positiveButton.setDefaultButton(true);
        negativeButton.setCancelButton(true);
        negativeButton.setOnAction(event -> rootPane.getScene().getWindow().hide());
    }

    /**
     * Sets texts and button functions for this DialogWindow instance.
     *
     * @param message              Message that should be displayed in center of the window.
     * @param positiveButtonText   Text in button on the left.
     * @param negativeButtonText   Text in button on the right.
     * @param positiveButtonAction Functionality of button on the left.
     */
    public void setData(String message, String positiveButtonText, String negativeButtonText, EventHandler<ActionEvent> positiveButtonAction) {
        Platform.runLater(() -> {
            messageText.setText(message);
            positiveButton.setText(positiveButtonText);
            positiveButton.setOnAction(positiveButtonAction);
            negativeButton.setText(negativeButtonText);
        });
    }

    /**
     * Sets texts and button functions for this DialogWindow instance.
     *
     * @param message              Message that should be displayed in center of the window.
     * @param positiveButtonText   Text in button on the left.
     * @param negativeButtonText   Text in button on the right.
     * @param positiveButtonAction Functionality of button on the left.
     */
    public void setData(String message, String positiveButtonText, String negativeButtonText, EventHandler<ActionEvent> positiveButtonAction, EventHandler<ActionEvent> negativeButtonAction) {
        Platform.runLater(() -> {
            messageText.setText(message);
            positiveButton.setText(positiveButtonText);
            positiveButton.setOnAction(positiveButtonAction);
            negativeButton.setText(negativeButtonText);
            negativeButton.setOnAction(negativeButtonAction);
        });
    }

}
