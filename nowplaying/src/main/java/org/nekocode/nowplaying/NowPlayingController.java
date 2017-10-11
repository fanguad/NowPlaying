/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.components.MouseActions;
import org.nekocode.nowplaying.components.RatingChangeEvent;
import org.nekocode.nowplaying.components.modes.control.ControlPanel;
import org.nekocode.nowplaying.components.modes.tag.TagPanel;
import org.nekocode.nowplaying.components.modes.tagsdnd.TagDnDPanel;
import org.nekocode.nowplaying.events.TagChangeListener;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType;
import org.nekocode.nowplaying.internals.TrackMonitor;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.resources.images.Icons;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.TagView;

import javax.swing.ImageIcon;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * MVC controller.  Entry point of application.
 *
 * @author dan.clark@nekocode.org
 */
public class NowPlayingController
{
	@SuppressWarnings("unused")
	private static final Logger log = LogManager.getLogger(NowPlayingController.class);
	private MediaPlayer mediaPlayer;
	private NowPlayingView view;
	private TagView tagView;
	private TagModel tagModel;
	private TrayIcon trayIcon;
	private TrackMonitor monitor;
    private boolean shutdown;

    public static void main(String... args) {
		LogMuter.muteLogging();
		NowPlayingController controller = new NowPlayingController();
		controller.start();
	}

	public NowPlayingController() {
        shutdown = false;
        String mediaPlayerClassName = NowPlayingProperties.loadProperties().getProperty(NowPlayingProperties.MEDIA_PLAYER.name());
        try {
//            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // the font gets changed, and the tag editor look *terrible* if I change the UI...

            if (mediaPlayerClassName == null) {
                throw new RuntimeException("MediaPlayer was not supplied.  Cannot proceed");
            }
            Class<?> mediaPlayerClass = Class.forName(mediaPlayerClassName);
            mediaPlayer = (MediaPlayer) mediaPlayerClass.newInstance();

            tagModel = new TagModel();
            view = new NowPlayingView();
            tagView = new TagView();

            monitor = new TrackMonitor(view, mediaPlayer);

            connectComponents();
        } catch (Exception e) {
            log.fatal("Fatal error", e);
            // try to shut down smoothly
            shutdown();
        }
    }

	private void connectComponents() {
	    // hook everything up
		mediaPlayer.addTrackChangeListener(view::updateTrack);
        mediaPlayer.addTrackChangeListener(monitor);
		tagView.addTagChangeListener(new TagChangeListener() {
			public void tagAdded(@NotNull Track track, @NotNull String tag) {
				String metadata = null;
				int separator = tag.indexOf(": ");
				if (separator > 0) {
					// TODO hack to get metadata in there
					metadata = tag.substring(0, separator);
					tag = tag.substring(separator+2);
					log.debug(String.format("HACK for splitting metadata from tag: %s/%s", tag, metadata));
				}

				tagModel.addTag(track, tag, metadata);
			}

            @Override
            public void tagsChanged(@NotNull Track track) {
                // no way to trigger this change
            }

            public void tagRemoved(@NotNull Track track, @NotNull String tag) {
				tagModel.removeTag(track, tag);
			}});

		tagModel.addTagChangeListener(new TagChangeListener() {
			@Override
			public void tagAdded(@NotNull Track track, @NotNull String tag) {
                tagsChanged(track);
			}

            @Override
            public void tagsChanged(@NotNull Track track) {
                if (track.getTrackId() == view.getCurrentTrack().getTrackId())	 {
                    TrackChangeEvent e = new TrackChangeEvent(track, ChangeType.TAG_CHANGE);
                    view.updateTrack(e);
                }
            }

            @Override
			public void tagRemoved(@NotNull Track track, @NotNull String tag) {
                tagsChanged(track);
			}});

		ControlPanel controls = new ControlPanel();
		controls.addRatingChangeListener(e -> {
            RatingChangeEvent rce = (RatingChangeEvent) e;
            mediaPlayer.updateTrackRating(mediaPlayer.getCurrentTrack(), rce.getNewRating());
        });
		controls.addControlListener(e -> {
            Controls controlType = (Controls)e.getSource();
            switch (controlType) {
            case PLAY:
                mediaPlayer.play();
                break;
            case PAUSE:
                mediaPlayer.pause();
                break;
            case NEXT:
                mediaPlayer.next();
                break;
            case PREVIOUS:
                mediaPlayer.previous();
                break;
            }
        });

		TagPanel tags = new TagPanel(tagModel, tagView);
//		TagOperationsPanel tagOps = new TagOperationsPanel(mediaPlayer, tagModel);
//		PlaylistPanel playlist = new PlaylistPanel(mediaPlayer, tagModel);
        TagDnDPanel tagsdnd = new TagDnDPanel(mediaPlayer, tagModel);

		view.addMode("controls", controls);
		view.addMode("tags", tags);
		view.addMode("tags ops", tagsdnd);
//		view.addMode("tag ops", tagOps);
//		view.addMode("playlist", playlist);

		view.setMode("controls");
    }

