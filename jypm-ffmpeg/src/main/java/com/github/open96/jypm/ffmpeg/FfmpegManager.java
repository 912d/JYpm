package com.github.open96.jypm.ffmpeg;


import com.github.open96.jypm.playlist.PLAYLIST_STATUS;
import com.github.open96.jypm.playlist.PlaylistManager;
import com.github.open96.jypm.settings.SettingsManager;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import com.github.open96.jypm.util.ProcessWrapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FfmpegManager {
    //This object is a singleton thus storing instance of it is needed
    private volatile static FfmpegManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(FfmpegManager.class.getName());
    //Allowed bitrate options for mp3
    public static final Integer[] availableBitrates = {
            8, 16, 24, 32, 40, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320
    };
    //List of booleans that stores state of conversion tasks
    List<Boolean> conversionProgress;


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
        if (!checkIfExecutableIsValid()) {
            LOG.warn("Ffmpeg executable is not valid, change that in settings!");
        }
        resetInterruptedPlaylistsState();
        LOG.debug("FfmpegManager has been successfully initialized");
    }


    public boolean checkIfExecutableIsValid() {
        try {
            String command[] = {SettingsManager.getInstance().getFfmpegExecutable(), "-version"};
            Process process = Runtime.getRuntime().exec(command);
            while (process.isAlive()) {
                Thread.sleep(10);
            }
            //Check if executable is indeed ffmpeg.
            ProcessWrapper processWrapper = new ProcessWrapper(process);
            if (!processWrapper.getProcessOutput().contains("ffmpeg version ")) {
                return false;
            }
        } catch (IOException | InterruptedException e) {
            return false;
        }
        return true;
    }


    public List<Boolean> getConversionProgress() {
        return conversionProgress;
    }

    /**
     * Attempts to convert every file in directory via ffmpeg
     */
    public List<Boolean> convertDirectory(String directory, FILE_EXTENSION targetExtension, Integer bitrate) {
        //Initialize list which will contain states of ffmpeg tasks
        List<Boolean> taskList = new ArrayList<>();
        //Check if bitrate is supported
        if (Arrays.stream(availableBitrates)
                .noneMatch(allowedBitrate -> allowedBitrate.equals(bitrate))) {
            if (bitrate != null && targetExtension == FILE_EXTENSION.MP3) {
                LOG.error("Unsupported bitrate!");
                return null;
            }
        }
        //Save state of directory in variable so converted files will be filtered out later
        File targetDirectory = new File(directory);
        File[] directoryContentsBeforeConversion = targetDirectory.listFiles();
        //For each file in that directory - run ffmpeg
        if (directoryContentsBeforeConversion != null) {
            LOG.trace("Starting conversion of directory: " + directory
                    + "\nFormat: " + targetExtension.toString());
            Runtime runtime = Runtime.getRuntime();
            for (File file : directoryContentsBeforeConversion) {
                taskList.add(Boolean.FALSE);
                int positionInList = taskList.size() - 1;
                //Issue conversion task
                ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
                    try {
                        //Create command that will be issued via Runtime
                        String command[] = createCommand(file.getName(), targetExtension, bitrate);
                        if (command != null) {
                            if (file.getName().endsWith(targetExtension.toString())) {
                                LOG.trace(file.getName() + " - Conversion from to same format is pointless, skipping");
                            } else {
                                if (ThreadManager.getExecutionPermission()) {
                                    //Run ffmpeg
                                    Process p = runtime.exec(command, null, targetDirectory);
                                    //Wait until it finishes
                                    while (p.isAlive() && ThreadManager.getExecutionPermission()) {
                                        Thread.sleep(100);
                                    }
                                }
                            }
                        }
                        taskList.set(positionInList, Boolean.TRUE);
                    } catch (IOException | InterruptedException e) {
                        LOG.error("Conversion failed", e);
                    }
                }), TASK_TYPE.CONVERSION);
            }
        } else {
            LOG.warn("No files in directory, conversion is not possible...");
            return null;
        }
        conversionProgress = taskList;
        return taskList;
    }


    private String[] createCommand(String filename, FILE_EXTENSION extension, Integer bitrate) {
        String command[] = {SettingsManager.getInstance().getFfmpegExecutable(), "-threads", "1", "-y", "-i", filename};
        String filenameWithoutExtension = filename.split("\\.")[0];
        String extensionCommand[];
        switch (extension) {
            case MP3:
                extensionCommand = new String[]{"-codec:a", "libmp3lame", "-b:a", bitrate + "k", filenameWithoutExtension + ".mp3"};
                return ArrayUtils.addAll(command, extensionCommand);
            case MP4:
                extensionCommand = new String[]{filenameWithoutExtension + ".mp4"};
                return ArrayUtils.addAll(command, extensionCommand);
            default:
                LOG.error("Extension is not supported!");
        }
        return null;
    }


    private void resetInterruptedPlaylistsState() {
        PlaylistManager
                .getInstance()
                .getPlaylists()
                .stream()
                .filter(playlist -> playlist.getStatus() == PLAYLIST_STATUS.CONVERTING)
                .forEach(playlist -> playlist.setStatus(PLAYLIST_STATUS.DOWNLOADED));
    }

}
