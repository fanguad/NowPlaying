/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.control;

import org.nekocode.nowplaying.Controls;
import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.components.RatingChangeEvent;
import org.nekocode.nowplaying.components.RatingDisplay;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType;
import org.nekocode.nowplaying.resources.images.Icons;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * A Panel containing playback and rating controls.
 *
 * @author dan.clark@nekocode.org
 */
public class ControlPanel extends NowPlayingControl {

	private final static EnumSet<ChangeType> shouldUpdate = EnumSet.of(
			ChangeType.CURRENT_SONG_CHANGE, ChangeType.FILE_CHANGE);

	private Set<ChangeListener> ratingChangeListeners;
	private Set<ChangeListener> controlListeners;

	private RatingDisplay rating;

	/**
	 * Creates basic play controls. The goal is to have them centered
	 * horizontally, and on the bottom vertically.
	 */
	public ControlPanel() {
		setLayout(new BorderLayout());

		ratingChangeListeners = new HashSet<>();
		controlListeners = new HashSet<>();

		JPanel controls = new JPanel();
		JComponent next = createImageLabel("next.png");
		JComponent previous = createImageLabel("previous.png");
		JComponent play = createImageLabel("play.png");
		JComponent pause = createImageLabel("pause.png");
		previous.addMouseListener(createControlListener(Controls.PREVIOUS));
		pause.addMouseListener(createControlListener(Controls.PAUSE));
		play.addMouseListener(createControlListener(Controls.PLAY));
		next.addMouseListener(createControlListener(Controls.NEXT));
		controls.add(previous);
		controls.add(pause);
		controls.add(play);
		controls.add(next);
		controls.setOpaque(false);

		rating = new RatingDisplay();
		setOpaque(true);

		// so that both the rating and the controls will be SOUTH aligned
		JPanel subLayout = new JPanel(new BorderLayout());
		subLayout.setOpaque(false);
		subLayout.add(controls, BorderLayout.SOUTH);

		add(subLayout, BorderLayout.CENTER);
		add(rating, BorderLayout.SOUTH);


		rating.addChangeListener(e -> fireRatingChanged());

		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('z'), Controls.PREVIOUS);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('c'), Controls.PAUSE);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('v'), Controls.PLAY);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('b'), Controls.NEXT);

		for (Controls control : Controls.values()) {
			getActionMap().put(control, createControlAction(control));
		}

	}

	private Action createControlAction(final Controls controlType) {
        return new AbstractAction() {
            /* (non-Javadoc)
             * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                fireControlEvent(controlType);
            }};
	}

	private MouseListener createControlListener(final Controls controlType) {
		return new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e)
			{
				fireControlEvent(controlType);
			}
		};
	}

	private JLabel createImageLabel(String string)
	{
		URL imageURL = Icons.class.getResource(string);
		ImageIcon icon = new ImageIcon(imageURL);
        return new JLabel(icon);
	}

	protected void fireControlEvent(Controls controlType) {
		ChangeEvent e = new ChangeEvent(controlType);

		for (ChangeListener l : controlListeners) {
			l.stateChanged(e);
		}
	}

	/**
	 * Listener that will be called if the rating changes.  RatingChangeEvents
	 * will created.
	 *
	 * @param l
	 */
	public void addRatingChangeListener(ChangeListener l) {
		ratingChangeListeners.add(l);
	}


	/**
	 * Informs all the listeners that the rating has changed.
	 */
	protected void fireRatingChanged() {
		RatingChangeEvent e = new RatingChangeEvent(this, rating.getValue());

		for (ChangeListener l : ratingChangeListeners) {
			l.stateChanged(e);
		}
	}


	/**
	 * Listener that will be called if the rating changes.  The source will be
	 * an enum from Controls.
	 *
	 * @param l
	 */
	public void addControlListener(ChangeListener l) {
		controlListeners.add(l);
	}

	/**
	 * This method will be called whenever the currently playing track changes
	 * (either to a different track, or the data of the same track changes).
	 *
	 * @param trackChange information about the new track
	 */
	@Override
	public void updateTrack(final TrackChangeEvent trackChange) {
		if (shouldUpdate.contains(trackChange.getType())) {
			SwingUtilities.invokeLater(() -> rating.setValue(trackChange.getTrack().getRating()));
		}
	}

	@Override
	public void shutdown() {
		// nothing to do
	}
}