	public void start() {
        if (shutdown) return;

		view.setUndecorated(true);

//		JPanel spacer = new JPanel();
//		spacer.setOpaque(false);
//		spacer.setMinimumSize(tagCloudButton.getMinimumSize());
//		spacer.setPreferredSize(tagCloudButton.getPreferredSize());
//		spacer.setMaximumSize(tagCloudButton.getMaximumSize());
//		view.addInternal(spacer, BorderPositions.LINE_END);

		// give the view a reference to the frame holding it
		Track currentTrack = mediaPlayer.getCurrentTrack();
		TrackChangeEvent trackChange =
			new TrackChangeEvent(currentTrack, ChangeType.CURRENT_SONG_CHANGE);
		view.updateTrack(trackChange);

		// set up the mouse actions
		MouseActions ma = new MouseActions(view);
		view.addMouseListener(ma);
		view.addMouseMotionListener(ma);

		view.addWindowListener(new OnExit());
		view.setAlwaysOnTop(true);

//        System.setProperty("sun.java2d.noddraw", "true");
        // install per-pixel transparency
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        if (gd.isWindowTranslucencySupported(WindowTranslucency.PERPIXEL_TRANSPARENT)) {
//            view.setBackground(new Color(0, 0, 0, 0));
        } else {
            LogManager.getLogger(getClass()).warn("Per pixel transparency not supported.");
        }
        trayIcon = null;
		if (SystemTray.isSupported()) {
            // get the SystemTray instance
            SystemTray tray = SystemTray.getSystemTray();
            // load an image
            ImageIcon imageIcon = new ImageIcon(Icons.class.getResource("play-tray.png"));
            // construct a TrayIcon
            trayIcon = new TrayIcon(imageIcon.getImage(), "Now Playing...", ma.getPopupMenu());
            trayIcon.setImageAutoSize(true);
            // add the tray image
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                log.error("Error registering system tray", e);
            }
        }

		view.pack();
		view.setInitialLocation();
		view.setVisible(true);
		monitor.start();
	}

	private class OnExit extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e)
		{
			e.getWindow().dispose();
		}

		@Override
		public void windowClosed(WindowEvent e)
		{
			shutdown();
		}
	}

	private void shutdown() {
        shutdown = true;

		if (monitor != null)
			try {
				monitor.shutdown();
			} catch (Exception e) {
				log.error("Exception during shutdown", e);
			}

		if (tagModel != null)
			try {
				tagModel.shutdown();
			} catch (Exception e) {
				log.error("Exception during shutdown", e);
			}

		if (mediaPlayer != null)
			try {
				mediaPlayer.onShutdown();
			} catch (Exception e) {
				log.error("Exception during shutdown", e);
			}

		if (view != null) {
			view.shutdown();
		}

		if (tagView != null) {
			tagView.shutdown();
		}

		if (trayIcon != null)
			SystemTray.getSystemTray().remove(trayIcon);

        NowPlayingProperties.storeProperties();

        // as a safe-guard against hangs, force a quit after 60 more seconds
		TimerTask forceExit = new TimerTask() {
			@Override
			public void run() {
				log.error("Application did not shutdown cleanly.  Forcing exit.");
				System.exit(0);
			}};
		Timer t = new Timer("Force Quit Timer", true);
		t.schedule(forceExit, 60 * 1000);
	}
}
