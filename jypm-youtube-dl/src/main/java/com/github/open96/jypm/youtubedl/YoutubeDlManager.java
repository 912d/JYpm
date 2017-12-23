package com.github.open96.jypm.youtubedl;

import com.github.open96.jypm.api.github.GitHubApiClient;
import com.github.open96.jypm.api.github.GitHubApiEndpointInterface;
import com.github.open96.jypm.api.github.pojo.release.Asset;
import com.github.open96.jypm.api.github.pojo.release.ReleaseJSON;
import com.github.open96.jypm.internetconnection.ConnectionChecker;
import com.github.open96.jypm.settings.OS_TYPE;
import com.github.open96.jypm.settings.SettingsManager;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;

public class YoutubeDlManager {
    //This object is a singleton thus storing instance of it is needed
    private volatile static YoutubeDlManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(YoutubeDlManager.class.getName());
    //Field to store ready for querying retrofit client
    private GitHubApiEndpointInterface apiService;
    //Field to store online version of youtube-dl
    private String onlineVersion;
    //Field to store executables relevant for application
    private Map<OS_TYPE, Asset> assets;
    //State of YoutubeDlManager
    private EXECUTABLE_STATE executableState;
    //Name of directory where executable will be stored
    private static final String YOUTUBE_DL_DIRECTORY = "youtube-dl";

    private YoutubeDlManager() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of YoutubeDlManager.
     */
    public static YoutubeDlManager getInstance() {
        if (singletonInstance == null) {
            synchronized (YoutubeDlManager.class) {
                if (singletonInstance == null) {
                    LOG.debug("Instance is null, initializing...");
                    singletonInstance = new YoutubeDlManager();
                }
            }
        }
        return singletonInstance;
    }

    /**
     * Initialize subcomponents on first instance creation.
     */
    private void init() {
        LOG.trace("Initializing YoutubeDlManager");

        executableState = EXECUTABLE_STATE.NOT_READY;
        Retrofit retrofit = GitHubApiClient.getInstance().getClient();
        apiService = retrofit.create(GitHubApiEndpointInterface.class);
        downloadYoutubeDl();

        LOG.debug("YoutubeDlManager has been successfully initialized.");
    }

    /**
     * @param releaseJSON Object that has wrapped API response in it.
     * @return Map that contains two executables - one for Windows (key:OS_TYPE.WINDOWS) and
     * for Unix systems(key:OS_TYPE.OPEN_SOURCE_UNIX).
     */
    private Map<OS_TYPE, Asset> getExecutablesAssets(ReleaseJSON releaseJSON) {
        Map<OS_TYPE, Asset> assets = new HashMap<>();
        //Now parse data to object that contains only needed data
        if (releaseJSON != null) {
            LOG.debug("Parsing GitHub api response");
            //From all those assets we get we only need  windows and linux executables
            for (Asset asset : releaseJSON.getAssets()) {
                if (asset.getName().equals("youtube-dl")) {
                    assets.put(OS_TYPE.OPEN_SOURCE_UNIX, asset);
                }
                if (asset.getName().equals("youtube-dl.exe")) {
                    assets.put(OS_TYPE.WINDOWS, asset);
                }
            }
        } else {
            LOG.error("API object is empty!", new IllegalStateException("API object is empty!"));
        }
        if (assets.size() != 2) {
            LOG.fatal(new IllegalStateException("Unexpected number of assets!"));
        }
        return assets;
    }

    /**
     * @param releaseJSON Object that has wrapped API response in it.
     * @return Version of youtube-dl available in github repo.
     */
    private String getOnlineVersion(ReleaseJSON releaseJSON) {
        if (releaseJSON != null) {
            return releaseJSON.getTagName();
        } else {
            LOG.error("API object is empty!", new IllegalStateException("API object is empty!"));
        }
        return null;
    }

