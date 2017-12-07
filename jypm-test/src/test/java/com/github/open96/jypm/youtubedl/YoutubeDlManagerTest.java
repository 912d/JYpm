package com.github.open96.jypm.youtubedl;

import com.github.open96.jypm.internetconnection.ConnectionChecker;
import com.github.open96.jypm.settings.OS_TYPE;
import com.github.open96.jypm.settings.SettingsManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class YoutubeDlManagerTest {
    private static String executablePath;
    private static final String EXECUTABLE_DIRECTORY = "youtube-dl/";


    @BeforeClass
    public static void initialize() {
        setExecutablePath();
        ensureExecutableDirectoryIsPresentAndEmpty();
    }

    @AfterClass
    public static void removeExecutableDirectory() {
        File dir = new File(EXECUTABLE_DIRECTORY);
        //If file/directory with "youtube-dl" name is present, delete it
        if (dir.exists()) {
            if (dir.isDirectory()) {
                for (File f : dir.listFiles()) {
                    f.delete();
                }
            }
            dir.delete();
            assertFalse(dir.exists());
        }
        assertFalse(dir.exists());
    }


    private static void setExecutablePath() {
        //Create File variable that will lead to youtube-dl executable (based on OS filename differs)
        String fileName = EXECUTABLE_DIRECTORY;
        if (SettingsManager.getInstance().getOS() == OS_TYPE.WINDOWS) {
            fileName += "youtube-dl.exe";
        } else {
            fileName += "youtube-dl";
        }
        executablePath = fileName;
    }


    private static void ensureExecutableDirectoryIsPresentAndEmpty() {
        File dir = new File(EXECUTABLE_DIRECTORY);
        //If file/directory with "youtube-dl" name is present, delete it
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


    private void waitForDownload() {
        final int DOWNLOAD_TIMEOUT = 1000 * 60 * 4, SLEEP_TIME = 10;
        int timeout = 0;
        try {
            while (YoutubeDlManager.getInstance().getExecutableState() != EXECUTABLE_STATE.READY) {
                Thread.sleep(SLEEP_TIME);
                timeout += SLEEP_TIME;
                assertTrue(timeout <= DOWNLOAD_TIMEOUT);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Before
    public void deleteSingletonInstance() {
        try {
            Field singletonInstance = YoutubeDlManager.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void checkInternetConnection() {
        assertTrue(ConnectionChecker.getInstance().checkInternetConnection());
    }

    @Test
    public void testDownloadYoutubeDl() {
        try {
            assertEquals(EXECUTABLE_STATE.NOT_READY, YoutubeDlManager.getInstance().getExecutableState());
            //This starts download procedure
            YoutubeDlManager.getInstance();
            //Wait for download to finish
            waitForDownload();
            assertEquals(EXECUTABLE_STATE.READY, YoutubeDlManager.getInstance().getExecutableState());
            File executable = new File(executablePath);
            assertTrue(executable.exists());
            assertEquals(executablePath, executable.getPath());
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
        }
    }

}
