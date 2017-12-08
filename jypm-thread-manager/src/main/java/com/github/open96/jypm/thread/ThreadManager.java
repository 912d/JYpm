package com.github.open96.jypm.thread;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadManager {
    //This object is a singleton thus storing instance of it is needed
    private static ThreadManager singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(ThreadManager.class.getName());

    private static boolean executionPermission = false;
    private Map<TASK_TYPE, ExecutorService> executorServiceMap;

    private ThreadManager() {
        init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of ThreadManager.
     */
    public static ThreadManager getInstance() {
        if (singletonInstance == null) {
            LOG.debug("Instance is null, initializing...");
            singletonInstance = new ThreadManager();
        }
        return singletonInstance;
    }

    /**
     * @return Permission for threads to execute.
     */
    public static boolean getExecutionPermission() {
        return executionPermission;
    }

    /**
     * Initialize subcomponents on first instance creation.
     */
    private void init() {
        LOG.trace("Initializing ThreadManager");
        //Allow thread execution
        executionPermission = true;
        //Initialize HashMap and populate it
        executorServiceMap = new HashMap<>();
        for (TASK_TYPE taskType : TASK_TYPE.values()) {
            switch (taskType) {
                //UI and OTHER task types can have asynchronous executions at the same time
                case UI:
                case OTHER:
                    executorServiceMap.put(taskType, Executors.newCachedThreadPool());
                    break;
                case CONVERSION:
                    //TODO Make it source thread count from SettingsManager
                    executorServiceMap.put(taskType, Executors.newFixedThreadPool(4));
                    break;
                //Rest of executorServices should only process one task at a time
                default:
                    executorServiceMap.put(taskType, Executors.newSingleThreadExecutor());
            }
        }
        LOG.debug("ThreadManager has been successfully initialized.");
    }

    /**
     * Processes task that is not expected to return any kind of result.
     *
     * @param thread    Task to accomplish
     * @param task_type Type of task
     */
    public void sendVoidTask(Thread thread, TASK_TYPE task_type) {
        executorServiceMap.get(task_type).submit(thread);
    }

    /**
     * Processes task that returns any kind of result and returns said result.
     *
     * @param callable  Task to accomplish
     * @param task_type Type of task
     * @return Result of computed task
     */
    public Future sendTask(Callable callable, TASK_TYPE task_type) {
        return executorServiceMap.get(task_type).submit(callable);
    }

    /**
     * Shutdowns all executorServices in HashMap and stops ongoing threads.
     */
    public void stopAllThreads() {
        executorServiceMap.forEach((taskType, executorService) -> executorService.shutdown());
        executionPermission = false;
    }
}
