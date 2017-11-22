import com.github.open96.jypm.internetconnection.ConnectionChecker;
import com.github.open96.jypm.updater.Updater;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.Assert.*;

public class UpdaterTest {

    public UpdaterTest() {
        try {
            Updater.getInstance();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
        }
    }

    private void resetSingleton() {
        try {
            Field singletonInstance = Updater.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void setFakeRuntimeVersion(String version) {
        try {
            Field runtimeVersion = Updater.class.getDeclaredField("runtimeVersion");
            runtimeVersion.setAccessible(true);
            runtimeVersion.set(Updater.getInstance(), version);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCheckForUpdate() {
        if (ConnectionChecker.getInstance().checkInternetConnection()) {
            try {
                resetSingleton();
                setFakeRuntimeVersion("-1");
                String newestVersion = Updater.getInstance().checkForUpdate();
                assertNotNull(newestVersion);
                if (newestVersion != null) {
                    setFakeRuntimeVersion(newestVersion);
                    assertNull(Updater.getInstance().checkForUpdate());
                }
            } catch (IllegalStateException e) {
                System.out.println("Empty API object");
            }
        }
    }

    @Test
    public void testRefresh() {
        if (ConnectionChecker.getInstance().checkInternetConnection()) {
            try {
                //Get runtime version
                Properties properties = new Properties();
                properties.load(Updater.class.getClassLoader().getResourceAsStream("version.properties"));
                resetSingleton();
                String newestVersion = Updater.getInstance().checkForUpdate();
                setFakeRuntimeVersion("-1");
                Updater.getInstance().refresh();
                String updatedVersion = Updater.getInstance().checkForUpdate();
                if (!updatedVersion.equals(properties.getProperty("runtime.version"))) {
                    assertEquals(newestVersion, Updater.getInstance().checkForUpdate());
                }
            } catch (IllegalStateException e) {
                System.out.println("Empty API object");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
