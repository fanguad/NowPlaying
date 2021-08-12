/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   TrackMonitor.java
 * Created On: Apr 16, 2008
 */
package org.nekocode.nowplaying.internals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.MediaPlayer.PlayerState;
import org.nekocode.nowplaying.NowPlayingView;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeListener;
import org.nekocode.nowplaying.objects.Track;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Follows the progress of the track, updating certain components (like progress
 * bars, for example) on a regular basis.
 *
 * @author dan.clark@nekocode.org
 */
public class TrackMonitor implements Runnable, TrackChangeListener {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(TrackMonitor.class);

	private NowPlayingView view;
	private MediaPlayer player;

	private Timer timer;

    /**
     * rate that source application updates track time, in ms
     */
	private static final long MEDIA_PLAYER_POLL_RATE = 15000;
    /**
     * Update rate, in milliseconds.
     */
	private static final int UPDATE_RATE = 50;

	//////////////////////////////////////////////////////////////////////
	// experimental code for updates faster than 1/second
	//////////////////////////////////////////////////////////////////////
	private double lastMeasuredPercent;
	private long lastMeasuredTime;
    /**
     * percent of song covered in MEDIA_PLAYER_POLL_RATE time
     */
	private double velocity;
    private static final long NANO_TO_MILLI = 1000000;
    private boolean shutdown;
    private boolean forceRefresh;

    /**
	 * Creates a new TrackMonitor that will periodically send updates to the view.
	 *
	 * @param myView
	 * @param myPlayer
	 */
	public TrackMonitor(NowPlayingView myView, MediaPlayer myPlayer) {
		this.view = myView;
		this.player = myPlayer;
        shutdown = false;
        forceRefresh = false;
	}

	/**
	 * Causes the TrackMonitor to begin monitoring.
	 */
	public void start() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				TrackMonitor.this.run();
			}
		};

		timer = new Timer("TrackMonitor", true);
		timer.schedule(task, 1000, UPDATE_RATE);
	}

	@Override
	public void run() {
        if (shutdown) {
            return;
        }

		long currentTime = System.nanoTime();

        // it's not yet time to poll the media player
        long timeSinceLastUpdate = currentTime - lastMeasuredTime;
        long timeUntilNextUpdate = MEDIA_PLAYER_POLL_RATE * NANO_TO_MILLI - timeSinceLastUpdate;

        if (forceRefresh) {
            forceRefresh = false;
            fullRefresh(currentTime);
        } else if (timeUntilNextUpdate > 0) {
            interpolatePercentComplete(currentTime);
            log.debug("interpolating progress: next update in " + timeUntilNextUpdate + "ns");
        } else {
            fullRefresh(currentTime);
        }
	}

    /**
     * Request an update on the position to calculate true percent complete.
     *
     * @param currentTime current system time (nanos)
     */
    private void fullRefresh(long currentTime) {
        // always record that we measured the time when this method is called
        lastMeasuredTime = System.nanoTime();

        Track currentTrack = player.getCurrentTrack();

        if (currentTrack == null) {
            view.updateTrackProgress(0);
        } else {
            // time might have changed, so poll
            double duration = currentTrack.getDuration();
            double position = player.getCurrentTrackPosition();
            double measuredPercent = position / duration;
            // velocity per period
            if (player.getPlayerState() == PlayerState.PLAYING) {
                velocity = 1.0 / duration / 1000 * MEDIA_PLAYER_POLL_RATE;

                if (lastMeasuredPercent == measuredPercent) {
                    // if this is the same as the last measurement, interpolate
                    interpolatePercentComplete(currentTime);
                    log.debug("querying media player for progress - interpolating results");
                } else {
                    // if the current measure value isn't the same as the last
                    //    measured value, use the actual value
                    lastMeasuredPercent = measuredPercent;
                    view.updateTrackProgress(measuredPercent);
                    log.debug("querying media player for progress - found new time");
                }
            } else {
                velocity = 0;
                // update the track progress to make sure it is correct
                lastMeasuredPercent = measuredPercent;
                view.updateTrackProgress(measuredPercent);
                log.debug("querying media player for progress - player is stopped");
            }
        }
    }

    private void interpolatePercentComplete(long currentTime) {
        if (velocity > 0) {
            long timeElapsed = (currentTime - lastMeasuredTime) / NANO_TO_MILLI;
            // timeElapsed * velocity performed first so integer division is avoided
            double percentComplete = lastMeasuredPercent + (timeElapsed * velocity) / MEDIA_PLAYER_POLL_RATE;
            view.updateTrackProgress(percentComplete);
            // log.debug(String.format("lastValue: %s, velocity: %s", lastValue, velocity));
        }
    }

    /**
	 *
	 */
	public void shutdown() {
        shutdown = true;
        if (timer != null) {
            timer.cancel();
        }
	}

    @Override
    public void trackChanged(TrackChangeEvent e) {
        switch (e.getType()) {
            case PLAY_STATE_CHANGE:
            case CURRENT_SONG_CHANGE:
            case FILE_CHANGE:
            case ART_CHANGE:
                // force a full update
                forceRefresh = true;
                break;
        }
    }
}
