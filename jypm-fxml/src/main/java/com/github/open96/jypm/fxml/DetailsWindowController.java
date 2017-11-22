package com.github.open96.jypm.fxml;

import com.github.open96.jypm.download.DownloadManager;
import com.github.open96.jypm.thread.TASK_TYPE;
import com.github.open96.jypm.thread.ThreadManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class DetailsWindowController implements Initializable {

    public static boolean threadKiller = false;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(DetailsWindowController.class.getName());
    @FXML
    ScrollPane scrollPane;

    @FXML
    Text detailsText;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //Start a task on separate Thread that will update text displayed in the window
        ThreadManager
                .getInstance()
                .sendVoidTask(new Thread(() -> {
                    while (ThreadManager.getExecutionPermission()) {
                        try {
                            String details = DownloadManager
                                    .getInstance()
                                    .getDetailsString();
                            if (details != null) {
                                if (!detailsText.getText().equals(details) && details.toCharArray().length >= 0) {
                                    //If details String is different from what is being displayed to user - change it and scroll to its bottom
                                    Platform.runLater(() -> detailsText.setText(details));
                                    scrollPane.setVvalue(1.0);
                                }
                            }
                            Thread.sleep(1000);
                            if (threadKiller) {
                                break;
                            }
                        } catch (InterruptedException | NullPointerException | ArrayIndexOutOfBoundsException | NegativeArraySizeException e) {
                            LOG.error("There was a problem during initialization", e);
                        }
                    }
                }), TASK_TYPE.UI);

    }
}
