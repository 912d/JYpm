package com.github.open96.jypm.fxml;

import com.github.open96.jypm.playlist.PlaylistManager;
import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import javafx.application.Platform;
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
    private static final Logger LOG = LogManager.getLogger(RootListViewController.class.getName());
    @FXML
    private ListView<Playlist> listView;

    public RootListViewController() {
        LOG.debug("Loading playlists in controller...");

        //Fill ObservableList with playlists that should be displayed for user in UI
        playlistObservableList = PlaylistManager
                .getInstance()
                .getPlaylists();

        LOG.debug("Finished loading playlists in controller, succesfully loaded " +
                playlistObservableList.size() + " playlists into the ListView");
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //Make ListView display elements from playlistObservableList
        listView.setItems(playlistObservableList);
        //Make sure ListView will display them in from now objects from RootListCellController class
        listView.setCellFactory(playlistListView -> new RootListCellController());
        startUIUpdaterThread();
    }

    private void startUIUpdaterThread() {
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            try {
                int lastKnownObservableListSize = playlistObservableList.size();
                while (ThreadManager.getExecutionPermission()) {
                    if (playlistObservableList.size() != lastKnownObservableListSize) {
                        Platform.runLater(() -> {
                            listView.setItems(null);
                            listView.setItems(playlistObservableList);
                        });
                        lastKnownObservableListSize = playlistObservableList.size();
                    }
                    Thread.sleep(255);
                }
            } catch (InterruptedException e) {
                LOG.error("Thread sleep has been interrupted");
            }
        }), TASK_TYPE.UI);
    }
}
