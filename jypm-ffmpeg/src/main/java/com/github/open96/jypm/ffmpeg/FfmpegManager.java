package com.github.open96.jypm.ffmpeg;


import com.github.open96.jypm.settings.SettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class FfmpegManager {
    //This object is a singleton thus storing instance of it is needed
    private static FfmpegManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(FfmpegManager.class.getName());


    private FfmpegManager() {
        init();
    }

    /**
     * Create an instance that will be always returned when running that method
     *
     * @return Singleton instance of ExecutableWrapper
     */
    public static FfmpegManager getInstance() {
        if (singletonInstance == null) {
            LOG.debug("Instance is null, initializing...");
            singletonInstance = new FfmpegManager();
        }
        return singletonInstance;
    }

    private void init() {
        checkIfExecutableIsValid();
    }


    public boolean checkIfExecutableIsValid() {
        try {
            String command[] = {SettingsManager.getInstance().getFfmpegExecutable(), "-version"};
            Process process = Runtime.getRuntime().exec(command);
            while (process.isAlive()) {
                Thread.sleep(10);
            }
            //TODO - check if executable is indeed ffmpeg. To do that I need to finally
            //export getProcessOutput() from ExecutableWrapper into some utility class.
        } catch (IOException | InterruptedException e) {
            return false;
        }
        return true;
    }

    /**
     * Attempts to convert every file in directory via ffmpeg
     */
    public void convertDirectory(String directory, FILE_EXTENSION fileExtension) {

    }
}
