package com.github.open96.fxml;

import com.github.open96.api.github.pojo.release.ReleaseJSON;
import com.github.open96.settings.OS_TYPE;
import com.github.open96.settings.SettingsManager;
import com.github.open96.updater.Updater;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
            runUpdater();
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

    /**
     * Runs Updater class from jypm-updater and based on it's response it shows user that update is available.
     */
    private void runUpdater() throws IOException {
        if (Updater.getInstance().checkForUpdate() != null) {
            ReleaseJSON releaseJSON = Updater.getInstance().getJSONObject();
            //Create message
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("Update available").append("\n");
            messageBuilder.append(SettingsManager.getInstance().getRuntimeVersion()).append(" ").append("(Current)").append(" --> ")
                    .append(releaseJSON.getTagName()).append(" ").append("(").append(releaseJSON.getPublishedAt().substring(0, 10)).append(")")
                    .append("\n");
            messageBuilder.append("Visit").append(" ").append(releaseJSON.getHtmlUrl()).append(" ").append("for changelog and more details.")
                    .append("\n");

            //Windows users have executables, so they have to visit GitHub and download it manually for the time being
            String positiveButtonText = "Visit GitHub";
            String negativeButtonText = "Later";

            EventHandler<ActionEvent> positiveButtonEventHandler = event -> {
                if (Desktop.isDesktopSupported()) {
                    try {
                        if (SettingsManager.getInstance().getOS() == OS_TYPE.OPEN_SOURCE_UNIX) {
                            Runtime.getRuntime().exec("xdg-open " + releaseJSON.getHtmlUrl(), null);
                        } else if (SettingsManager.getInstance().getOS() == OS_TYPE.WINDOWS) {
                            Desktop.getDesktop().browse(new URI(releaseJSON.getHtmlUrl()));
                        }

                    } catch (IOException | URISyntaxException e) {
                        log.error(e);
                    } catch (UnsupportedOperationException e) {
                        log.error("Browsing is not supported on this system");
                    }
                }
            };

            Stage subStage = new Stage();
            subStage.setTitle("Update available");
            subStage.getIcons().add(new Image("/icon/launcher-128-128.png"));
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/dialogWindow.fxml"));
            Parent root = fxmlLoader.load();
            DialogWindowController controller = fxmlLoader.getController();
            controller.setData(messageBuilder.toString(), positiveButtonText, negativeButtonText, positiveButtonEventHandler);
            Scene scene = new Scene(root);
            subStage.setScene(scene);
            subStage.show();
            subStage.setAlwaysOnTop(true);
            subStage.requestFocus();
        }

    }

}
