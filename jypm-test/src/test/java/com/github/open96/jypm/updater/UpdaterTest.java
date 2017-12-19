package com.github.open96.jypm.updater;

import com.github.open96.jypm.internetconnection.ConnectionChecker;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class UpdaterTest {

    @Before
    public void resetSingleton() {
        try {
            Field singletonInstance = Updater.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        Updater.getInstance();
    }

    @Before
    public void checkInternetConnection() {
        assertTrue(ConnectionChecker.getInstance().checkInternetConnection());
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
        try {
            //Check if checkForUpdate() returns not-null string when online version is different from runtime one
            setFakeRuntimeVersion("VERSION THAT DOESN'T EXIST");
            String newestVersion = Updater.getInstance().checkForUpdate();
            assertNotNull(newestVersion);
            //Now check if checkForUpdate() will return null when runtime version matches the online one
            setFakeRuntimeVersion(newestVersion);
            assertNull(Updater.getInstance().checkForUpdate());
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
            e.printStackTrace();
        }
    }

}
