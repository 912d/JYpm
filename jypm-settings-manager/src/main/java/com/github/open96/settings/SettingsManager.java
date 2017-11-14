package com.github.open96.settings;

import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SettingsManager {
    //File which stores Playlist objects in form of JSON
    private final static String JSON_FILE_NAME = "settings.json";
    //This object is a singleton thus storing instance of it is needed
    private static SettingsManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(SettingsManager.class.getName());
    //Pojo object where settings are stored during the runtime
    private Settings settings;
    

    private SettingsManager() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of SettingsManager.
     */
    public static SettingsManager getInstance() {
        if (singletonInstance == null) {
            log.debug("Instance is null, initializing...");
            singletonInstance = new SettingsManager();
        }
        return singletonInstance;
    }

    /**
     * Method that initializes whole object and loads data from .json file to it if one exists.
     * Singleton design pattern ensures class won't load that data more than once during application runtime
     * and possibly cause desync between what is stored in RAM and what is stored in JSON.
     */
    private void init() {
        log.trace("Initializing SettingsManager");
        settings = new Settings();
        //Read data from file, if it exists.
        try (FileReader fileReader = new FileReader(JSON_FILE_NAME)) {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            Type type = new TypeToken<Settings>() {
            }.getType();
            Settings loadedSettings = gson.fromJson(fileReader, type);
            if (loadedSettings != null) {
                settings = loadedSettings;
            }
        } catch (FileNotFoundException e) {
            log.info(JSON_FILE_NAME + " has not been found, assuming it's a first run of application...");
        } catch (IOException e) {
            log.error("Unable to initialize FileReader...", e);
        }
        determineHostOS();
        setDefaultFileManagerIfNotSet();
        saveToJson();
        log.debug("SettingsManager has been successfully initialized");
    }

    /**
     * Stores settings object's state to a JSON file.
     */
    private void saveToJson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        Gson gson = gsonBuilder.create();
        try (FileWriter fileWriter = new FileWriter(JSON_FILE_NAME)) {
            fileWriter.write(gson.toJson(settings));
            fileWriter.flush();
        } catch (IOException e) {
            log.error("Could not save settings to " + JSON_FILE_NAME, e);
        }
    }

    /**
     * @return Path to youtube-dl executable stored in SettingsManager in form of String object
     */
    public String getYoutubeDlExecutable() {

        Callable<String> settingsGetterThread = () -> settings.getYoutubeDlExecutable();
        Future<String> settingsFuture = ThreadManager.getInstance().sendTask(settingsGetterThread, TASK_TYPE.SETTING);

        try {
            return settingsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve setting", e);
        }
        return null;
    }

    /**
     * Sets path to youtube-dl executable and saves it in SettingsManager
     *
     * @param executableLocation path to youtube-dl executable
     */
    public void setYoutubeDlExecutable(String executableLocation) {

        //Create a Runnable thread that will download needed playlist and video data
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            settings.setYoutubeDlExecutable(executableLocation);
            if (ThreadManager.getExecutionPermission()) {
                saveToJson();
            }
        }), TASK_TYPE.SETTING);
    }

    /**
     * @return Type of OS application is running on
     */
    public OS_TYPE getOS() {
        return settings.getOsType();
    }


    /**
     * @return File manager command stored in SettingsManager in form of String object
     */
    public String getFileManagerCommand() {

        Callable<String> settingsGetterThread = () -> settings.getFileManagerCommand();
        Future<String> settingsFuture = ThreadManager.getInstance().sendTask(settingsGetterThread, TASK_TYPE.SETTING);

        try {
            return settingsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve setting", e);
        }
        return null;
    }

    /**
     * Sets file manager command and saves it in SettingsManager
     *
     * @param fileManagerCommand path to youtube-dl executable
     */
    public void setFileManagerCommand(String fileManagerCommand) {

        //Create a Runnable thread that will download needed playlist and video data
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            settings.setFileManagerCommand(fileManagerCommand);
            saveToJson();
        }), TASK_TYPE.SETTING);
    }


    /**
     * @return Notification policy stored in SettingsManager in form of Boolean object
     */
    public Boolean getNotificationPolicy() {

        Callable<Boolean> settingsGetterThread = () -> settings.getNotificationPolicy();
        Future<Boolean> settingsFuture = ThreadManager.getInstance().sendTask(settingsGetterThread, TASK_TYPE.SETTING);

        try {
            return settingsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve setting", e);
        }
        return null;
    }

    /**
     * Sets notification policy and saves it in SettingsManager
     *
     * @param isEnabled Should notifications be enabled
     */
    public void setNotificationPolicy(Boolean isEnabled) {

        //Create a Runnable thread that will download needed playlist and video data
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            settings.setNotificationPolicy(isEnabled);
            if (ThreadManager.getExecutionPermission()) {
                saveToJson();
            }
        }), TASK_TYPE.SETTING);
    }

    /**
     * @return youtube-dl version stored in SettingsManager in form of String object
     */
    public String getYoutubeDlVersion() {

        Callable<String> settingsGetterThread = () -> settings.getYoutubeDlVersion();
        Future<String> settingsFuture = ThreadManager.getInstance().sendTask(settingsGetterThread, TASK_TYPE.SETTING);

        try {
            return settingsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve setting", e);
        }
        return null;
    }

    /**
     * Sets youtube-dl version and saves it in SettingsManager
     *
     * @param version Version of youtube-dl
     */
    public void setYoutubeDlVersion(String version) {

        //Create a Runnable thread that will download needed playlist and video data
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            settings.setYoutubeDlVersion(version);
            if (ThreadManager.getExecutionPermission()) {
                saveToJson();
            }
        }), TASK_TYPE.SETTING);
    }


    /**
     * @return runtime version stored in SettingsManager in form of String object
     */
    public String getRuntimeVersion() {

        Callable<String> settingsGetterThread = () -> settings.getRuntimeVersion();
        Future<String> settingsFuture = ThreadManager.getInstance().sendTask(settingsGetterThread, TASK_TYPE.SETTING);

        try {
            return settingsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to retrieve setting", e);
        }
        return null;
    }

    /**
     * Sets runtime version and saves it in SettingsManager
     *
     * @param version Runtime version
     */
    public void setRuntimeVersion(String version) {

        //Create a Runnable thread that will download needed playlist and video data
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            settings.setRuntimeVersion(version);
            if (ThreadManager.getExecutionPermission()) {
                saveToJson();
            }
        }), TASK_TYPE.SETTING);
    }


    private void determineHostOS() {
        if (SystemUtils.IS_OS_WINDOWS) {
            settings.setOsType(OS_TYPE.WINDOWS);
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_FREE_BSD || SystemUtils.IS_OS_NET_BSD || SystemUtils.IS_OS_OPEN_BSD) {
            settings.setOsType(OS_TYPE.OPEN_SOURCE_UNIX);
        } else if (SystemUtils.IS_OS_MAC) {
            settings.setOsType(OS_TYPE.MAC_OS);
        } else {
            settings.setOsType(OS_TYPE.UNKNOWN);
            log.warn("Unsupported OS, you are on your own...");
        }
    }

    private void setDefaultFileManagerIfNotSet() {
        if (getFileManagerCommand().equals("")) {
            if (getOS() == OS_TYPE.WINDOWS) {
                setFileManagerCommand("explorer");
            } else if (getOS() == OS_TYPE.OPEN_SOURCE_UNIX) {
                setFileManagerCommand("xdg-open");
            }
        }
    }

}
