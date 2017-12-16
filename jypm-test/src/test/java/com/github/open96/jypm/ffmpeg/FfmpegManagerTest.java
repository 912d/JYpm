package com.github.open96.jypm.ffmpeg;

import com.github.open96.jypm.download.DownloadManager;
import com.github.open96.jypm.playlist.PlaylistManager;
import com.github.open96.jypm.playlist.QUEUE_STATUS;
import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.settings.SettingsManager;
import com.github.open96.jypm.youtubedl.EXECUTABLE_STATE;
import com.github.open96.jypm.youtubedl.YoutubeDlManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.*;

public class FfmpegManagerTest {
    //IMPORTANT - IF YOU USE DOWNLOADED EXECUTABLE YOU NEED TO CHANGE THIS VARIABLE
    private static final String PATH_TO_FFMPEG = "ffmpeg";
    private static final String SAMPLE_PLAYLIST_LINK = "PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39";
    private static String playlistPath;

    @BeforeClass
    public static void initialize() {
        deleteConfigFiles();
        PlaylistManager.getInstance();
        YoutubeDlManager.getInstance();
        DownloadManager.getInstance();
        setPlaylistPath();
        assertEquals(0, PlaylistManager.getInstance().getPlaylists().size());
    }

    @AfterClass
    public static void cleanup() {
        File playlistDirectory = new File("playlist_dir/");
        playlistDirectory.delete();
    }

    private static void setPlaylistPath() {
        File playlistDirectory = new File("playlist_dir/");
        playlistPath = playlistDirectory.getAbsolutePath();
    }

    private static void deleteConfigFiles() {
        File settingsJSON = new File("settings.json");
        File playlistsJSON = new File("playlists.json");
        settingsJSON.delete();
        playlistsJSON.delete();
    }

    @Before
    public void prepareYoutubeDl() throws InterruptedException {
        YoutubeDlManager.getInstance().deletePreviousVersionIfExists();
        if (YoutubeDlManager.getInstance().getExecutableState() != EXECUTABLE_STATE.READY) {
            int retryCount = 0;
            YoutubeDlManager.getInstance().downloadYoutubeDl();
            while (retryCount < 180) {
                Thread.sleep(1000);
                retryCount++;
                if (YoutubeDlManager.getInstance().getExecutableState() == EXECUTABLE_STATE.READY) {
                    break;
                }
            }
        }
    }

    @Before
    public void resetSingleton() {
        try {
            FfmpegManager.getInstance();
            Field singletonInstance = FfmpegManager.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
            e.printStackTrace();
        }
        //Call singleton class to let it set itself up again
        FfmpegManager.getInstance();
    }

    /**
     * Resets PlaylistManager via reflections
     */
    @Before
    public void resetPlaylistManagerSingleton() {
        try {
            //Delete playlists.json to ensure that we don't have leftovers from other tests
            new File("playlists.json").delete();
            PlaylistManager.getInstance();
            Field singletonInstance = PlaylistManager.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
            e.printStackTrace();
        }
        //Call PlaylistManager to intialize it from scratch
        assertTrue(PlaylistManager.getInstance().getPlaylists().size() == 0);
    }

