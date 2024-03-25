/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.listview;

import javax.swing.*;

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
