package com.github.open96.jypm.thread;

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

    public ThreadManagerTest() {
        ThreadManager.getInstance();
    }

    private void resetSingleton() {
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
    }

    @Test
    public void checkInit() {
        resetSingleton();
        assertFalse(ThreadManager.getExecutionPermission());
        ThreadManager.getInstance();
        assertTrue(ThreadManager.getExecutionPermission());
    }

    @Test
    public void checkSendTask() {
        resetSingleton();
        ThreadManager threadManager = ThreadManager.getInstance();
        try {
            Callable<Boolean> c = () -> true;
            Future<Boolean> future = threadManager.sendTask(c, TASK_TYPE.OTHER);
            Boolean b = future.get();
            assertTrue(b);
        } catch (InterruptedException | ExecutionException e) {
            assertFalse(false);
            e.printStackTrace();
        } catch (RejectedExecutionException e) {
            assertFalse(false);
        }
    }


    @Test
    public void checkSendVoidTask() {
        resetSingleton();
        ThreadManager threadManager = ThreadManager.getInstance();
        final Boolean[] someVariable = {false};
        threadManager.sendVoidTask(new Thread(() -> someVariable[0] = true), TASK_TYPE.OTHER);
        try {
            while (!someVariable[0]) {
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(someVariable[0]);
    }


    @Test
    public void checkStopAllThreads() {
        resetSingleton();
        ThreadManager threadManager = ThreadManager.getInstance();
        threadManager.stopAllThreads();
        assertFalse(ThreadManager.getExecutionPermission());
        try {
            Callable<Boolean> c = () -> true;
            Future<Boolean> future = threadManager.sendTask(c, TASK_TYPE.OTHER);
            Boolean b = future.get();
            assertNull(b);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } catch (RejectedExecutionException e) {
            assertTrue(true);
        }
        resetSingleton();
    }
}
