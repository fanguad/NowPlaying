/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nekocode.nowplaying.objects.Track;

/**
 * Interface that will be called by the view to update the view
 */
public abstract class UpdateTrack extends SwingWorker<ImageIcon, Object> {

    private static final Logger log = LogManager.getLogger(UpdateTrack.class);

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