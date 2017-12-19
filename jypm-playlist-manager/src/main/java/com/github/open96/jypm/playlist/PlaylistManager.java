package com.github.open96.jypm.playlist;

import com.github.open96.jypm.html.YouTubeParser;
import com.github.open96.jypm.internetconnection.ConnectionChecker;
import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
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
    private volatile static PlaylistManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(PlaylistManager.class.getName());
    //Variable where all managed playlists are stored
    private static ObservableList<Playlist> playlists;

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
            synchronized (PlaylistManager.class) {
                if (singletonInstance == null) {
                    LOG.debug("Instance is null, initializing...");
                    singletonInstance = new PlaylistManager();
                }
            }
        }
        return singletonInstance;
    }

    /**
     * Method that initializes whole object and loads data from .json file to it if one exists.
     * Singleton design pattern ensures class won't load that data more than once during application runtime
     * and possibly cause desync between what is stored in RAM and what is stored in JSON.
     */
    private void init() {
        LOG.trace("Initializing PlaylistManager");
        playlists = FXCollections.observableArrayList();
        try (FileReader fileReader = new FileReader(JSON_FILE_NAME)) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            Type type = new TypeToken<ObservableList<Playlist>>() {
            }.getType();
            //To load data it is mandatory to cast gson response to ArrayList before adding it to playlists variable
            ArrayList<Playlist> gsonResponse = gson.fromJson(fileReader, type);
            if (gsonResponse != null) {
                playlists.addAll(gsonResponse);
            }
        } catch (FileNotFoundException e) {
            LOG.info(JSON_FILE_NAME + " has not been found, assuming it's a first run of application...");
        } catch (IOException e) {
            LOG.error("Unable to initialize FileReader...", e);
        }
        LOG.debug("PlaylistManager has been successfully initialized.");
    }

    /**
     * Stores playlists object's state to a JSON file.
     */
    private synchronized void saveToJson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        try (FileWriter fileWriter = new FileWriter(JSON_FILE_NAME)) {
            fileWriter.write(gson.toJson(playlists));
            fileWriter.flush();
        } catch (IOException e) {
            LOG.error("Could not save playlist to " + JSON_FILE_NAME, e);
        }
    }

    /**
     * Creates Playlist object and adds that playlist to playlists object.
     *
     * @param playlist Object of Playlist class that should be stored into PlaylistManager's array of playlists.
     * @return true if successful, false otherwise.
     */
    public synchronized boolean add(Playlist playlist) {
        if (playlists.stream()
                .anyMatch(playlist1 -> playlist1.getPlaylistLink().equals(playlist.getPlaylistLink()))) {
            LOG.warn("Adding two playlists with the same link is unsupported.");
            return false;
        }
        if (playlists.stream()
                .anyMatch(playlist1 -> playlist1.getPlaylistLocation().equals(playlist.getPlaylistLocation()))) {
            LOG.warn("Adding two playlists in the same directory/folder is not permitted.");
            return false;
        }

        //Add empty playlist to mark it as reserved and prevent duplicates
        playlists.add(playlist);

        //Create a Runnable thread that will download needed playlist data
        ThreadManager
                .getInstance().
                sendVoidTask(new Thread(() -> {
                    parsePlaylistData(playlist);
                    if (ThreadManager.getExecutionPermission() && ConnectionChecker
                            .getInstance()
                            .checkInternetConnection()) {
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
    public synchronized void remove(Playlist playlist, boolean deleteDir) {
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    //Look for playlist that matches playlist that was requested to be removed
                    playlists.stream()
                            .filter(playlist1 -> playlist1.getPlaylistLink()
                                    .equals(playlist.getPlaylistLink()))
                            .forEach(playlist1 -> Platform.runLater(() -> {
                                LOG.trace("Removing " + playlist1.getPlaylistName());
                                playlists.remove(playlist1);
                                ;
                            }));
                }), TASK_TYPE.PLAYLIST);
        //Delete playlist directory
        if (deleteDir) {
            ThreadManager
                    .getInstance()
                    .sendVoidTask(new Thread(() -> {
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
        saveToJson();
    }

    /**
     * @return All playlists as ArrayList object.
     */
    public ObservableList<Playlist> getPlaylists() {

        //Get playlist asynchronously via executorService, this ensures playlists object is in readable state
        Callable<ObservableList<Playlist>> playlistGetterThread = () -> playlists;
        Future<ObservableList<Playlist>> playlistFuture = ThreadManager
                .getInstance()
                .sendTask(playlistGetterThread, TASK_TYPE.PLAYLIST);

        try {
            return playlistFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to retrieve playlists", e);
        }
        //In case of failure empty list will be displayed
        return FXCollections.observableArrayList();
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
            Future<Playlist> playlistFuture = ThreadManager
                    .getInstance()
                    .sendTask(playlistGetterThread, TASK_TYPE.PLAYLIST);
            return playlistFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            //If stream threw IndexOutOfBoundsException the playlist with specified link has not been found
            if (!(e.getCause() instanceof IndexOutOfBoundsException)) {
                LOG.error("Failed to retrieve playlists", e);
            }
            return null;
        } catch (RejectedExecutionException e) {
            return null;
        }
    }


    /**
     * Sets requested status of requested playlist
     */
    public void updatePlaylistStatus(Playlist playlist, PLAYLIST_STATUS status) {
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    playlists.stream()
                            .filter(playlist1 -> playlist.getPlaylistLink()
                                    .equals(playlist1.getPlaylistLink()))
                            .forEach(playlist1 -> {
                                playlist1.setStatus(status);
                                if (status == PLAYLIST_STATUS.DOWNLOADING) {
                                    playlist1.setCurrentVideoCount(0);
                                }
                                saveToJson();
                            });
                }), TASK_TYPE.PLAYLIST);
    }


    private void parsePlaylistData(Playlist playlist) {
        if (ConnectionChecker.getInstance().checkInternetConnection()) {
            YouTubeParser youTubeParser = new YouTubeParser(playlist.getPlaylistLink());
            int parserRetryCount = 0;
            while (!youTubeParser.validateDocument() && parserRetryCount < 10) {
                youTubeParser = new YouTubeParser(playlist.getPlaylistLink());
                parserRetryCount++;
            }
            if (parserRetryCount >= 10) {
                LOG.error("Could not parse playlist info, " +
                        "check if your firewall doesn't block youtube access");
            }
            playlist.setPlaylistName(youTubeParser.getPlaylistName());
            playlist.setTotalVideoCount(Integer.parseInt(youTubeParser.getVideoCount()));
            playlist.setPlaylistThumbnailUrl(youTubeParser.getThumbnailLink());
            LOG.trace("Playlist data successfully parsed.");
            saveToJson();
        }
    }


    public Boolean updatePlaylistData(Playlist playlist) {
        Future playlistUpdaterThread = ThreadManager
                .getInstance()
                .sendTask(() -> {
                    playlists.stream()
                            .filter(playlist1 -> playlist.getPlaylistLink()
                                    .equals(playlist1.getPlaylistLink()))
                            .forEach(playlist1 -> parsePlaylistData(playlist));
                    return true;
                }, TASK_TYPE.PLAYLIST);
        try {
            return (Boolean) playlistUpdaterThread.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Could not receive playlist data", e);
        }
        return null;
    }


    /**
     * Sets current video count of requested playlist
     */
    public void setCurrentVideoCount(Playlist playlist, Integer videoCount) {
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    playlists.stream()
                            .filter(playlist1 -> playlist.getPlaylistLink()
                                    .equals(playlist1.getPlaylistLink()))
                            .forEach(playlist1 -> {
                                playlist1.setCurrentVideoCount(videoCount);
                                saveToJson();
                            });
                }), TASK_TYPE.PLAYLIST);
    }

}
