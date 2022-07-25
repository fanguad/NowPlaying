package org.nekocode.nowplaying.remote.mediamonkey5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nekocode.nowplaying.AbstractMediaPlayer;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.objects.Playlist;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.ImageIcon;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.ART_CHANGE;
import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.CURRENT_SONG_CHANGE;
import static org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType.PLAY_STATE_CHANGE;

public class MM5RemoteModel extends AbstractMediaPlayer {

    private static final Logger LOG = LogManager.getLogger(MM5RemoteModel.class);
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
                            MM5RemoteModel.this.currentTrack = MM5Track.getCurrentTrack(connection);
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
                    LOG.debug("thumbnail update for track {}: {}", e.getOldValue(), e.getNewValue());
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
                            LOG.info("thumbnail for track {} available at: {}", trackId, fullPath);
                            mmTrack.addArtwork(new ImageIcon(fullPath.toString()));
                            fireTrackChanged(new TrackChangeEvent(currentTrack, ART_CHANGE));
                        }
                    }
                }
            }
        });
    }

    @Override
    public @Nullable Track getCurrentTrack() {
        if (currentTrack == null)
            currentTrack = MM5Track.getCurrentTrack(connection);
        return currentTrack;
    }

    private void asyncCommand(String command)
    {
        try {
            connection.evaluateAsync("app.player.%sAsync()".formatted(command));
        } catch (ScriptException e) {
            LOG.error("Error in '%s':".formatted(command), e);
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
            LOG.error("Error updating rating:", e);
        }
    }

    @Override
    public double getCurrentTrackPosition() {
        try {
            return (Integer) connection.evaluate("app.player.trackPositionMS") / 1000.0;
        } catch (ScriptException e) {
            LOG.error("Error in 'getCurrentTrackPosition':", e);
            return 0;
        }
    }

    @Override
    public Playlist getCurrentPlaylist() {
        return null;
    }

    @Override
    public @Nullable Track getTrack(int trackId) {
        return MM5Track.getTrack(connection, trackId);
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
            LOG.error("Error in 'getPlayerState':", e);
            return PlayerState.STOPPED;
        }
    }

    @Override
    public @NotNull List<Track> findTracks(@Nullable String title, @Nullable String artist, @Nullable String album) {
        return null;
    }

    @Override
    public @NotNull List<String> findTrackIds(@Nullable String title, @Nullable String artist, @Nullable String album) {
        return null;
    }
}
