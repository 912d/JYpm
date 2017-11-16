package com.github.open96.tray;

import com.github.open96.settings.SettingsManager;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.ActionListener;
import java.io.IOException;

public class TrayIcon {

    //Variable that stores object of this class
    private static TrayIcon singletonInstance;
    //Initialize log4j logger for later use in this class
    private static final Logger LOG = LogManager.getLogger(TrayIcon.class.getName());
    //Stage to which tray icon should be linked, preferably the main one
    private static Stage mainWindowStage;
    private static boolean isTrayWorking = false;
    //Instance of an actual TrayIcon object
    private java.awt.TrayIcon trayIcon;


    private TrayIcon() {
        isTrayWorking = init();
    }

    /**
     * Method that ensures object will be initialized only once.
     * For reasoning behind using this design pattern look into init() method.
     *
     * @return Singleton instance of PlaylistManager.
     */
    public static TrayIcon getInstance() {
        if (singletonInstance == null) {
            LOG.debug("Instance is null, initializing...");
            singletonInstance = new TrayIcon();
        }
        return singletonInstance;
    }

    /**
     * @param mainWindowStage Stage that should be "root" of tray icon.
     */
    public static void setMainWindowStage(Stage mainWindowStage) {
        TrayIcon.mainWindowStage = mainWindowStage;
    }

    /**
     * @return Current state of TrayIcon (e.g. if it now being displayed or not)
     */
    public static boolean isTrayWorking() {
        return isTrayWorking;
    }

    /**
     * Initialize subcomponents on first instance creation.
     */
    private boolean init() {
        LOG.trace("Initializing TrayIcon");
        if (mainWindowStage != null) {
            LOG.debug("TrayIcon has been successfully initialized.");
            return createSystemTrayIcon(mainWindowStage);
        } else {
            LOG.error("No stage has been set, invoke setMainWindowStage() before calling the constructor");
        }
        return false;
    }

    /**
     * Creates tray icon using AWT library.
     */
    private boolean createSystemTrayIcon(Stage stage) {

        if (SystemTray.isSupported()) {
            try {
                //Cast SystemTray class to object
                SystemTray systemTray = SystemTray.getSystemTray();

                //Create popup menu
                PopupMenu popupMenu = new PopupMenu();

                //This action will be linked to more than one button, so it is better to cast it to a variable
                ActionListener maximize = e -> {
                    LOG.info("Maximizing from tray.");
                    //Show window to user
                    Platform.runLater(() -> {
                        stage.show();
                        stage.toFront();
                        //Restore default JavaFX behaviour when stage.close() is invoked.
                        Platform.setImplicitExit(true);
                    });
                };

                //Create popup menu entries
                MenuItem showWindow = new MenuItem("Show window");
                showWindow.addActionListener(maximize);

                MenuItem exitApplication = new MenuItem("Exit JYpm");
                exitApplication.addActionListener(e -> Platform.runLater(() -> {
                    Platform.setImplicitExit(true);
                    stage.close();
                }));

                //Add menu entries to popup menu
                popupMenu.add(showWindow);
                popupMenu.add(exitApplication);

                //Create tray icon
                trayIcon = new java.awt.TrayIcon(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon/launcher-128-128.png")),
                        "JYpm",
                        popupMenu);

                //Let image resize itself if needed
                trayIcon.setImageAutoSize(true);

                //Add action on icon click
                trayIcon.addActionListener(maximize);

                //Add tray icon to system tray
                systemTray.add(trayIcon);
                LOG.debug("Tray icon has been successfully created!");
                return true;
            } catch (AWTException e) {
                LOG.error("Something went wrong during system tray icon creation.", e);
                Platform.exit();
            }
        } else {
            LOG.warn("No system tray detected, disabling some features...");
        }
        return false;
    }

    /**
     * Displays notification with one of the libraries depending on OS application runs on.
     *
     * @param title       Title of notification.
     * @param description Description of notification.
     * @return true if notification was displayed, false otherwise.
     */
    public boolean displayNotification(String title, String description) {
        if (SettingsManager
                .getInstance()
                .getNotificationPolicy()) {
            switch (SettingsManager
                    .getInstance()
                    .getOS()) {
                case WINDOWS:
                    trayIcon.displayMessage(title, description, java.awt.TrayIcon.MessageType.INFO);
                    return true;
                case UNKNOWN:
                case OPEN_SOURCE_UNIX:
                    try {
                        ProcessBuilder processBuilder = new ProcessBuilder("notify-send", "-u", "normal", "-c", "im", title, description);
                        processBuilder.start();
                        return true;
                    } catch (IOException e) {
                        LOG.error("There was an error while displaying notification", e);
                        return false;
                    }
                case MAC_OS:
                    //I don't have tools to check if that works, so I decided to leave it blank for the time being
                    break;
            }
        }
        return false;
    }

    /**
     * Removes tray icon from SystemTray.
     */
    public void removeTrayIcon() {
        SystemTray.getSystemTray().remove(trayIcon);
    }
}