    /**
     * Calls GitHub API and wraps response in releaseJSON object.
     */
    private void getAPIResponse() {
        Call<ReleaseJSON> releaseJSONCall = apiService.getLatestRelease("rg3", "youtube-dl");
        try {
            //Cast API responses to objects from api package
            ReleaseJSON releaseJSON = releaseJSONCall.execute().body();
            onlineVersion = getOnlineVersion(releaseJSON);
            assets = getExecutablesAssets(releaseJSON);
        } catch (IOException e) {
            LOG.error("There was an IO error during GitHub api call", e);
        }
    }

    /**
     * Delete recursively directory in which youtube-dl is stored.
     */
    public void deletePreviousVersionIfExists() {
        executableState = EXECUTABLE_STATE.NOT_READY;
        File youtubeDlDirectory = new File(YOUTUBE_DL_DIRECTORY);
        if (youtubeDlDirectory.exists() && youtubeDlDirectory.listFiles() != null) {
            for (File f : youtubeDlDirectory.listFiles()) {
                f.delete();
            }
        }
        youtubeDlDirectory.delete();
        youtubeDlDirectory.mkdir();
    }

    /**
     * Utilizing methods in this class this method checks for youtube-dl update and appends it if needed.
     */
    public void downloadYoutubeDl() {
        File youtubeDlDirectory = new File(YOUTUBE_DL_DIRECTORY);
        boolean doesFileIntegritySeemOk = youtubeDlDirectory.exists()
                && youtubeDlDirectory.isDirectory()
                && youtubeDlDirectory.listFiles() != null
                && youtubeDlDirectory.listFiles().length == 1;
        ThreadManager.getInstance().sendVoidTask(new Thread(() -> {
            if (ConnectionChecker
                    .getInstance()
                    .checkInternetConnection()) {

                getAPIResponse();
                boolean isVersionOutOfDate = (!SettingsManager
                        .getInstance()
                        .getYoutubeDlVersion().equals(onlineVersion));
                if ((isVersionOutOfDate || !doesFileIntegritySeemOk) && ThreadManager.getExecutionPermission()) {
                    LOG.debug("New youtube-dl version available, downloading...");
                    try {
                        //Create URL based on OS type
                        URL downloadLink;
                        Asset asset;
                        if (SettingsManager
                                .getInstance()
                                .getOS() == OS_TYPE.WINDOWS) {
                            asset = assets.get(OS_TYPE.WINDOWS);
                        } else {
                            asset = assets.get(OS_TYPE.OPEN_SOURCE_UNIX);
                        }
                        downloadLink = new URL(asset.getBrowserDownloadUrl());

                        //Clean youtube-dl directory before making any changes to it
                        deletePreviousVersionIfExists();

                        //Download youtube-dl
                        ReadableByteChannel readableByteChannel = Channels.newChannel(downloadLink.openStream());
                        String pathToExecutable = YOUTUBE_DL_DIRECTORY + "/" + asset.getName();
                        FileOutputStream fileOutputStream = new FileOutputStream(pathToExecutable);
                        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
                        fileOutputStream.close();
                        readableByteChannel.close();
                        LOG.debug("youtube-dl " + onlineVersion + " has been downloaded");

                        //Make file executable
                        if (SettingsManager
                                .getInstance()
                                .getOS() != OS_TYPE.WINDOWS) {
                            LOG.debug("Making youtube-dl executable");
                            String[] command = new String[]{"chmod", "+x", pathToExecutable};
                            Runtime.getRuntime().exec(command);
                        }

                        SettingsManager
                                .getInstance()
                                .setYoutubeDlVersion(onlineVersion);
                        SettingsManager
                                .getInstance()
                                .setYoutubeDlExecutable(new File(pathToExecutable).getAbsolutePath());
                        LOG.debug("Download finished");
                    } catch (MalformedURLException e) {
                        LOG.error("Invalid GitHub url", e);
                    } catch (IOException e) {
                        LOG.error("Error during opening stream", e);
                    }
                }
                executableState = EXECUTABLE_STATE.READY;
            } else {
                if (doesFileIntegritySeemOk) {
                    executableState = EXECUTABLE_STATE.READY;
                }
            }
        }), TASK_TYPE.DOWNLOAD);
    }

    public EXECUTABLE_STATE getExecutableState() {
        return executableState;
    }
}
