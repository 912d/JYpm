package com.github.open96.updater;

import com.github.open96.api.github.GitHubApiClient;
import com.github.open96.api.github.GitHubApiEndpointInterface;
import com.github.open96.api.github.pojo.release.ReleaseJSON;
import com.github.open96.settings.SettingsManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import retrofit2.Call;
import retrofit2.Retrofit;

import java.io.IOException;
import java.util.Properties;

public class Updater {
    //This object is a singleton thus storing instance of it is needed
    private static Updater singletonInstance;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(Updater.class.getName());
    //Field to store ready for querying retrofit client
    private GitHubApiEndpointInterface apiService;
    private String runtimeVersion;
    private ReleaseJSON releaseJSON;

    private Updater() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of Updater.
     */
    public static Updater getInstance() {
        if (singletonInstance == null) {
            log.debug("Instance is null, initializing...");
            singletonInstance = new Updater();
        }
        return singletonInstance;
    }

    /**
     * Initialize subcomponents on first instance creation.
     */
    private void init() {
        log.trace("Initializing Updater");

        //Get runtime version
        try {
            Properties properties = new Properties();
            properties.load(Updater.class.getClassLoader().getResourceAsStream("version.properties"));
            runtimeVersion = properties.getProperty("runtime.version");
        } catch (IOException e) {
            log.error("Missing or corrupt version.properties file!", e);
        }

        SettingsManager.getInstance().setRuntimeVersion(runtimeVersion);

        //Initialize GitHubApiClient
        Retrofit retrofit = GitHubApiClient.getInstance().getClient();
        apiService = retrofit.create(GitHubApiEndpointInterface.class);

        //Query API for releases
        refresh();

        log.debug("Updater has been successfully initialized.");
    }

    /**
     * Checks if JYpm update is available
     *
     * @return Null if no update, String with new update version otherwise
     */
    public String checkForUpdate() {
        if (releaseJSON != null) {
            if (!releaseJSON.getTagName().equals(runtimeVersion)) {
                log.trace("New update available\nCURRENT VERSION: " + runtimeVersion + "\nNEW VERSION: " + releaseJSON.getTagName());
                return releaseJSON.getTagName();
            }
        } else {
            log.error("API object is empty!", new IllegalStateException("API object is empty!"));
        }
        return null;
    }

    /**
     * Refreshes API response
     */
    public void refresh() {
        try {
            Call<ReleaseJSON> releaseJSONCall = apiService.getLatestRelease("Open96", "JYpm");
            //Cast API response to ReleaseJSON
            releaseJSON = releaseJSONCall.execute().body();
            //If api object is empty call API again
            int retryCount = 0;
            while (true) {
                if (releaseJSON == null) {
                    Call<ReleaseJSON> repeatedJSONCall = apiService.getLatestRelease("Open96", "JYpm");
                    releaseJSON = repeatedJSONCall.execute().body();
                    retryCount++;
                    if (retryCount > 10) {
                        log.fatal("Could not get API object...");
                        throw new IllegalStateException("Empty API response");
                    }
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.error("There was an IO error during GitHub API call", e);
        }
    }

    /**
     * @return JSON response from github casted to object
     */
    public ReleaseJSON getJSONObject() {
        return releaseJSON;
    }

}
