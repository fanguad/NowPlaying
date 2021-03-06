/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * To be run when the track has been updating.  Resizes the window to maintain prettiness.
 */
public class ResizeUpdateTrack extends UpdateTrack {
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(ResizeUpdateTrack.class);

	private JFrame view;
	private int size;
	private ArtPanel artworkPanel;

	@Override
	public void setComponent(ArtPanel panel, int height, int width)
	{
		this.artworkPanel = panel;
		this.size = Math.min(height, width);
	}

	@Override
	public void setFrame(JFrame view)
	{
		this.view = view;
	}

	@Override
	public void updateArtDisplayArea()
	{
		artworkPanel.repaint();
		// nothing special needs to be done
	}

	@Override
	protected ImageIcon doInBackground() throws Exception {
		Deque<ImageIcon> artList = new ArrayDeque<>(newTrack.getArtwork(size));
		return artList.poll();
	}

	@Override
	protected void done() {
		ImageIcon artwork = null;
		try {
			artwork = get();
		} catch (InterruptedException | ExecutionException e) {
			log.error(e);
		}
		if (artwork == null) {
			artworkPanel.clearArt();
		} else {
			artworkPanel.setArt(artwork);
		}

        log.debug("Resizing ArtPanel: old size = " + view.getSize() );
		view.pack();
        log.debug("Resizing ArtPanel: new size = " + view.getSize() );

		updateArtDisplayArea();
	}
}