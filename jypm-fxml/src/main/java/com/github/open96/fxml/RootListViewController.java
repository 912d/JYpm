package com.github.open96.fxml;

import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.pojo.Playlist;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the list of managed playlists that are displayed in center of the main window in application.
 */
public class RootListViewController implements Initializable {

    private static ObservableList<Playlist> playlistObservableList;
    private static Logger log = LogManager.getLogger(RootListViewController.class.getName());
    @FXML
    private ListView<Playlist> listView;

    public RootListViewController() {
        log.debug("Loading playlists in controller...");

        //Fill ObservableList with playlists that should be displayed for user in UI
        playlistObservableList = PlaylistManager
                .getInstance()
                .getObservablePlaylists();

        log.debug("Finished loading playlists in controller, succesfully loaded " + playlistObservableList.size() + " playlists into the ListView");
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Make ListView display elements from playlistObservableList
        listView.setItems(playlistObservableList);
        //Make sure ListView will display them in from now objects from RootListCellController class
        listView.setCellFactory(playlistListView -> new RootListCellController());
    }
}
