/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   ListView.java
 * Created On: Jun 19, 2008
 */
package org.nekocode.nowplaying.listview;

import javax.swing.JFrame;

/**
 * The root of NowPlaying's list view.  This is a utility view, akin to iTunes's
 * main window.  It will contain subsections to view playlists, search tags
 * and display track information in table form.
 *
 * @author dan.clark@nekocode.org
 */
public class ListView extends JFrame {
	private TrackTableView tracks;

	public static void main(String... args) {
		ListView view = new ListView();

		view.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		view.setSize(200, 200);
		view.setLocationByPlatform(true);
		view.setVisible(true);
	}
}
