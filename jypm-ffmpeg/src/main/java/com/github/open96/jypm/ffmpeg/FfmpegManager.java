package com.github.open96.jypm.ffmpeg;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    }


    public boolean checkIfExecutableIsValid() {
        return false;
    }
}
