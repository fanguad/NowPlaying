/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   PlaylistPanel.java
 * Created On: Sep 23, 2008
 */
package org.nekocode.nowplaying.components.modes.playlist;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.components.swing.NekoLabel;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.objects.Playlist;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.TagModel;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 * @author dan.clark@nekocode.org
 */
public class PlaylistPanel extends NowPlayingControl {
	private static final Logger log = Logger.getLogger(PlaylistPanel.class);
    private MediaPlayer mediaPlayer;
    private NekoLabel next1Track;
    private NekoLabel next2Track;
    private NekoLabel next3Track;

    public PlaylistPanel(MediaPlayer mediaPlayer, TagModel tagModel) {
		setLayout(new GridLayout(0, 1));
		setOpaque(true);

        this.mediaPlayer = mediaPlayer;
        
        this.next1Track = new NekoLabel("Next Track", NekoLabel.CENTER);
        this.next2Track = new NekoLabel("Next Next Track", NekoLabel.CENTER);
        this.next3Track = new NekoLabel("Next Next Next Track", NekoLabel.CENTER);

        add(next1Track);
        add(next2Track);
        add(next3Track);

        updateNextTracks();

//		NekoLabel playlist = new NekoLabel("playlist");
//		add(playlist, BorderLayout.CENTER);
//
//		playlist.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//            	log.debug("Displaying playlist");
//
//            	// TODO testing only, use all files in this directory
//            	List<Track> playlist = new ArrayList<Track>();
//            	File dir = new File("D:\\Music\\Japanese\\m-flo\\ASTROMANTIC\\");
//            	for (File file : dir.listFiles()) {
//            		if (file.getName().toLowerCase().endsWith("mp3"))
//            			playlist.add(new MPlayerTrack(file.getAbsolutePath()));
//            	}
//
//            	JXFrame frame = new JXFrame("Playlist");
//            	TrackTableView playlistView = new TrackTableView();
//            	playlistView.setTracks(playlist);
//				frame.add(playlistView);
//				frame.setSize(500, 500);
//				frame.setLocationByPlatform(true);
//				frame.setVisible(true);
//            }
//		});
	}

    public void updateNextTracks() {
        Playlist playlist = mediaPlayer.getCurrentPlaylist();
        if (playlist != null) {
            // TODO check for null
            Track currentTrack = mediaPlayer.getCurrentTrack();
            List<Track> nextTracks = new ArrayList<Track>();

            if (playlist.count() <= 100) {
                Logger.getLogger(PlaylistPanel.class).warn("Skipping updateNextTracks because >100 tracks in current playlist");
                List<Track> tracks = playlist.getTracks();
                boolean foundCurrentTrack = false;
                for (int i = 0; i < tracks.size() && nextTracks.size() < 3; i++) {
                    Track track = tracks.get(i);
                    if (!foundCurrentTrack) {
                        foundCurrentTrack = currentTrack.equals(track);
                    } else {
                        nextTracks.add(track);
                    }
                }
            }

            final String empty = "";

            if (nextTracks.size() > 0) {
                next1Track.setText(nextTracks.get(0).getTitle());
            } else {
                next1Track.setText(empty);
            }

            if (nextTracks.size() > 1) {
                next2Track.setText(nextTracks.get(1).getTitle());
            } else {
                next2Track.setText(empty);
            }

            if (nextTracks.size() > 2) {
                next3Track.setText(nextTracks.get(2).getTitle());
            } else {
                next3Track.setText(empty);
            }
        } else {
            next1Track.setText("playlist was empty");
        }
    }

	/* (non-Javadoc)
	 * @see org.nekocode.nowplaying.components.NowPlayingControl#shutdown()
	 */
	@Override
	public void shutdown() {

	}

	/* (non-Javadoc)
	 * @see org.nekocode.nowplaying.components.NowPlayingControl#updateTrack(org.nekocode.nowplaying.events.TrackChangeEvent)
	 */
	@Override
	public void updateTrack(TrackChangeEvent trackChange) {
        updateNextTracks();
	}
}