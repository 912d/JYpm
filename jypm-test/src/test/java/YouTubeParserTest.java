import com.github.open96.jypm.html.YouTubeParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YouTubeParserTest {


    @Test
    public void testGetPlaylistName() {
        YouTubeParser youTubeParser = new YouTubeParser("PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39");
        String playlistName = youTubeParser.getPlaylistName();
        assertEquals("Test playlist", playlistName);
    }

    @Test
    public void testGetVideoCount() {
        YouTubeParser youTubeParser = new YouTubeParser("PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39");
        String videoCount = youTubeParser.getVideoCount();
        assertEquals("2", videoCount);
    }

    @Test
    public void testGetThumbnailUrl() {
        YouTubeParser youTubeParser = new YouTubeParser("PLK1OE0wPYodKbHOX0pd5nD4TTT9CflG39");
        String thumbnailUrl = youTubeParser.getThumbnailLink();
        //Not sure about that as I'm not 100% sure link doesn't change.
        assertEquals("https://i.ytimg.com/vi/yVpbFMhOAwE/hqdefault.jpg?sqp=-oaymwEXCPYBEIoBSFryq4qpAwkIARUAAIhCGAE=&rs=AOn4CLDWV3RjB4O1AkB0KolNoZO1AxuOlQ", thumbnailUrl);
    }

}
