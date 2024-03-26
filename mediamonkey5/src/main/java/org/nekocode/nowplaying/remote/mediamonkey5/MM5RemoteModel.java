/*
 * Copyright (c) 2024. Dan Clark
 */

package org.nekocode.nowplaying.remote.mediamonkey5;

import org.nekocode.nowplaying.AbstractMediaPlayer;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.objects.Playlist;
import org.nekocode.nowplaying.objects.Track;

import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.ART_CHANGE;
import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.CURRENT_SONG_CHANGE;
import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.METADATA_CHANGE;
import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.PLAY_STATE_CHANGE;

@Log4j2
public class MM5RemoteModel extends AbstractMediaPlayer {

    private final MM5Connection connection;
    private Track currentTrack;

    public MM5RemoteModel() {
        connection = new MM5Connection();

        connection.addPropertyChangeListener(e -> {
            switch (e.getPropertyName()) {
                case "seekChange" -> {
                    // seekChange happens on rating update
                    Track currentTrack = getCurrentTrack();
                    if (currentTrack != null) {
                        fireTrackChanged(new TrackChangeEvent(currentTrack, PLAY_STATE_CHANGE));
                    }
                }
                case "playbackState" -> {
                    switch (e.getNewValue().toString()) {
                        case "trackChanged" -> {
                            MM5RemoteModel.this.currentTrack = MM5TrackFactory.getCurrentTrack(connection);
                            fireTrackChanged(new TrackChangeEvent(MM5RemoteModel.this.currentTrack, CURRENT_SONG_CHANGE));
                        }
                        case "play", "pause", "unpause" -> {
                            Track currentTrack = getCurrentTrack();
                            if (currentTrack != null) {
                                fireTrackChanged(new TrackChangeEvent(currentTrack, PLAY_STATE_CHANGE));
                            }
                        }
                    }
                }
//                case "playbackEnd" -> {
//                    MM5RemoteModel.this.currentTrack = MM5Track.getCurrentTrack(connection);
//                    fireTrackChanged(new TrackChangeEvent(MM5RemoteModel.this.currentTrack, CURRENT_SONG_CHANGE));
//                }
                case "thumbnail" -> {
                    log.debug("thumbnail update for track {}: {}", e.getOldValue(), e.getNewValue());
                    String trackId = (String) e.getOldValue();
                    Track currentTrack = getCurrentTrack();
                    if (currentTrack instanceof MM5Track mmTrack) {
                        if (Objects.equals(mmTrack.getPersistentId(), trackId)) {
                            // TODO do in background
                            String url = (String) e.getNewValue();
                            String filterTemp = url.replace("file:///temp/", "");
                            filterTemp = filterTemp.replace("file:///", "");
                            Path mmPath = Path.of(filterTemp);
                            Path fullPath = Path.of(System.getProperty("java.io.tmpdir")).resolve(mmPath);
                            log.info("thumbnail for track {} available at: {}", trackId, fullPath);
                            mmTrack.addArtwork(url, new ImageIcon(fullPath.toString()));
                            fireTrackChanged(new TrackChangeEvent(currentTrack, ART_CHANGE));
                        }
                    }
                }
                case "trackModified" -> {
                    String trackId = (String) e.getNewValue();
                    if (currentTrack instanceof MM5Track mmTrack) {
                        if (Objects.equals(mmTrack.getPersistentId(), trackId)) {
                            // reload the current track and then push out an update for whatever changed
                            currentTrack = MM5TrackFactory.getCurrentTrack(connection);
                            switch (isSimpleChange(mmTrack, currentTrack)) {
                                case SIMPLE -> fireTrackChanged(new TrackChangeEvent(currentTrack, METADATA_CHANGE));
                                case COMPLEX -> fireTrackChanged(new TrackChangeEvent(currentTrack, CURRENT_SONG_CHANGE));
                            }
                        }
                    }
                }
                case "findTracks" -> {
                    addFindTracksResult(UUID.fromString(e.getOldValue().toString()), e.getNewValue().toString());
                }
            }
        });
    }

    enum ChangeType {SIMPLE, COMPLEX, UNUSED}

    /**
     * Determines if the change from oldTrack to currentTrack represents a simple change.
     *
     * @param  oldTrack      the old track
     * @param  currentTrack  the current track
     * @return               the type of change
     */
    private ChangeType isSimpleChange(Track oldTrack, Track currentTrack) {
        // TODO there doesn't seem to be a way to check if an artwork has changed without loading the artwork

        // these changes are simple
        boolean isSimpleChange = Stream.<Function<Track, ?>>of(
                        Track::getArtist,
                        Track::getAlbum,
                        Track::getTitle,
                        Track::getRating)
                .anyMatch(f -> !Objects.equals(f.apply(oldTrack), f.apply(currentTrack)));
        if (isSimpleChange) {
            return ChangeType.SIMPLE;
        }

        // otherwise, nothing we care about changed
        return ChangeType.UNUSED;
    }

