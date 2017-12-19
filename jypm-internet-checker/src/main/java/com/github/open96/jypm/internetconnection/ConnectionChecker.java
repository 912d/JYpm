package com.github.open96.jypm.internetconnection;

import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
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
    private volatile static ConnectionChecker singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(ConnectionChecker.class.getName());

    private ConnectionChecker() {
        init();
    }

    //Singleton pattern
    public static ConnectionChecker getInstance() {
        if (singletonInstance == null) {
            synchronized (ConnectionChecker.class) {
                if (singletonInstance == null) {
                    LOG.debug("Instance is null, initializing...");
                    singletonInstance = new ConnectionChecker();
                }
            }
        }
        return singletonInstance;
    }

    /**
     * Method that initializes whole object and loads data from .json file to it if one exists.
     * Singleton design pattern ensures class won't load that data more than once during application runtime
     * and possibly cause desync between what is stored in RAM and what is stored in JSON.
     */
    private void init() {
        LOG.trace("Initializing ConnectionChecker");
        //Define list of links that should be tested for availability
        criticalLinksArray = new ArrayList<>();
        criticalLinksArray.add("localhost");
        criticalLinksArray.add("www.google.com");
        criticalLinksArray.add("www.youtube.com");
        //Initialize variable
        isInternetAvailableWithTimeout = new Pair<>(Boolean.FALSE, new Date());
        //Finally check internet connection
        checkInternetConnection();
        LOG.debug("ConnectionChecker has been successfully initialized");
    }


    /**
     * Checks internet connection to sites application uses
     *
     * @return true if connection to specified links is up and running, false otherwise
     */
    public boolean checkInternetConnection() {
        Callable<Boolean> internetCallable = () -> {
            if (ThreadManager.getExecutionPermission()) {
                //If there was less than 30 seconds since last successful check just return previous value
                if (new Date().getTime() - isInternetAvailableWithTimeout.getValue().getTime() <= 1000 * 30
                        && isInternetAvailableWithTimeout.getKey()) {
                    return isInternetAvailableWithTimeout.getKey();
                }
                //For each link create new thread that will check if address is reachable
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
                    futures.add(ThreadManager
                            .getInstance()
                            .sendTask(pingCallable, TASK_TYPE.OTHER));
                }
                //Start all threads that should check for internet connectivity
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
        //Now start whole thread that is written above this line of code and return it's result
        Future<Boolean> internetFuture = ThreadManager
                .getInstance()
                .sendTask(internetCallable, TASK_TYPE.SETTING);
        try {
            return internetFuture.get();
        } catch (InterruptedException e) {
            LOG.error("Thread has been interrupted", e);
        } catch (ExecutionException e) {
            LOG.error("There was an error during execution", e);
        }
        return false;
    }

}
