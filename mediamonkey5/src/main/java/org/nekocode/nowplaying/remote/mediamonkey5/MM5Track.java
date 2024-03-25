package org.nekocode.nowplaying.remote.mediamonkey5;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Log4j2
public class MM5Track implements Track {
    private final MM5Connection connection;
    private final int persistentId;
    private final String title;
    private final String artist;
    private final String album;
    private final int rating;
    private final String genre;
    private final String comment;
    private final String grouping1;
    private final double duration;
    private final Object artworkLock = new Object();
    private final Collection<ImageIcon> artwork = new ArrayList<>();

    public static Track getCurrentTrack(MM5Connection connection) {
        try {
            Map<String, Object> trackProperties = connection.evaluate("app.player.getCurrentTrack()");
            return createMM5Track(connection, trackProperties);
        } catch (Exception e) {
            log.error("Error parsing property map", e);
            return ErrorTrack.ERROR_TRACK;
        }
    }

    public static Track getTrack(MM5Connection connection, int trackId) {
        try {
            Map<String, Object> trackProperties = connection.evaluate(
                    "app.getObject('track', { id: %d })".formatted(trackId));
            return createMM5Track(connection, trackProperties);
        } catch (Exception e) {
            log.error("Error parsing property map", e);
            return ErrorTrack.ERROR_TRACK;
        }
    }

    @NotNull
    private static MM5Track createMM5Track(MM5Connection connection, Map<String, Object> trackProperties) {
        String title = (String) trackProperties.get("title");
        String artist = (String) trackProperties.get("artist");
        String album = (String) trackProperties.get("album");
        String genre = (String) trackProperties.get("genre");
        String comment = (String) trackProperties.get("commentShort");
//        String path = trackProperties.get("path");
        String grouping1 = (String) trackProperties.get("custom1"); // anime series
//        String custom2 = trackProperties.get("custom2"); // anime
        Integer id = (Integer) trackProperties.get("id"); // idsong and persistentID contain the same value
        int rating = (Integer) trackProperties.get("rating");
        double songLength = (Integer) trackProperties.get("songLength") / 1000d; // units are MS

        return new MM5Track(connection, id, title, artist, album, rating, genre, comment, grouping1, songLength);
    }

    @Override
    public String getAlbum() {
        return album;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public boolean isCompilation() {
        return false;
    }

    @Override
    public String getComposer() {
        return null;
    }

    @Override
    public int getDiscCount() {
        return 0;
    }

    @Override
    public int getDiscNumber() {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getGenre() {
        return genre;
    }

    @Override
    public String getGrouping() {
        return grouping1;
    }

    @Override
    public int getPlayedCount() {
        return 0;
    }

    @Override
    public Date getPlayedDate() {
        return null;
    }

    @Override
    public int getRating() {
        return rating;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public int getTrackCount() {
        return 0;
    }

    @Override
    public int getTrackNumber() {
        return 0;
    }

    @Override
    public int getVolumeAdjustment() {
        return 0;
    }

    @Override
    public int getYear() {
        return 0;
    }

    @Override
    public int getBitRate() {
        return 0;
    }

    @Override
    public int getBpm() {
        return 0;
    }

    @Override
    public Date getDateAdded() {
        return null;
    }

    @Override
    public double getDuration() {
        return duration;
    }

    @Override
    public Date getModificationDate() {
        return null;
    }

    @Override
    public int getSampleRate() {
        return 0;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String getTime() {
        return null;
    }

    @Override
    public int getTrackId() {
        return persistentId;
    }

    @Override
    public long getDatabaseId() {
        return 0;
    }

    @Override
    public Object getOriginal() {
        return null;
    }

    @Override
    public String getPersistentId() {
        return persistentId + "";
    }

    @Override
    public Collection<ImageIcon> getArtwork(int size) {
        synchronized (artworkLock) {
            if (!artwork.isEmpty()) {
                return artwork.stream()
                        .map(image -> resizeArt(image, size))
                        .collect(Collectors.toList());
            }
        }

        // asynchronously acquire a reference to the artwork
        // TODO figure out how to do this synchronously
        String getCover = """
                    app.getObject('track', { id: %d })
                        .then(function(track) {
                            if (track) {
                                var cover = track.getThumbAsync(%d, %d,
                                    function(thumb) {
                                        console.debug('thumbnail:%d:' + thumb);
                                    });
                            }
                        });
                """.formatted(persistentId, size, size, persistentId);
        try {
            connection.evaluateAsync(getCover);
        } catch (ScriptException e) {
            log.error("Error loading cover art", e);
        }
        return Collections.emptyList();
    }

    /**
     * only scales down
     */
    private ImageIcon resizeArt(ImageIcon imageIcon, int size) {
        if (imageIcon.getIconWidth() > size || imageIcon.getIconHeight() > size) {
            Image image = imageIcon.getImage();
            int height = imageIcon.getIconHeight() >= imageIcon.getIconWidth()
                    ? size
                    : -1;
            int width = imageIcon.getIconWidth() >= imageIcon.getIconHeight()
                    ? size
                    : -1;
            Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(newimg);
        } else {
            return imageIcon;
        }
    }

    public void addArtwork(ImageIcon imageIcon) {
        synchronized (artworkLock) {
            artwork.add(imageIcon);
        }
    }
}
