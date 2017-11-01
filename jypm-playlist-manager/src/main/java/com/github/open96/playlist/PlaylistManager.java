package com.github.open96.playlist;

import com.github.open96.html.YouTubeParser;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.settings.SettingsManager;
import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * Class that is solely responsible from managing playlists and keeping track of everything happening inside of them.
 * Every change that happens to any of the playlists is saved in JSON file.
 * Writing/Reading from/to JSON is implemented via Google's GSON library.
 */
public class PlaylistManager {
    //File which stores Playlist objects in form of JSON
    private final static String JSON_FILE_NAME = "playlists.json";
    //This object is a singleton thus storing instance of it is needed
    private static PlaylistManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(PlaylistManager.class.getName());
    //Variable that points to array responsible for displaying playlists in UI Thread
    private static ObservableList<Playlist> observablePlaylists;
    //Variable where all managed playlists are stored
    private static ArrayList<Playlist> playlists;

    private PlaylistManager() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of PlaylistManager.
     */
    public static PlaylistManager getInstance() {
        if (singletonInstance == null) {
            log.debug("Instance is null, initializing...");
            singletonInstance = new PlaylistManager();
        }
        return singletonInstance;
    }

    /**
     * Method that initializes whole object and loads data from .json file to it if one exists.
     * Singleton design pattern ensures class won't load that data more than once during application runtime
     * and possibly cause desync between what is stored in RAM and what is stored in JSON.
     */
    private void init() {
        log.trace("Initializing PlaylistManager");
        playlists = new ArrayList<>();
        observablePlaylists = FXCollections.observableArrayList();
        try (FileReader fileReader = new FileReader(JSON_FILE_NAME)) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            Type type = new TypeToken<ObservableList<Playlist>>() {
            }.getType();
            //To load data it is mandatory to cast gson response to ArrayList before adding it to playlists variable
            ArrayList<Playlist> gsonResponse = gson.fromJson(fileReader, type);
            if (gsonResponse != null) {
                playlists.addAll(gsonResponse);
                observablePlaylists.addAll(playlists);
            }
        } catch (FileNotFoundException e) {
            log.info(JSON_FILE_NAME + " has not been found, assuming it's a first run of application...");
        } catch (IOException e) {
            log.error("Unable to initialize FileReader...", e);
        }
        log.debug("PlaylistManager has been successfully initialized.");
    }

    /**
     * Stores playlists object's state to a JSON file.
     */
    private void saveToJson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        try (FileWriter fileWriter = new FileWriter(JSON_FILE_NAME)) {
            fileWriter.write(gson.toJson(playlists));
            fileWriter.flush();
        } catch (IOException e) {
            log.error("Could not save playlist to " + JSON_FILE_NAME, e);
        }
    }

    /**
     * Creates Playlist object and adds that playlist to playlists object.
     *
     * @param playlist Object of Playlist class that should be stored into PlaylistManager's array of playlists.
     * @return true if successful, false otherwise.
     */
    public boolean add(Playlist playlist) {
        if (playlists.stream()
                .anyMatch(playlist1 -> playlist1.getPlaylistLink().equals(playlist.getPlaylistLink()))) {
            log.warn("Adding two playlists with the same link is unsupported.");
            return false;
        }
        if (playlists.stream()
                .anyMatch(playlist1 -> playlist1.getPlaylistLocation().equals(playlist.getPlaylistLocation()))) {
            log.warn("Adding two playlists in the same directory/folder is not permitted.");
            return false;
        }

        //Add empty playlist to mark it as reserved and prevent duplicates
        playlists.add(playlist);

        //Create a Runnable thread that will download needed playlist data
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            YouTubeParser youTubeParser = new YouTubeParser(playlist.getPlaylistLink());
            playlist.setPlaylistName(youTubeParser.getPlaylistName());
            playlist.setVideoCount(Integer.parseInt(youTubeParser.getVideoCount()));
            playlist.setPlaylistThumbnailUrl(youTubeParser.getThumbnailLink());
            log.trace("Playlist data successfully parsed.");
            if (ThreadManager.getExecutionPermission() && SettingsManager.getInstance().checkInternetConnection()) {
                ThreadManager.getInstance().sendVoidTask(new Thread(() -> Platform.runLater(() -> observablePlaylists.add(playlist))), TASK_TYPE.UI);
                saveToJson();
            } else {
                playlists.remove(playlist);
            }
        }), TASK_TYPE.PLAYLIST);
        return true;
    }

    /**
     * Removes playlist from playlists object.
     *
     * @param playlist Object of Playlist class that should be removed from PlaylistManager's playlists variable.
     */
    public void remove(Playlist playlist, boolean deleteDir) {
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            //Look for playlist that matches playlist that was requested to be removed
            playlists.stream()
                    .filter(playlist1 -> playlist1.getPlaylistLink()
                            .equals(playlist.getPlaylistLink()))
                    .forEach(playlist1 -> {
                        log.trace("Removing " + playlist1.getPlaylistName());
                        playlists.remove(playlist1);
                        saveToJson();
                    });
        }), TASK_TYPE.PLAYLIST);
        //Delete playlist directory
        if (deleteDir) {
            ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
                //Give DownloadManager time to stop downloading.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //Proceed to delete every file in playlist directory
                File playlistDirectory = new File(playlist.getPlaylistLocation());
                if (playlistDirectory.exists() && playlistDirectory.listFiles() != null) {
                    for (File f : playlistDirectory.listFiles()) {
                        f.delete();
                    }
                    playlistDirectory.delete();
                }
            }), TASK_TYPE.OTHER);
        }

    }


    /**
     * @return All playlists as ObservableList object.
     */
    public ObservableList<Playlist> getObservablePlaylists() {

        //Get playlist asynchronously via executorService, this ensures playlists object is in readable state
        Callable<ObservableList<Playlist>> observablePlaylistGetterThread = () -> observablePlaylists;
        Future<ObservableList<Playlist>> observablePlaylistFuture = ThreadManager.getInstance().sendTask(observablePlaylistGetterThread, TASK_TYPE.PLAYLIST);

        try {
            return observablePlaylistFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve observable playlists", e);
        }
        //In case of failure empty list will be displayed
        return FXCollections.observableArrayList();
    }

    /**
     * @return All playlists as ArrayList object.
     */
    public ArrayList<Playlist> getPlaylists() {

        //Get playlist asynchronously via executorService, this ensures playlists object is in readable state
        Callable<ArrayList<Playlist>> playlistGetterThread = () -> playlists;
        Future<ArrayList<Playlist>> playlistFuture = ThreadManager.getInstance().sendTask(playlistGetterThread, TASK_TYPE.PLAYLIST);

        try {
            return playlistFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve playlists", e);
        }
        //In case of failure empty list will be displayed
        return new ArrayList<>();
    }

    /**
     * @param link Link of playlist we want to get from PlaylistManager
     * @return Playlist object if it matched specified link, null otherwise
     */
    public Playlist getPlaylistByLink(String link) {
        try {
            //Get playlist asynchronously via executorService, this ensures playlists object is in readable state
            Callable<Playlist> playlistGetterThread = () -> playlists.stream()
                    .filter(playlist -> playlist.getPlaylistLink()
                            .equals(link)).collect(Collectors.toList())
                    .get(0);
            Future<Playlist> playlistFuture = ThreadManager.getInstance().sendTask(playlistGetterThread, TASK_TYPE.PLAYLIST);
            return playlistFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            //If stream threw IndexOutOfBoundsException the playlist with specified link has not been found
            if (!(e.getCause() instanceof IndexOutOfBoundsException)) {
                log.error("Failed to retrieve playlists", e);
            }
            return null;
        } catch (RejectedExecutionException e) {
            return null;
        }
    }


    /**
     * Sets requested status of requested playlist
     */
    public void updatePlaylistStatus(Playlist playlist, QUEUE_STATUS status) {
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            playlists.stream()
                    .filter(playlist1 -> playlist.getPlaylistLink()
                            .equals(playlist1.getPlaylistLink()))
                    .forEach(playlist1 -> {
                        playlist1.setStatus(status);
                        saveToJson();
                    });
        }), TASK_TYPE.PLAYLIST);
    }

}
