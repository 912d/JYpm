package com.github.open96.playlist.pojo;

import com.github.open96.playlist.QUEUE_STATUS;

public class Playlist {
    private String playlistName;
    private String playlistLink;
    private Integer videoCount;
    private String playlistLocation;
    private String playlistThumbnailUrl;
    private QUEUE_STATUS status;

    public Playlist(String playlistLink, String playlistLocation) {
        this.playlistLink = playlistLink;
        this.playlistLocation = playlistLocation;
        status = QUEUE_STATUS.QUEUED;
    }

    public String getPlaylistThumbnailUrl() {
        return playlistThumbnailUrl;
    }

    public void setPlaylistThumbnailUrl(String playlistThumbnailUrl) {
        this.playlistThumbnailUrl = playlistThumbnailUrl;
    }

    public String getPlaylistName() {
        return playlistName;
    }

    public void setPlaylistName(String playlistName) {
        this.playlistName = playlistName;
    }

    public String getPlaylistLink() {
        return playlistLink;
    }

    public String getPlaylistLocation() {
        return playlistLocation;
    }

    public Integer getVideoCount() {
        return videoCount;
    }

    public void setVideoCount(Integer videoCount) {
        this.videoCount = videoCount;
    }

    public QUEUE_STATUS getStatus() {
        return status;
    }

    public void setStatus(QUEUE_STATUS status) {
        this.status = status;
    }
}