    @Override
    public @Nullable Track getCurrentTrack() {
        if (currentTrack == null) {
            currentTrack = MM5TrackFactory.getCurrentTrack(connection);
        }
        return currentTrack;
    }

    private void asyncCommand(String command) {
        try {
            connection.evaluateAsync("app.player.%sAsync()".formatted(command));
        } catch (ScriptException e) {
            log.error("Error in '%s':".formatted(command), e);
        }
    }

    @Override
    public void play() {
        asyncCommand("play");
    }

    @Override
    public void pause() {
        asyncCommand("pause");
    }

    @Override
    public void next() {
        asyncCommand("next");
    }

    @Override
    public void previous() {
        asyncCommand("prev");
    }

    @Override
    public void onShutdown() {
        // TODO unregister callbacks
        connection.close();
    }

    @Override
    public void updateTrackRating(@NotNull Track track, int newRating) {
        try {
            String ratingCommand = """
                    app.getObject('track', { id: %d })
                       .then(function(track) { if (track) {track.rating = %d; track.commitAsync();}
                    });
                    """;
            connection.evaluateAsyncAndWait(ratingCommand.formatted(track.getTrackId(), newRating));
        } catch (ScriptException e) {
            log.error("Error updating rating:", e);
        }
    }

    @Override
    public double getCurrentTrackPosition() {
        try {
            return (Integer) connection.evaluate("app.player.trackPositionMS") / 1000.0;
        } catch (ScriptException e) {
            log.error("Error in 'getCurrentTrackPosition':", e);
            return 0;
        }
    }

    @Override
    public Playlist getCurrentPlaylist() {
        return null;
    }

    @Override
    public @Nullable Track getTrack(int trackId) {
        return MM5TrackFactory.getTrack(connection, trackId);
    }

    @Override
    public @NotNull PlayerState getPlayerState() {
        try {
            String playStateQuery = """
                    var playState = {
                         'isPlaying': app.player.isPlaying,
                         'paused': app.player.paused};
                    playState
                    """;
            Map<String, Object> playState = connection.evaluate(playStateQuery);
            Boolean isPlaying = (Boolean) playState.getOrDefault("isPlaying", Boolean.FALSE);
            Boolean paused = (Boolean) playState.getOrDefault("paused", Boolean.FALSE);
            return isPlaying && !paused
                    ? PlayerState.PLAYING
                    : PlayerState.STOPPED;
        } catch (ScriptException e) {
            log.error("Error in 'getPlayerState':", e);
            return PlayerState.STOPPED;
        }
    }


    private final Map<UUID, Deque<String>> findTracksResults = new ConcurrentHashMap<>();

    public void addFindTracksResult(UUID uuid, String trackId) {
        log.info("addFindTracksResult({}): {}", uuid, trackId);
        findTracksResults.computeIfAbsent(uuid, k -> new ConcurrentLinkedDeque<>()).add(trackId);
    }

    @Override
    public @NotNull List<Track> findTracks(@Nullable String title, @Nullable String artist, @Nullable String album) {
        List<String> trackIds = findTrackIds(title, artist, album);
        return trackIds.stream()
                .mapToInt(Integer::parseInt)
                .mapToObj(this::getTrack)
                .filter(Objects::nonNull)
                .filter(t -> !ErrorTrack.ERROR_TRACK.equals(t))
                .toList();
    }

    @Override
    public @NotNull List<String> findTrackIds(@Nullable String title, @Nullable String artist, @Nullable String album) {
        UUID uuid = UUID.randomUUID();
        try {
            log.info("findTracks({}, {}, {}, {})", uuid, title, artist, album);

            int libraryId = connection.evaluate("app.collections.getEntireLibrary().id");

            connection.evaluateAsync(STR."""
                  var list = app.db.getTracklist('SELECT * FROM Songs WHERE Songs.SongTitle LIKE "%\{title}%"', \{libraryId});
                  list.whenLoaded()
                      .then(function () {
                        list.forEach(function (track) {
                            console.debug("findTracks:\{uuid}:" + track.id);
                        });
                        console.debug("findTracks:\{uuid}:Done");
                      });
            """);
        } catch (ScriptException e) {
            log.error("Error in 'findTracks':", e);
            return List.of();
        }

        // busy wait for results up to one minute
        long start = System.currentTimeMillis();
        int maxWaitTime = 30_000;
        Deque<String> results = new ArrayDeque<>();
        while (System.currentTimeMillis() - start < maxWaitTime) {
            var currentResults = findTracksResults.get(uuid);
            log.info("currentResults: {}", currentResults);
            if (currentResults != null && Objects.equals("Done", currentResults.peekLast()))
            {
                log.info("findTrackResults found 'Done'");
                results = currentResults;
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        findTracksResults.remove(uuid);

        log.info("findTrackResults complete");
        return results.stream()
                .filter(k -> !"Done".equals(k))
                .toList();

    }
}
