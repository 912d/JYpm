package com.github.open96.jypm.fxml.util;

import com.github.open96.jypm.api.github.pojo.release.ReleaseJSON;
import com.github.open96.jypm.fxml.DialogWindowController;
import com.github.open96.jypm.settings.OS_TYPE;
import com.github.open96.jypm.settings.SettingsManager;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import com.github.open96.jypm.updater.Updater;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Calling this class with runUpdater() method will display update window if JYPM update is available
 */
public class UpdateWindow {

    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(UpdateWindow.class.getName());

    /**
     * Runs Updater class from jypm-updater and based on it's response it shows user that update is available.
     */
    public void runUpdater() {
        if (Updater.getInstance().checkForUpdate() != null) {
            ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
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
                            LOG.error(e);
                        } catch (UnsupportedOperationException e) {
                            LOG.error("Browsing is not supported on this system");
                        }
                    }
                };
                //Display update window
                ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
                    Platform.runLater(() -> {
                        try {
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
                        } catch (IOException e) {
                            LOG.error("Failed to load dialogWindow.fxml", e);
                        }
                    });
                }), TASK_TYPE.UI);
            }), TASK_TYPE.OTHER);
        }
    }

}
