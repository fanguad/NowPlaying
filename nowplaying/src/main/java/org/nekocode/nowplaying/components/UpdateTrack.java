/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components;

import lombok.extern.log4j.Log4j2;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.*;

/**
 * Interface that will be called by the view to update the view
 */
@Log4j2
public abstract class UpdateTrack extends SwingWorker<ImageIcon, Object> {
	protected Track newTrack;

	/**
	 * Shortcut for setTrack() followed by execute();  This is the method that
	 * should be called when the track changes.
	 *
	 * @param newTrack new Track
	 */
	public void execute(Track newTrack) {
        try {
            setTrack(newTrack);
            execute();
        } catch (Exception e) {
            log.error("Problem on track update", e);
        }
	}

	/**
	 * Sets the track that this Updater should work on.
	 *
	 * @param newTrack new Track
	 */
	public void setTrack(Track newTrack) {
		this.newTrack = newTrack;
	}

	public abstract void setComponent(ArtPanel artPanel, int height, int width);
	public abstract void setFrame(JFrame view);
	public abstract void updateArtDisplayArea();
}