    @Before
    public void ensurePlaylistDirectoryIsPresentAndEmpty() {
        File dir = new File(playlistPath);
        //If file/directory with "playlist_dir" name is present, delete it
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    f.delete();
                }
            }
            dir.delete();
            assertFalse(dir.exists());
        }
        dir.mkdir();
        assertTrue(dir.exists());
        assertEquals(0, dir.listFiles().length);
    }

    @Test
    public void testCheckIfExecutableIsValid() {
        SettingsManager.getInstance().setFfmpegExecutable(PATH_TO_FFMPEG);
        assertTrue(FfmpegManager.getInstance().checkIfExecutableIsValid());
        SettingsManager.getInstance().setFfmpegExecutable("totally_not_ffmpeg");
        assertFalse(FfmpegManager.getInstance().checkIfExecutableIsValid());
        SettingsManager.getInstance().setFfmpegExecutable(PATH_TO_FFMPEG);
    }

    @Test
    public void testConvertDirectoryToMP3() throws InterruptedException {
        //Create playlist object and issue the download
        Playlist testPlaylist = new Playlist(SAMPLE_PLAYLIST_LINK, playlistPath);
        PlaylistManager.getInstance().add(testPlaylist);
        while (PlaylistManager.getInstance().getPlaylistByLink(testPlaylist.getPlaylistLink()) == null) {
            Thread.sleep(10);
        }
        DownloadManager.getInstance().download(testPlaylist);
        //Track download progress
        while (testPlaylist.getStatus() == QUEUE_STATUS.DOWNLOADING
                || testPlaylist.getStatus() == QUEUE_STATUS.QUEUED) {
            Thread.sleep(500);
            Integer downloadProgress = DownloadManager.getInstance().getDownloadProgress();
            if (downloadProgress != null) {
                System.out.println(downloadProgress + "/" + testPlaylist.getTotalVideoCount());
            }
        }
        //Make sure files are in place where they should be
        assertEquals(QUEUE_STATUS.DOWNLOADED, testPlaylist.getStatus());
        Integer directoryFileCount = new File(playlistPath).listFiles().length;
        assertEquals(testPlaylist.getTotalVideoCount(), directoryFileCount);
        //After successful download convert all files in that directory to mp3 format
        //and expect twice as many files as a result from which half of them have a .mp3 extension
        List<Boolean> taskList = FfmpegManager
                .getInstance().convertDirectory(playlistPath, FILE_EXTENSION.MP3, 320);
        assertNotNull(taskList);
        boolean areAllTasksFinished = false;
        while (!areAllTasksFinished) {
            int unfinishedTasks = 0;
            areAllTasksFinished = true;
            for (Boolean b : taskList) {
                if (b == Boolean.FALSE) {
                    unfinishedTasks++;
                    areAllTasksFinished = false;
                }
            }
            System.out.println("Conversion progress: " + (taskList.size() - unfinishedTasks) + "/" + taskList.size());
            Thread.sleep(1000);
        }
        Integer mp3FileCounter = 0;
        for (File f : new File(playlistPath).listFiles()) {
            if (f.getName().endsWith(".mp3")) {
                mp3FileCounter++;
            }
        }
        assertEquals(testPlaylist.getTotalVideoCount(), mp3FileCounter);
        PlaylistManager.getInstance().remove(testPlaylist, true);
    }


    @Test
    public void testConvertDirectoryToMP4() throws InterruptedException {
        //Create playlist object and issue the download
        Playlist testPlaylist = new Playlist(SAMPLE_PLAYLIST_LINK, playlistPath);
        PlaylistManager.getInstance().add(testPlaylist);
        while (PlaylistManager.getInstance().getPlaylistByLink(testPlaylist.getPlaylistLink()) == null) {
            Thread.sleep(10);
        }
        DownloadManager.getInstance().download(testPlaylist);
        //Track download progress
        while (testPlaylist.getStatus() == QUEUE_STATUS.DOWNLOADING
                || testPlaylist.getStatus() == QUEUE_STATUS.QUEUED) {
            Thread.sleep(500);
            Integer downloadProgress = DownloadManager.getInstance().getDownloadProgress();
            if (downloadProgress != null) {
                System.out.println(downloadProgress + "/" + testPlaylist.getTotalVideoCount());
            }
        }
        //Make sure files are in place where they should be
        assertEquals(QUEUE_STATUS.DOWNLOADED, testPlaylist.getStatus());
        Integer directoryFileCount = new File(playlistPath).listFiles().length;
        assertEquals(testPlaylist.getTotalVideoCount(), directoryFileCount);
        //After successful download convert all files in that directory to mp3 format
        //and expect twice as many files as a result from which half of them have a .mp3 extension
        List<Boolean> taskList = FfmpegManager
                .getInstance().convertDirectory(playlistPath, FILE_EXTENSION.MP4, null);
        assertNotNull(taskList);
        boolean areAllTasksFinished = false;
        while (!areAllTasksFinished) {
            int unfinishedTasks = 0;
            areAllTasksFinished = true;
            for (Boolean b : taskList) {
                if (b == Boolean.FALSE) {
                    unfinishedTasks++;
                    areAllTasksFinished = false;
                }
            }
            System.out.println("Conversion progress: " + (taskList.size() - unfinishedTasks) + "/" + taskList.size());
            Thread.sleep(1000);
        }
        Integer mp4FileCounter = 0;
        for (File f : new File(playlistPath).listFiles()) {
            if (f.getName().endsWith(".mp4")) {
                mp4FileCounter++;
            }
        }
        assertEquals(testPlaylist.getTotalVideoCount(), mp4FileCounter);
        PlaylistManager.getInstance().remove(testPlaylist, true);
    }

}
