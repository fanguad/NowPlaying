/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingWorker;

import org.nekocode.nowplaying.objects.Track;

/**
 * Interface that will be called by the view to update the view
 */
public abstract class UpdateTrack extends SwingWorker<ImageIcon, Object> {

	protected Track newTrack;

	/**
	 * Shortcut for setTrack() followed by execute();  This is the method that
	 * should be called when the track changes.
	 *
	 * @param newTrack new Track
	 */
	public void execute(Track newTrack) {
		setTrack(newTrack);
		execute();
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