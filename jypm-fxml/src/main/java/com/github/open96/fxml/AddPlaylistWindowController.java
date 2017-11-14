package com.github.open96.fxml;

import com.github.open96.download.DownloadManager;
import com.github.open96.internetconnection.ConnectionChecker;
import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.settings.SettingsManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class AddPlaylistWindowController implements Initializable {

    private final static String BASE_YOUTUBE_URL = "https://www.youtube.com/playlist?list=";
    private File selectedDirectory;
    private boolean isDirectoryChosen = false;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(AddPlaylistWindowController.class.getName());
    @FXML
    GridPane rootPane;

    @FXML
    Label playlistLinkLabel;

    @FXML
    Label playlistDirectoryLabel;

    @FXML
    Label directoryLabel;

    @FXML
    TextField playlistLinkTextField;

    @FXML
    Button directoryChooserButton;

    @FXML
    Button addPlaylistButton;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    /**
     * Prompts user for link and target directory.
     * Handles link/directory validation and if successful - passes it to PlaylistManager class.
     */
    public void onAddPlaylistButtonClick(ActionEvent actionEvent) {
        //Cast link provided by user to String for later use
        String playlistLink = playlistLinkTextField.getText();

        //Check if link is valid, if directory was chosen and if user has write/read access to it.
        boolean validation = validateLink(playlistLink) && isDirectoryChosen
                && selectedDirectory.canRead() && selectedDirectory.canWrite() && ConnectionChecker
                .getInstance()
                .checkInternetConnection();

        //Trim link for easier operations on it later
        try {
            playlistLink = playlistLink.substring(BASE_YOUTUBE_URL.length());
        } catch (StringIndexOutOfBoundsException e) {
            log.warn("Link is too short to be a valid link", e);
            validation = false;
        }


        log.debug("\nUser tried to add a new playlist:" +
                "\nPlaylist link: " + playlistLink +
                "\nDirectory: " + isDirectoryChosen +
                "\nValidation: " + validation);

        if (validation) {
            if (!PlaylistManager
                    .getInstance()
                    .add(new Playlist(playlistLink, selectedDirectory.getAbsolutePath()))) {
                //If playlist wasn't accepted by PlaylistManager it means it has a duplicate link or directory.
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText(null);
                alert.setTitle("Already in use");
                alert.setContentText("Playlist or directory is already in use.");
                alert.showAndWait();
            } else {
                if (!SettingsManager
                        .getInstance()
                        .getYoutubeDlExecutable().equals("")) {
                    //After validating playlist, download it
                    DownloadManager
                            .getInstance()
                            .download(PlaylistManager
                                    .getInstance()
                                    .getPlaylistByLink(playlistLink));
                }
                //If it was, job is done and window should close
                rootPane.getScene().getWindow().hide();
            }
        } else {
            //If validation process has not been passed, tell user data he provided is invalid
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(null);
            alert.setTitle("Error");
            alert.setContentText("Please provide valid playlist data");
            alert.showAndWait();
        }
    }

    /**
     * Display system-native directory chooser for user to specify target directory.
     */
    public void onDirectoryChooserButtonClick(ActionEvent actionEvent) {
        //Prompt user to chose directory and cast it to File variable
        selectedDirectory = new DirectoryChooser().showDialog(rootPane.getScene().getWindow());
        if (selectedDirectory == null) {
            //In case user exited file chooser display default message
            directoryLabel.setText("None");
        } else {
            //In case user chosen a directory, update label to show what he chose and set isDirectoryChosen to true
            directoryLabel.setText(selectedDirectory.getAbsolutePath());
            isDirectoryChosen = true;
        }
    }

    /**
     * @param link Link in form of string that should be validated
     * @return True if link is valid, false otherwise
     */
    private boolean validateLink(String link) {
        return link.contains(BASE_YOUTUBE_URL);
    }
}
