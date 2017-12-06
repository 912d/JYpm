package com.github.open96.jypm.playlist;

import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.settings.SettingsManager;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;

import static org.junit.Assert.*;

public class PlaylistManagerTest {
    private static Playlist samplePlaylist;
    private static String playlistPath;

    @BeforeClass
    public static void initialize() {
        //Initialize SettingsManager first
        SettingsManager.getInstance();
        try {
            //Get path to test class to ensure we have safe directory to store files
            URL path;
            path = PlaylistManagerTest.class.getResource("PlaylistManagerTest.class");
            //Cut "PlaylistManagerTest.class" from path and append "playlist_dir" suffix to it
            path = new URL(path.toString().substring(0,
                    path.toString().substring("PlaylistManagerTest.class".length()).length()));
            path = new URL(path.toString() + "playlist_dir");
            //Parse URL to string ensuring proper encoding is being used
            playlistPath = URLDecoder.decode(path.getPath(), "UTF-8");
            //Initialize Playlist object with path which we created before
            samplePlaylist = new Playlist("PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39", playlistPath);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        //Call JavaFX component so JavaFX initializes and executes Platform.runLater() tasks
        new JFXPanel();
    }

    /**
     * Resets PlaylistManager via reflections
     */
    @Before
    public void resetSingleton() {
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
    }


    private void waitForPlaylistInitialization(Playlist p) {
        final int OBJECT_INITIALIZATION_TIMEOUT = 10000, SLEEP_TIME = 10;
        int timeout = 0;
        try {
            boolean checkIfNotNull = p.getTotalVideoCount() != null && p.getPlaylistName() != null && p.getPlaylistThumbnailUrl() != null;
            while (!checkIfNotNull) {
                Thread.sleep(SLEEP_TIME);
                timeout += SLEEP_TIME;
                assertTrue(timeout <= OBJECT_INITIALIZATION_TIMEOUT);
                checkIfNotNull = p.getTotalVideoCount() != null && p.getPlaylistName() != null && p.getPlaylistThumbnailUrl() != null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testAdd() {
        try {
            //Check if PlaylistManager rejects duplicates
            assertTrue(PlaylistManager
                    .getInstance()
                    .add(samplePlaylist));
            //Wait for object to end its initialization phase
            waitForPlaylistInitialization(samplePlaylist);
            assertFalse(PlaylistManager
                    .getInstance()
                    .add(samplePlaylist));
            //Check if added playlist is present and properly set in PlaylistManager
            assertEquals(samplePlaylist.getPlaylistLink(),
                    PlaylistManager
                            .getInstance()
                            .getPlaylistByLink(samplePlaylist.getPlaylistLink()).getPlaylistLink());
            assertEquals(samplePlaylist.getPlaylistLocation(),
                    PlaylistManager
                            .getInstance()
                            .getPlaylistByLink(samplePlaylist.getPlaylistLink()).getPlaylistLocation());
            int videoCount = PlaylistManager
                    .getInstance()
                    .getPlaylistByLink(samplePlaylist.getPlaylistLink()).getTotalVideoCount();
            assertEquals(2, videoCount);
            assertEquals(QUEUE_STATUS.QUEUED, PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()).getStatus());
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
            e.printStackTrace();
        }
    }

    @Test
    public void testRemove() {
        try {
            PlaylistManager
                    .getInstance()
                    .add(samplePlaylist);
            waitForPlaylistInitialization(samplePlaylist);
            ObservableList<Playlist> playlists = PlaylistManager.getInstance().getPlaylists();
            assertEquals(1, playlists.size());
            PlaylistManager.getInstance().remove(samplePlaylist, false);
            int timeout = 0;
            while (PlaylistManager.getInstance().getPlaylists().size() != 0) {
                Thread.sleep(10);
                timeout += 10;
                assertTrue(timeout <= 5000);
            }
            assertEquals(0, playlists.size());
            assertNull(PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
            e.printStackTrace();
        }
    }

    @Test
    public void testRemoveDirectoryDeletion() {
        try {
            File samplePlaylistDir = new File(playlistPath);
            PlaylistManager.getInstance().add(samplePlaylist);
            Thread.sleep(1000);
            assertTrue(samplePlaylistDir.exists());
            File sampleVid1 = new File(playlistPath + "/v1");
            File sampleVid2 = new File(playlistPath + "/v2");
            sampleVid1.createNewFile();
            sampleVid2.createNewFile();
            assertEquals(samplePlaylistDir.listFiles().length, 2);
            PlaylistManager.getInstance().remove(samplePlaylist, false);
            Thread.sleep(1000);
            assertTrue(samplePlaylistDir.exists());
            assertEquals(samplePlaylistDir.listFiles().length, 2);
            PlaylistManager.getInstance().add(samplePlaylist);
            Thread.sleep(1000);
            PlaylistManager.getInstance().remove(samplePlaylist, true);
            Thread.sleep(2000);
            assertFalse(samplePlaylistDir.exists());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetPlaylists() {
        try {
            PlaylistManager.getInstance();
            Thread.sleep(1000);
            assertEquals(0, PlaylistManager.getInstance().getPlaylists().size());
            assertNull(PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()));
            PlaylistManager.getInstance().add(samplePlaylist);
            Thread.sleep(1000);
            assertEquals(1, PlaylistManager.getInstance().getPlaylists().size());
            assertEquals(samplePlaylist, PlaylistManager.getInstance().getPlaylists().get(0));
            assertEquals(samplePlaylist, PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()));
            PlaylistManager.getInstance().remove(samplePlaylist, false);
            Thread.sleep(500);
            assertNull(PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()));
            assertEquals(0, PlaylistManager.getInstance().getPlaylists().size());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
        }

    }

}
