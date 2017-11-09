package com.github.open96.download;

import com.github.open96.playlist.PlaylistManager;
import com.github.open96.playlist.QUEUE_STATUS;
import com.github.open96.playlist.pojo.Playlist;
import com.github.open96.settings.SettingsManager;
import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import com.github.open96.tray.TrayIcon;
import com.github.open96.youtubedl.EXECUTABLE_STATE;
import com.github.open96.youtubedl.YoutubeDlManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class DownloadManager {
    //This object is a singleton thus storing instance of it is needed
    private static DownloadManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(DownloadManager.class.getName());
    //Store the wrapper in variable as it is critical component of this class
    private ExecutableWrapper executableWrapper;
    //Variable where output of commands issued by executableWrapper will be stored
    private StringBuilder detailsString;
    private boolean threadLock;

    private DownloadManager() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of DownloadManager.
     */
    public static DownloadManager getInstance() {
        if (singletonInstance == null) {
            log.debug("Instance is null, initializing...");
            singletonInstance = new DownloadManager();
        }
        return singletonInstance;
    }


    /**
     * Initialize subcomponents on first instance creation
     */
    private void init() {
        YoutubeDlManager.getInstance();
        log.trace("Initializing DownloadManager");
        executableWrapper = ExecutableWrapper.getInstance();
        detailsString = new StringBuilder();
        threadLock = false;
        //Resume all interrupted tasks
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            while (ThreadManager.getExecutionPermission()) {
                //Wait for YoutubeDlManager first
                while (YoutubeDlManager.getInstance().getExecutableState() == EXECUTABLE_STATE.NOT_READY) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        log.error("Thread sleep has been interrupted", e);
                    }
                }
                if (SettingsManager.getInstance().checkInternetConnection()) {
                    ArrayList<Playlist> playlists = PlaylistManager.getInstance().getPlaylists();
                    Queue<Playlist> resumedPlaylists = new LinkedBlockingQueue<>();
                    for (Playlist p : playlists) {
                        if (p.getStatus() == QUEUE_STATUS.DOWNLOADING) {
                            download(p);
                            resumedPlaylists.add(p);
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //Resume all queued tasks
                    for (Playlist p : playlists) {
                        if (p.getStatus() == QUEUE_STATUS.QUEUED) {
                            if (!resumedPlaylists.contains(p)) {
                                download(p);
                                resumedPlaylists.add(p);
                            }
                        }
                    }
                    //Wait for TrayIcon initialization
                    int trayTimeout = 0;
                    while (!TrayIcon.isTrayWorking()) {
                        try {
                            Thread.sleep(50);
                            trayTimeout += 50;
                            if (trayTimeout > 500) {
                                break;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //Send notification
                    if (resumedPlaylists.size() > 0 && TrayIcon.isTrayWorking()) {
                        TrayIcon.getInstance().displayNotification("JYpm - resuming downloads", "Resuming " + resumedPlaylists.size() + " playlist downloads");
                    }
                    log.debug("Initializer thread has completed initialization...");
                    break;
                } else {
                    try {
                        Thread.sleep(1000 * 3); //3 seconds
                    } catch (InterruptedException e) {
                        log.error(e);
                    }
                }
            }
        }), TASK_TYPE.DOWNLOAD);
        log.debug("DownloadManager has been initialized");
    }

    /**
     * This method downloads playlist while taking care of letting all other components know what is happening.
     * All download requests should be passed to it instead of executableWrapper.
     *
     * @param playlist Playlist that should be downloaded
     */
    public void download(Playlist playlist) {
        //Assing queued status to playlist and put it in queue in ThreadManagers singleThreadExecutor.
        PlaylistManager.getInstance().updatePlaylistStatus(playlist, QUEUE_STATUS.QUEUED);

        log.trace("Downloading playlist \"" + playlist.getPlaylistName() + "\" " + "specified by link " + playlist.getPlaylistLink() + " to location " + playlist.getPlaylistLocation());
        detailsString.append("Downloading playlist \"").append(playlist.getPlaylistName()).append("\" ").append("specified by link ").append(playlist.getPlaylistLink()).append(" to location ").append(playlist.getPlaylistLocation()).append("\n");

        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            try {
                PlaylistManager.getInstance().updatePlaylistStatus(playlist, QUEUE_STATUS.DOWNLOADING);
                //Start the download
                Process process = executableWrapper.downloadPlaylist(playlist);
                try (InputStream inputStream = process.getInputStream()) {
                    //Create BufferedReader and read all output of the process until it dies
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    while (process.isAlive() && PlaylistManager.getInstance().getPlaylistByLink(playlist.getPlaylistLink()) != null && ThreadManager.getExecutionPermission()) {
                        if (!threadLock) {
                            if ((line = bufferedReader.readLine()) != null || line != null) {
                                detailsString.append(line).append("\n");
                                //Trim the StringBuilder to reduce memory usage
                                if (detailsString.length() > 16000) {
                                    detailsString.trimToSize();
                                    detailsString = new StringBuilder(detailsString.toString().substring(detailsString.length() - 16000));
                                }
                                Thread.sleep(250);
                            }
                        }
                    }
                    if (PlaylistManager.getInstance().getPlaylistByLink(playlist.getPlaylistLink()) != null) {
                        TrayIcon.getInstance().displayNotification("JYpm - download completed", "Playlist " + playlist.getPlaylistName() + " has been downloaded");
                    }
                    detailsString.append("\n").append("-----------Task finished-----------\n").append("\n");
                    PlaylistManager.getInstance().updatePlaylistStatus(playlist, QUEUE_STATUS.DOWNLOADED);
                    bufferedReader.close();
                }
            } catch (InterruptedException | IOException | NullPointerException e) {
                log.warn("Missing executable, start the download again after executable is finished downloading.");
                ExecutableWrapper.getInstance().triggerExecutableRedownload();
                detailsString.append("\n").append("-----------Task failed-----------").append("\n").append(e.toString());
                log.error("Download failed", e);
                PlaylistManager.getInstance().updatePlaylistStatus(playlist, QUEUE_STATUS.FAILED);
            }
        }), TASK_TYPE.DOWNLOAD);
    }

    /**
     * @return String object containing output of all (trimmed to 16000 characters) commands ran so far.
     */
    public String getDetailsString() {
        //Lock the thread that get output from process and get string.
        Callable<String> stringGetterThread = () -> {
            String output = processOutputGetter();
            if (output.toCharArray().length >= 0)
                return output;
            return null;
        };
        Future<String> stringGetterFuture = ThreadManager.getInstance().sendTask(stringGetterThread, TASK_TYPE.OTHER);

        try {
            return stringGetterFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve output", e);
        }
        //In case of failure empty list will be displayed
        return null;
    }


    /**
     * @return Number of already downloaded videos in form of Integer object.
     */
    public Integer getDownloadProgress() {
        //Lock the thread that get output from process and get string.
        Callable<Integer> countGetterThread = () -> {
            String output = processOutputGetter();
            if (output != null && output.toCharArray().length >= 0) {
                //youtube-dl outputs following message "[download] Downloading video x out of y" each time it downloads next video, so we just need to get the "x" part to know the progress
                String dividerString = "[download] Downloading video ";
                String[] downloadMessages = output.split(dividerString);
                String[] splitMessages = downloadMessages[downloadMessages.length - 1].split(" of ");
                String videoCount = splitMessages[splitMessages.length - 2].substring(splitMessages[splitMessages.length - 2].indexOf(dividerString) + dividerString.length());
                return Integer.valueOf(videoCount);
            }
            return null;
        };
        Future<Integer> countGetterFuture = ThreadManager.getInstance().sendTask(countGetterThread, TASK_TYPE.OTHER);
        try {
            return countGetterFuture.get();
        } catch (InterruptedException e) {
            log.error("Failed to retrieve output", e);
        } catch (ExecutionException e) {
            return null;
        }
        //In case of failure empty list will be displayed
        return null;
    }


    /**
     * Locks the downloaderThread and gets data from it.
     *
     * @return Output of youtube-dl in form of String object.
     */
    private String processOutputGetter() {
        String output = null;
        try {
            threadLock = true;
            Thread.sleep(100);
            output = detailsString.toString();
            threadLock = false;
        } catch (InterruptedException e) {
            log.error("Thread has been interrupted");
        }
        return output;
    }
}

