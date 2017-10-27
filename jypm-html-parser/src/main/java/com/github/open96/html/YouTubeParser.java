package com.github.open96.html;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;

public class YouTubeParser {
    private final static String BASE_YOUTUBE_URL = "https://www.youtube.com/playlist?list=";
    //Storage for downloaded html that will be parsed
    private Document document;
    //Initialize log4j logger for later use in this class
    private static Logger log = LogManager.getLogger(YouTubeParser.class.getName());

    /**
     * @param playlistToken Suffix of youtube playlist address
     */
    public YouTubeParser(String playlistToken) {
        try {
            String htmlLink = BASE_YOUTUBE_URL + playlistToken;
            log.trace("Attempting to connect and save " + htmlLink);
            Connection connection = Jsoup.connect(htmlLink).userAgent("Mozilla/5.0");
            document = connection.get();
            log.trace("Connection successful");
        } catch (IOException e) {
            document = null;
            log.error("Connection failed", e);
        }
    }

    /**
     * @return Name of playlist
     */
    public String getPlaylistName() {
        if (document != null) {
            Elements element = document.select("[class*=\"-header\"] > [class*=\"header-title\"]");
            log.debug("Got playlist name: " + element.get(1).text());
            return element.get(1).text();
        }
        return "";
    }

    /**
     * @return Video count of playlist
     */
    public String getVideoCount() {
        if (document != null) {
            Elements element = document.select("[class*=\"-header\"] > li:nth-child(2)");
            String videoCount = element.get(0).text();
            videoCount = videoCount.replaceAll("[a-z]", "");
            videoCount = videoCount.replaceAll(" ", "");
            log.debug("Got playlist video count: " + videoCount);
            return videoCount;
        }
        return "";
    }

    /**
     * @return Link to thumbnail of playlist
     */
    public String getThumbnailLink() {
        if (document != null) {
            Elements element = document.select("[class*=\"-header\"] > img");
            log.debug("Got playlist thumbnail: " + element.get(1).absUrl("src"));
            return element.get(1).absUrl("src");
        }
        return "";
    }

}
