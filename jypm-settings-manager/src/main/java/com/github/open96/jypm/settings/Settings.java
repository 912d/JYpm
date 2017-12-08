package com.github.open96.jypm.settings;

class Settings {
    private String youtubeDlExecutable;
    private String ffmpegExecutable;
    private OS_TYPE osType;
    private String fileManagerCommand;
    private boolean notificationPolicy;
    private String youtubeDlVersion;
    private String runtimeVersion;
    private Integer ffmpegThreadLimit;

    public Settings() {
        ffmpegExecutable = "";
        youtubeDlExecutable = "";
        fileManagerCommand = "";
        notificationPolicy = true;
        youtubeDlVersion = "";
        runtimeVersion = "";
        ffmpegThreadLimit = 2;
    }

    String getYoutubeDlExecutable() {
        return youtubeDlExecutable;
    }

    void setYoutubeDlExecutable(String youtubeDlExecutable) {
        this.youtubeDlExecutable = youtubeDlExecutable;
    }

    OS_TYPE getOsType() {
        return osType;
    }

    void setOsType(OS_TYPE osType) {
        this.osType = osType;
    }

    String getFileManagerCommand() {
        return fileManagerCommand;
    }

    void setFileManagerCommand(String fileManagerCommand) {
        this.fileManagerCommand = fileManagerCommand;
    }

    boolean getNotificationPolicy() {
        return notificationPolicy;
    }

    void setNotificationPolicy(boolean notificationPolicy) {
        this.notificationPolicy = notificationPolicy;
    }

    String getYoutubeDlVersion() {
        return youtubeDlVersion;
    }

    void setYoutubeDlVersion(String youtubeDlVersion) {
        this.youtubeDlVersion = youtubeDlVersion;
    }

    String getRuntimeVersion() {
        return runtimeVersion;
    }

    void setRuntimeVersion(String runtimeVersion) {
        this.runtimeVersion = runtimeVersion;
    }

    public String getFfmpegExecutable() {
        return ffmpegExecutable;
    }

    public void setFfmpegExecutable(String ffmpegExecutable) {
        this.ffmpegExecutable = ffmpegExecutable;
    }

    public Integer getFfmpegThreadLimit() {
        return ffmpegThreadLimit;
    }

    public void setFfmpegThreadLimit(Integer ffmpegThreadLimit) {
        this.ffmpegThreadLimit = ffmpegThreadLimit;
    }
}
