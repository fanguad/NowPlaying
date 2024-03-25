/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.components;

import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutionException;

/**
 * To be run when the track has been updating.  Resizes the window to maintain prettiness.
 */
@Log4j2
public class ResizeUpdateTrack extends UpdateTrack {
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