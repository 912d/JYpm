package com.github.open96.jypm.ffmpeg;

import com.github.open96.jypm.settings.SettingsManager;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FfmpegManagerTest {
    //IMPORTANT - IF YOU USE DOWNLOADED EXECUTABLE YOU NEED TO CHANGE THIS VARIABLE
    private static final String PATH_TO_FFMPEG = "ffmpeg";


    @Before
    public void resetSingleton() {
        try {
            FfmpegManager.getInstance();
            Field singletonInstance = FfmpegManager.class.getDeclaredField("singletonInstance");
            singletonInstance.setAccessible(true);
            singletonInstance.set(null, null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            System.out.println("Empty API object");
            e.printStackTrace();
        }
        //Call singleton class to let it set itself up again
        FfmpegManager.getInstance();
    }

    @Test
    public void testCheckIfExecutableIsValid() {
        SettingsManager.getInstance().setFfmpegExecutable(PATH_TO_FFMPEG);
        try {
            String command[] = {PATH_TO_FFMPEG, "-version"};
            Process process = Runtime.getRuntime().exec(command);
            while (process.isAlive()) {
                Thread.sleep(10);
            }
            System.out.println(getProcessOutput(process));
        } catch (IOException | InterruptedException e) {
            assertFalse(FfmpegManager.getInstance().checkIfExecutableIsValid());
            e.printStackTrace();
        }
        assertTrue(FfmpegManager.getInstance().checkIfExecutableIsValid());
    }


    //TODO Create Util module and put this code and other universal methods in there
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
}
