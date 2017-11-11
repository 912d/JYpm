package com.github.open96.updater;

import com.github.open96.api.github.GitHubApiClient;
import com.github.open96.api.github.GitHubApiEndpointInterface;
import com.github.open96.api.github.pojo.release.ReleaseJSON;
import com.github.open96.fxml.DialogWindowController;
import com.github.open96.settings.OS_TYPE;
import com.github.open96.settings.SettingsManager;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

public class Updater {
    //This object is a singleton thus storing instance of it is needed
    private static Updater singletonInstance;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(Updater.class.getName());
    //Field to store ready for querying retrofit client
    private GitHubApiEndpointInterface apiService;
    private String runtimeVersion;
    private ReleaseJSON releaseJSON;

    private Updater() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of Updater.
     */
    public static Updater getInstance() {
        if (singletonInstance == null) {
            log.debug("Instance is null, initializing...");
            singletonInstance = new Updater();
        }
        return singletonInstance;
    }

    /**
     * Initialize subcomponents on first instance creation.
     */
    private void init() {
        log.trace("Initializing Updater");

        //Get runtime version
        try {
            Properties properties = new Properties();
            properties.load(Updater.class.getClassLoader().getResourceAsStream("version.properties"));
            runtimeVersion = properties.getProperty("runtime.version");
        } catch (IOException e) {
            log.error("Missing or corrupt version.properties file!", e);
        }

        SettingsManager.getInstance().setRuntimeVersion(runtimeVersion);

        //Initialize GitHubApiClient
        Retrofit retrofit = GitHubApiClient.getInstance().getClient();
        apiService = retrofit.create(GitHubApiEndpointInterface.class);

        //Query API for releases
        refresh();

        log.debug("Updater has been successfully initialized.");
    }

    /**
     * Checks if JYpm update is available
     *
     * @return Null if no update, String with new update version otherwise
     */
    public String checkForUpdate() {
        if (releaseJSON != null) {
            if (!releaseJSON.getTagName().equals(runtimeVersion)) {
                log.trace("New update available\nCURRENT VERSION: " + runtimeVersion + "\nNEW VERSION: " + releaseJSON.getTagName());
                try {
                    alertUser();
                } catch (IOException e) {
                    log.error("Error during setting up update window", e);
                } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
                    log.error("No stage found", e);
                }
                return releaseJSON.getTagName();
            }
        } else {
            log.error("API object is empty!", new IllegalStateException("API object is empty!"));
        }
        return null;
    }

    /**
     * @return Version of JYpm that is currently running
     */
    private String getRuntimeVersion() {
        return runtimeVersion;
    }

    /**
     * Refreshes API response
     */
    public void refresh() {
        try {
            Call<ReleaseJSON> releaseJSONCall = apiService.getLatestRelease("Open96", "JYpm");
            //Cast API response to ReleaseJSON
            releaseJSON = releaseJSONCall.execute().body();
            //If api object is empty call API again
            int retryCount = 0;
            while (true) {
                if (releaseJSON == null) {
                    Call<ReleaseJSON> repeatedJSONCall = apiService.getLatestRelease("Open96", "JYpm");
                    releaseJSON = repeatedJSONCall.execute().body();
                    retryCount++;
                    if (retryCount > 10) {
                        log.fatal("Could not get API object...");
                        throw new IllegalStateException("Empty API response");
                    }
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.error("There was an IO error during GitHub API call", e);
        }
    }


    //TODO Integrate code from this method into jypm-fxml to break circular dependency between jypm-fxml and jypm-updater
    private void alertUser() throws IOException {

        //Create message
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("Update available").append("\n");
        messageBuilder.append(getRuntimeVersion()).append(" ").append("(Current)").append(" --> ")
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
