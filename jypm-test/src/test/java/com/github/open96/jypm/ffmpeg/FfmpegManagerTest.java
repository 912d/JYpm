package com.github.open96.jypm.ffmpeg;

import com.github.open96.jypm.download.DownloadManager;
import com.github.open96.jypm.playlist.PlaylistManager;
import com.github.open96.jypm.playlist.QUEUE_STATUS;
import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.settings.SettingsManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class FfmpegManagerTest {
    //IMPORTANT - IF YOU USE DOWNLOADED EXECUTABLE YOU NEED TO CHANGE THIS VARIABLE
    private static final String PATH_TO_FFMPEG = "ffmpeg";
    private static final String SAMPLE_PLAYLIST_LINK = "PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39";
    private static String playlistPath;

    @BeforeClass
    public static void initialize() {
        setPlaylistPath();
        deleteConfigFiles();
        PlaylistManager.getInstance();
        assertEquals(0, PlaylistManager.getInstance().getPlaylists().size());
        DownloadManager.getInstance();
    }

    @AfterClass
    public static void cleanup() {
        File playlistDirectory = new File("playlist_dir/");
        ensurePlaylistDirectoryIsPresentAndEmpty();
        playlistDirectory.delete();
    }

    private static void setPlaylistPath() {
        File playlistDirectory = new File("playlist_dir/");
        playlistPath = playlistDirectory.getAbsolutePath();
        ensurePlaylistDirectoryIsPresentAndEmpty();
    }

    private static void deleteConfigFiles() {
        File settingsJSON = new File("settings.json");
        File playlistsJSON = new File("playlists.json");
        settingsJSON.delete();
        playlistsJSON.delete();
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

    @Test
    public void testCheckIfExecutableIsValid() {
        SettingsManager.getInstance().setFfmpegExecutable(PATH_TO_FFMPEG);
        assertTrue(FfmpegManager.getInstance().checkIfExecutableIsValid());
    }

    @Test
    public void testConvertDirectory() throws InterruptedException {
        //Create playlist object and issue the download
        Playlist testPlaylist = new Playlist(SAMPLE_PLAYLIST_LINK, playlistPath);
        PlaylistManager.getInstance().add(testPlaylist);
        while (PlaylistManager.getInstance().getPlaylistByLink(testPlaylist.getPlaylistLink()) == null) {
            Thread.sleep(10);
        }
        DownloadManager.getInstance().download(testPlaylist);
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
        FfmpegManager.getInstance().convertDirectory(playlistPath, FILE_EXTENSION.MP3);
        Integer mp3FileCounter = 0;
        for (File f : new File(playlistPath).listFiles()) {
            if (f.getName().endsWith(".mp3")) {
                mp3FileCounter++;
            }
        }
        assertEquals(testPlaylist.getTotalVideoCount(), mp3FileCounter);
    }


    //TODO Create Util module and put this code and other universal methods in there
    private String getProcessOutput(Process process) throws IOException, InterruptedException {
        //Create BufferedReader that will read process's output
        try (InputStream inputStream = process.getInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null || process.isAlive()) {
                if (line != null) {
                    output.append(line);
                }
            }
            process.waitFor();
            bufferedReader.close();
            return output.toString();
        }
    }

    private static void ensurePlaylistDirectoryIsPresentAndEmpty() {
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
    }
}
