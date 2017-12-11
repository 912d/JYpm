package com.github.open96.jypm.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
    public String getProcessOutput() throws IOException, InterruptedException {
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
}
