package com.github.open96.jypm.util;

import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProcessWrapper {
    private Process process;

    public ProcessWrapper(Process process) {
        this.process = process;
    }

    /**
     * Casts Process's command line output to string
     *
     * @return String with process's output
     */
    public String getProcessOutput() throws InterruptedException {
        //Create BufferedReader that will read process's output
        Callable<String> processOutputGetter = () -> {
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
        };
        Future<String> processOutputFuture = ThreadManager.getInstance().sendTask(processOutputGetter, TASK_TYPE.OTHER);
        try {
            return processOutputFuture.get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }
}
