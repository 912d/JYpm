import com.github.open96.jypm.playlist.PlaylistManager;
import com.github.open96.jypm.playlist.QUEUE_STATUS;
import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.settings.SettingsManager;
import javafx.collections.ObservableList;
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
    private Playlist samplePlaylist;
    private String pathToClass;


    public PlaylistManagerTest() {
        try {
            URL path;
            path = PlaylistManagerTest.class.getResource("PlaylistManagerTest.class");
            path = new URL(path.toString().substring(0, path.toString().substring("PlaylistManagerTest.class".length()).length()));
            path = new URL(path.toString() + "playlist_dir");
            pathToClass = URLDecoder.decode(path.getPath(), "UTF-8");
            samplePlaylist = new Playlist("PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39", pathToClass);
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        try {
            SettingsManager.getInstance();
            Thread.sleep(2000); //If you have slower internet connection you can change that to greater value
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void resetSingleton() {
        try {
            new File("playlists.json").delete();
            PlaylistManager.getInstance();
            Field singletonInstance = PlaylistManager.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
        }
    }


    @Test
    public void testAdd() {
        try {
            resetSingleton();
            File dir = new File(pathToClass);
            if (!dir.exists()) {
                dir.mkdir();
                assertTrue(dir.exists());
            }
            assertTrue(dir.exists());
            assertTrue(PlaylistManager.getInstance().getPlaylists().size() == 0);
            assertTrue(PlaylistManager.getInstance().add(samplePlaylist));
            assertFalse(PlaylistManager.getInstance().add(samplePlaylist));
            if (PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()) != null) {
                assertEquals(samplePlaylist.getPlaylistLink(), PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()).getPlaylistLink());
                assertEquals(samplePlaylist.getPlaylistLocation(), PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()).getPlaylistLocation());
                Thread.sleep(1000); //Give YouTubeParser time to parse html
                int videoCount = PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()).getVideoCount();
                assertEquals(2, videoCount);
                assertEquals(QUEUE_STATUS.QUEUED, PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()).getStatus());
            }
            dir.delete();
            assertFalse(dir.exists());
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRemove() {
        try {
            resetSingleton();
            PlaylistManager.getInstance().add(samplePlaylist);
            Thread.sleep(1000);
            assertFalse(PlaylistManager.getInstance().add(samplePlaylist));
            ObservableList<Playlist> playlists = PlaylistManager.getInstance().getPlaylists();
            assertEquals(1, playlists.size());
            assertNotNull(PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()));
            PlaylistManager.getInstance().remove(samplePlaylist, false);
            Thread.sleep(400);
            assertEquals(0, playlists.size());
            assertNull(PlaylistManager.getInstance().getPlaylistByLink(samplePlaylist.getPlaylistLink()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
        }
    }

    @Test
    public void testRemoveDirectoryDeletion() {
        try {
            resetSingleton();
            File samplePlaylistDir = new File(pathToClass);
            PlaylistManager.getInstance().add(samplePlaylist);
            Thread.sleep(1000);
            assertFalse(samplePlaylistDir.exists());
            samplePlaylistDir.mkdir();
            File sampleVid1 = new File(pathToClass + "/v1");
            File sampleVid2 = new File(pathToClass + "/v2");
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
            resetSingleton();
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
