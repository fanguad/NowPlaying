package org.nekocode.nowplaying.remote.mediamonkey5;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nekocode.nowplaying.AbstractMediaPlayer;
import org.nekocode.nowplaying.objects.Playlist;
import org.nekocode.nowplaying.objects.Track;

import java.util.List;
import java.util.Map;

public class MM5RemoteModel extends AbstractMediaPlayer {

    private static final Logger LOG = LogManager.getLogger(MM5RemoteModel.class);
    private final MM5Connection connection;

    public MM5RemoteModel() {
        connection = new MM5Connection();
    }

    @Override
    public @Nullable Track getCurrentTrack() {
        try {
            Map<String, Object> propertyMap = connection.evaluate("player.getCurrentTrack()");
            return MM5Track.getInstance(propertyMap);
        } catch (ScriptException e) {
            return ErrorTrack.ERROR_TRACK;
        }
    }

    @Override
    public void play() {
        try {
            connection.evaluateAsyncAndWait("player.playAsync()");
        } catch (ScriptException e) {
            LOG.error("Error in 'play':", e);
        }
    }

    @Override
    public void pause() {
        try {
            connection.evaluateAsyncAndWait("player.playAsync()");
        } catch (ScriptException e) {
            LOG.error("Error in 'pause':", e);
        }
    }

    @Override
    public void next() {

    }

    @Override
    public void previous() {

    }

    @Override
    public void onShutdown() {
        connection.close();
    }

    @Override
    public void updateTrackRating(@NotNull Track track, int newRating) {

    }

    @Override
    public double getCurrentTrackPosition() {
        return 0;
    }

    @Override
    public Playlist getCurrentPlaylist() {
        return null;
    }

    @Override
    public @Nullable Track getTrack(int trackId) {
        return null;
    }

    @Override
    public @NotNull PlayerState getPlayerState() {
        return null;
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
