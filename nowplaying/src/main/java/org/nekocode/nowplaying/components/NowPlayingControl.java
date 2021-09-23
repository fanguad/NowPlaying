/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   NowPlayingControl.java
 * Created On: Dec 20, 2007
 */
package org.nekocode.nowplaying.components;

import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.events.TrackChangeEvent;

import javax.swing.JComponent;

/**
 * Interface for a control section of NowPlayingView that receives track update
 * notifications.
 *
 * @author dan.clark@nekocode.org
 */
public abstract class NowPlayingControl extends JComponent {

	/**
	 * This method will be called whenever the currently playing track changes
	 * (either to a different track, or the data of the same track changes).
	 *
	 * @param trackChange track change event containing new information
	 */
	public abstract void updateTrack(@NotNull TrackChangeEvent trackChange);

	public abstract void shutdown();

    public abstract String getModeName();
}
