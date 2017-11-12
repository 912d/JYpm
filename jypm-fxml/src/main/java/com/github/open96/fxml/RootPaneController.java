package com.github.open96.fxml;

import com.github.open96.fxml.util.UpdateWindow;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RootPaneController implements Initializable {

    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(RootPaneController.class.getName());

    @FXML
    private BorderPane rootPane;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            rootPane.setBottom(createStatusBar());
            rootPane.setCenter(createListView());
            //Display update window if update is available
            new UpdateWindow().runUpdater();
        } catch (IOException e) {
            log.error("Some .fxml files are corrupt or could not be loaded", e);
        }
    }

    /**
     * Creates Parent object that contains list of playlists managed by application.
     */
    private Parent createListView() throws IOException {
        return FXMLLoader.load(getClass().getResource("/fxml/rootListView.fxml"));
    }

    /**
     * Creates Parent object that contains notification bar.
     */
    private Parent createStatusBar() throws IOException {
        return FXMLLoader.load(getClass().getResource("/fxml/notificationBar.fxml"));
    }

}
