/*
 * Copyright (c) 2024. Dan Clark
 */

package org.nekocode.nowplaying.remote.mediamonkey5;

import org.nekocode.nowplaying.objects.Track;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Log4j2
public class MM5TrackFactory {

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
        CompletableFuture<Track> trackFuture = new CompletableFuture<>();
        PropertyChangeListener getTrackListener = event -> {
            log.info("getTrack/binding: {}", event.getNewValue());
            @SuppressWarnings("unchecked") // we know it's a Map from MM5Connection
            Map<String, Object> trackProperties = (Map<String, Object>) event.getNewValue();
            Track track = createMM5Track(connection, trackProperties);
            if (track.getTrackId() == trackId) {
                trackFuture.complete(track);
            }
        };
        try {
            connection.addBindingListener(getTrackListener);
            connection.evaluateAsync(
                    STR."""
                        app.getObject('track', { id: \{trackId} })
                           .then(function(track) { return window.getTrack(track.asJSON); })
                    """);
            return trackFuture.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Error parsing property map", e);
            return ErrorTrack.ERROR_TRACK;
        } finally {
            connection.removeBindingListener(getTrackListener);
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
        int id = ((Number) trackProperties.get("id")).intValue(); // idsong and persistentID contain the same value
        int rating = ((Number) trackProperties.get("rating")).intValue();
        double songLength = ((Number) trackProperties.get("songLength")).longValue() / 1000d; // units are MS
        int trackNumber = Integer.parseInt((String) trackProperties.get("trackNumber"));

        return new MM5Track(connection, id, title, artist, album, rating, genre, comment, grouping1, songLength, trackNumber);
    }
}
