package com.github.open96.jypm.download;

import com.github.open96.jypm.playlist.pojo.Playlist;
import com.github.open96.jypm.settings.SettingsManager;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import com.github.open96.jypm.youtubedl.YoutubeDlManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Wrapper that directly takes care of issuing commands in command line
 */
public class ExecutableWrapper {
    private final static String BASE_YOUTUBE_URL = "https://www.youtube.com/playlist?list=";
    //This object is a singleton thus storing instance of it is needed
    private static ExecutableWrapper singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(ExecutableWrapper.class.getName());
    //Store SettingsManager for easier usage
    private SettingsManager settingsManager;
    //Create variable for runtime where commands will be issued
    private Runtime runtime;

    private ExecutableWrapper() {
        init();
    }

    /**
     * Create an instance that will be always returned when running that method
     *
     * @return Singleton instance of ExecutableWrapper
     */
    public static ExecutableWrapper getInstance() {
        if (singletonInstance == null) {
            LOG.debug("Instance is null, initializing...");
            singletonInstance = new ExecutableWrapper();
        }
        return singletonInstance;
    }

    /**
     * Initialize subcomponents on first instance creation
     */
    private void init() {
        LOG.trace("Initializing ExecutableWrapper");

        settingsManager = SettingsManager.getInstance();
        runtime = Runtime.getRuntime();

        LOG.debug("ExecutableWrapper has been successfully initialized.");
    }

    /**
     * Query executable specified by SettingsManager for it's version
     *
     * @return Output of command "path-to-executable --version"
     */
    public String getYoutubeDlVersion() {
        try {
            String command[] = {settingsManager.getYoutubeDlExecutable(), "--version"};
            Process process = runtime.exec(command);
            return getProcessOutput(process);
        } catch (IOException e) {
            LOG.error("There was an error when querying executable for version", e);
            triggerExecutableRedownload();
            return "";
        } catch (InterruptedException e) {
            LOG.error(e);
            return "";
        }
    }

    /**
     * Downloads playlist with youtube-dl
     *
     * @param playlist Playlist that should be downloaded
     * @return Process of executed command
     */
    Process downloadPlaylist(Playlist playlist) throws IOException {
        //Check if there is at least something set as executable
        if (getYoutubeDlVersion().equals("")) {
            return null;
        }
        //Issue main youtube-dl command for an actual download and return it's process for further operations on it
        String command[] = {settingsManager
                .getYoutubeDlExecutable(), "-i", "-o %(title)s.%(ext)s", BASE_YOUTUBE_URL + playlist.getPlaylistLink()};
        return runtime.exec(command, null, new File(playlist.getPlaylistLocation()));
    }

    /**
     * Casts Process's command line output to string
     *
     * @param process Process that output should be read.
     * @return String with process's output
     * This function at worst case will only read one line, and probably not more in best case
     * but it's ok as on correct path to executable it will only receive one line of output anyways.
     */
    private String getProcessOutput(Process process) throws IOException, InterruptedException {
        //Create BufferedReader that will read process's output
        try (InputStream inputStream = process.getInputStream()) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder output = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null || process.isAlive()) {
                if (line != null) {
                    output.append(line);
                }
            }
            process.waitFor();
            bufferedReader.close();
            return output.toString();
        }
    }

    void triggerExecutableRedownload() {
        LOG.info("Executable redownload triggered");
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            SettingsManager.getInstance().setYoutubeDlVersion("");
            while (!SettingsManager.getInstance().getYoutubeDlVersion().equals("")) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            YoutubeDlManager.getInstance().downloadYoutubeDl();
        }), TASK_TYPE.DOWNLOAD);
    }

}
