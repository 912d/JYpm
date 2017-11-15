import com.github.open96.internetconnection.ConnectionChecker;
import com.github.open96.settings.OS_TYPE;
import com.github.open96.settings.SettingsManager;
import com.github.open96.thread.ThreadManager;
import com.github.open96.youtubedl.EXECUTABLE_STATE;
import com.github.open96.youtubedl.YoutubeDlManager;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class YoutubeDlManagerTest {


    @Test
    public void testDownloadYoutubeDl() {
        if (ConnectionChecker.getInstance().checkInternetConnection()) {
            try {
                ThreadManager.getInstance();
                YoutubeDlManager.getInstance();
                assertEquals(YoutubeDlManager.getInstance().getExecutableState(), EXECUTABLE_STATE.NOT_READY);
                //Create File variable that will lead to youtube-dl executable
                String dirName = "youtube-dl/";
                String fileName = dirName;
                if (SettingsManager.getInstance().getOS() == OS_TYPE.WINDOWS) {
                    fileName += "youtube-dl.exe";
                } else {
                    fileName += "youtube-dl";
                }
                File executable = new File(fileName);
                //Wait for download to finish
                int sleepTime = 0;
                final int threadSleepTimeout = 1000 * 60 * 10;
                while (YoutubeDlManager.getInstance().getExecutableState() != EXECUTABLE_STATE.READY) {
                    try {
                        Thread.sleep(1000);
                        sleepTime += 1000;
                        if (sleepTime > threadSleepTimeout) {
                            executable.delete();
                            new File(dirName).delete();
                            assertTrue(sleepTime <= threadSleepTimeout);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                executable.delete();
                new File(dirName).delete();
            } catch (IllegalStateException e) {
                System.out.println("Empty API object");
            }
        }
    }
}
