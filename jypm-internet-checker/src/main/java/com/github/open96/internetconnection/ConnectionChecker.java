package com.github.open96.internetconnection;

import com.github.open96.thread.TASK_TYPE;
import com.github.open96.thread.ThreadManager;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ConnectionChecker {
    //Set of links that will be tested for internet connection
    private static ArrayList<String> criticalLinksArray;
    //Variable in which connection status is stored
    private Pair<Boolean, Date> isInternetAvailableWithTimeout;
    //Singleton instance of this class
    private static ConnectionChecker singletonInstance;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(ConnectionChecker.class.getName());

    private ConnectionChecker() {
        init();
    }

    //Singleton pattern
    public static ConnectionChecker getInstance() {
        if (singletonInstance == null) {
            log.debug("Instance is null, initializing...");
            singletonInstance = new ConnectionChecker();
        }
        return singletonInstance;
    }

    /**
     * Method that initializes whole object and loads data from .json file to it if one exists.
     * Singleton design pattern ensures class won't load that data more than once during application runtime
     * and possibly cause desync between what is stored in RAM and what is stored in JSON.
     */
    private void init() {
        log.trace("Initializing ConnectionChecker");
        //Define list of links that should be tested for availability
        criticalLinksArray = new ArrayList<>();
        criticalLinksArray.add("localhost");
        criticalLinksArray.add("www.google.com");
        criticalLinksArray.add("www.youtube.com");
        //Initialize variable
        isInternetAvailableWithTimeout = new Pair<>(Boolean.FALSE, new Date());
        //Finally check internet connection
        checkInternetConnection();
        log.debug("ConnectionChecker has been successfully initialized");
    }


    /**
     * Checks internet connection to sites application uses
     * NOTE: This operation is fairly memory expensive as it uses finalizers which reside in heap for a long time.
     * Only use it when necessary.
     *
     * @return true if connection is up and running, false otherwise
     */
    public boolean checkInternetConnection() {
        Callable<Boolean> internetCallable = () -> {
            if (ThreadManager.getExecutionPermission()) {
                //If there was less than 30 seconds since last successful check just return previous value
                if (new Date().getTime() - isInternetAvailableWithTimeout.getValue().getTime() <= 1000 * 30 && isInternetAvailableWithTimeout.getKey()) {
                    return isInternetAvailableWithTimeout.getKey();
                }
                List<Future<Boolean>> futures = new ArrayList<>();
                for (String address : criticalLinksArray) {
                    Callable<Boolean> pingCallable = () -> {
                        String[] command = {"ping", "-c 1", address};
                        Process p = Runtime.getRuntime().exec(command);
                        p.waitFor();
                        if (p.exitValue() != 0) {
                            return false;
                        }
                        return true;
                    };
                    futures.add(ThreadManager.getInstance().sendTask(pingCallable, TASK_TYPE.OTHER));
                }
                for (Future<Boolean> future : futures) {
                    if (!future.get()) {
                        isInternetAvailableWithTimeout = new Pair<>(Boolean.TRUE, new Date());
                        return false;
                    }
                }
                isInternetAvailableWithTimeout = new Pair<>(Boolean.TRUE, new Date());
                return true;
            }
            return false;
        };
        Future<Boolean> internetFuture = ThreadManager.getInstance().sendTask(internetCallable, TASK_TYPE.SETTING);
        try {
            return internetFuture.get();
        } catch (InterruptedException e) {
            log.error("Thread has been interrupted", e);
        } catch (ExecutionException e) {
            log.error("There was an error during execution", e);
        }
        return false;
    }

}
