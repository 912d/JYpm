package com.github.open96.jypm.playlist.pojo;

import com.github.open96.jypm.playlist.PLAYLIST_STATUS;

public class Playlist {
    private String playlistName;
    private String playlistLink;
    private Integer totalVideoCount;
    private Integer currentVideoCount;
    private String playlistLocation;
    private String playlistThumbnailUrl;
    private PLAYLIST_STATUS status;

    public Playlist(String playlistLink, String playlistLocation) {
        this.playlistLink = playlistLink;
        this.playlistLocation = playlistLocation;
        status = PLAYLIST_STATUS.QUEUED;
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

    public Integer getTotalVideoCount() {
        return totalVideoCount;
    }

    public void setTotalVideoCount(Integer totalVideoCount) {
        this.totalVideoCount = totalVideoCount;
    }

    public Integer getCurrentVideoCount() {
        return currentVideoCount;
    }

    public void setCurrentVideoCount(Integer currentVideoCount) {
        this.currentVideoCount = currentVideoCount;
    }

    public PLAYLIST_STATUS getStatus() {
        return status;
    }

    public void setStatus(PLAYLIST_STATUS status) {
        this.status = status;
    }
}
