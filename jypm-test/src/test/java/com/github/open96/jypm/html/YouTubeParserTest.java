package com.github.open96.jypm.html;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YouTubeParserTest {

    //Feel free to change playlist token and assertions if you don't like default one
    private static final String TEST_PLAYLIST_TOKEN = "PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39";
    //Instance of YouTubeParser that is created before tests are ran
    private static YouTubeParser youTubeParser;

    @BeforeClass
    public static void initialize() {
        youTubeParser = new YouTubeParser(TEST_PLAYLIST_TOKEN);
    }

    @Test
    public void testGetPlaylistName() {
        assertEquals("Test playlist", youTubeParser.getPlaylistName());
    }

    @Test
    public void testGetVideoCount() {
        assertEquals("2", youTubeParser.getVideoCount());
    }

    @Test
    public void testGetThumbnailUrl() {
        assertEquals("https://i.ytimg.com/vi/yVpbFMhOAwE/hqdefault.jpg?" +
                        "sqp=-oaymwEXCPYBEIoBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLDWV3RjB4O1AkB0KolNoZO1AxuOlQ",
                youTubeParser.getThumbnailLink());
    }

}
