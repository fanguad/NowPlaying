/*
 * Copyright (c) 2010, fanguad@nekocode.org
 */

/*
 * Filename:   NowPlayingControl.java
 * Created On: Dec 20, 2007
 */
package org.nekocode.nowplaying.components;

import javax.swing.JComponent;

import org.nekocode.nowplaying.events.TrackChangeEvent;

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
	public abstract void updateTrack(TrackChangeEvent trackChange);

	public abstract void shutdown();
}
