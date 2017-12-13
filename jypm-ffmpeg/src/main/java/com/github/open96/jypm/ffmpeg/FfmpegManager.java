package com.github.open96.jypm.ffmpeg;


import com.github.open96.jypm.settings.SettingsManager;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class FfmpegManager {
    //This object is a singleton thus storing instance of it is needed
    private volatile static FfmpegManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(FfmpegManager.class.getName());
    //Allowed bitrate options for mp3
    private static final Integer[] availableBitrates = {
            8, 16, 24, 32, 40, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320
    };


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
            synchronized (FfmpegManager.class) {
                if (singletonInstance == null) {
                    LOG.debug("Instance is null, initializing...");
                    singletonInstance = new FfmpegManager();
                }
            }
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
    public boolean convertDirectory(String directory, FILE_EXTENSION targetExtension, Integer bitrate) {
        //Check if bitrate is supported
        if (Arrays.stream(availableBitrates)
                .noneMatch(allowedBitrate -> allowedBitrate.equals(bitrate))) {
            LOG.error("Unsupported bitrate!");
            return false;
        }
        //Save state of directory in variable so converted files will be filtered out later
        File targetDirectory = new File(directory);
        File[] directoryContentsBeforeConversion = targetDirectory.listFiles();
        //For each file in that directory - run ffmpeg
        if (directoryContentsBeforeConversion != null) {
            Runtime runtime = Runtime.getRuntime();
            for (File file : directoryContentsBeforeConversion) {
                //Issue conversion task
                ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
                    try {
                        //Create command that will be issued via Runtime
                        String command[] = createCommand(file.getName(), targetExtension);
                        //Run ffmpeg
                        Process p = runtime.exec(command, null, targetDirectory);
                        //Wait until it finishes
                        while (p.isAlive()) {
                            Thread.sleep(100);
                        }
                    } catch (IOException | InterruptedException e) {
                        LOG.error("Conversion failed", e);
                    }
                }), TASK_TYPE.CONVERSION);
            }
        } else {
            LOG.warn("No files in directory, conversion is not possible...");
            return false;
        }
        return true;
    }


    private String[] createCommand(String filename, FILE_EXTENSION extension) {
        String command[] = {SettingsManager.getInstance().getFfmpegExecutable(), "-i", filename};
        String filenameWithoutExtension = filename.split("\\.")[0];
        switch (extension) {
            case MP3:
                String extensionCommand[] = {"-codec:a", "libmp3lame", "-b:a", "320k", filenameWithoutExtension + ".mp3"};
                return ArrayUtils.addAll(command, extensionCommand);
            case MP4:
                //TODO
                break;
            default:
                LOG.error("Extension is not supported!");
        }
        return null;
    }

}
