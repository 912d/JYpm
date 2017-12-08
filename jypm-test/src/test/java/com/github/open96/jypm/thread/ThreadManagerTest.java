package com.github.open96.jypm.thread;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ThreadManagerTest {

    @Before
    public void resetSingleton() {
        try {
            Field singletonInstance = ThreadManager.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
            Field executionAllowance = ThreadManager.class.getDeclaredField("executionPermission");
            executionAllowance.setAccessible(true);
            executionAllowance.set(false, false);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        ThreadManager.getInstance();
    }

    @BeforeClass
    public static void checkInitialState() {
        assertFalse(ThreadManager.getExecutionPermission());
        ThreadManager.getInstance();
        assertTrue(ThreadManager.getExecutionPermission());
    }

    @Test
    public void checkSendTask() {
        ThreadManager threadManager = ThreadManager.getInstance();
        for (TASK_TYPE taskType : TASK_TYPE.values()) {
            try {
                Callable<Boolean> c = () -> true;
                Future<Boolean> future = threadManager.sendTask(c, taskType);
                Boolean b = future.get();
                assertTrue(b);
            } catch (InterruptedException | ExecutionException | RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    public void checkSendVoidTask() {
        ThreadManager threadManager = ThreadManager.getInstance();
        for (TASK_TYPE taskType : TASK_TYPE.values()) {
            final Boolean[] someVariable = {false};
            threadManager.sendVoidTask(new Thread(() -> someVariable[0] = true), taskType);
            try {
                while (!someVariable[0]) {
                    Thread.sleep(1);
                }
                assertTrue(someVariable[0]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    @Test
    public void checkStopAllThreads() {
        ThreadManager threadManager = ThreadManager.getInstance();
        for (TASK_TYPE taskType : TASK_TYPE.values()) {
            threadManager.stopAllThreads();
            assertFalse(ThreadManager.getExecutionPermission());
            try {
                Callable<Boolean> c = () -> true;
                Future<Boolean> future = threadManager.sendTask(c, taskType);
                Boolean b = future.get();
                assertNull(b);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            } catch (RejectedExecutionException e) {
                assertTrue(true);
            }
            //Call resetSingleton manually so ThreadManager will be responsible if ran with other test classes
            resetSingleton();
        }
    }
}
