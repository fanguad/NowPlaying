/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags;

import furbelow.SpinningDial;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.nekocode.nowplaying.components.swing.NekoButton;
import org.nekocode.nowplaying.components.swing.NekoLabel;
import org.nekocode.nowplaying.events.TagChangeListener;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.cloud.TagCloud;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.*;

/**
 * A Panel that allows the user to create and edit tags on files.
 *
 * @author dan.clark@nekocode.org
 */
@SuppressWarnings("serial")
@Log4j2
public class TagView extends JPanel
{
	public JTextField newTag;
	private final Set<TagChangeListener> listeners;

	private Track track;

	public TagCloud tagHolder;

	private final NekoButton autoTag;
	private final NekoLabel addNew;
	private final JLabel busy;

	private final JComponent textfields;
	private final SpinningDial spinningDial;

	public TagView(TagModel tagModel) {
		listeners = new HashSet<>();

		setLayout(new BorderLayout());
		setOpaque(false);

		newTag = new JTextField();
		newTag.setVisible(false);
		newTag.setHorizontalAlignment(JTextField.CENTER);
		newTag.addFocusListener(new NekoTextFieldFocusListener());

		tagHolder = new TagCloud();

		MouseAdapter tagListener = new MouseAdapter() {
			public void mouseEvent(MouseEvent e) {
				if (e.isPopupTrigger()) {
					final String value = ((JLabel)e.getComponent()).getText();

					JPopupMenu contextMenu = new JPopupMenu();
					JMenuItem removeItem = new JMenuItem();
					removeItem.setText("Remove Tag");
					removeItem.addActionListener(e1 -> removeTag(value));
					contextMenu.add(removeItem);
					JMenuItem editMetadata = new JMenuItem();
					editMetadata.setText("Edit Tag Metadata");
					// TODO set action for this menu item
					contextMenu.add(editMetadata);
					contextMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				mouseEvent(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mouseEvent(e);
			}
		};
		tagHolder.addTagCloudEntryMouseListener(tagListener);

		spinningDial = new SpinningDial(32, 32);
		busy = new JLabel(spinningDial);

		autoTag = new NekoButton("autotag");
		autoTag.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				addAutomaticTags();
			}
		});

		addNew = new NekoLabel("+");
		addNew.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				toggleTagInput();
			}
			});

		add(tagHolder, BorderLayout.CENTER);
		///////////////
		textfields = newTag;
		JPanel holder = new JPanel();
		holder.setOpaque(false);
		holder.add(newTag);
		add(holder, BorderLayout.PAGE_END); // in reverse order, tagHolder disappears
		///////////////

		Dimension size = newTag.getPreferredSize();
		size.width = 200;
		newTag.setPreferredSize(size);
		newTag.setMaximumSize(size);

		newTag.addActionListener(e -> {
            String tag = newTag.getText();
            addTag(tag);
        });

		Action showNewTag = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setTagInputVisible(true);
			}};
		Action hideNewTag = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setTagInputVisible(false);
			}};

		addNew.setToolTipText("Hit + or Spacebar to enter a new tag");
		addNew.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('+'), "showNewTag");
		addNew.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("SPACE"), "showNewTag");
		addNew.getActionMap().put("showNewTag", showNewTag);
		addNew.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "hideNewTag");
		addNew.getActionMap().put("hideNewTag", hideNewTag);

		addTagChangeListener(new TagChangeListener() {
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
	}

	static class NekoTextFieldFocusListener implements FocusListener {
		@Override
		public void focusGained(FocusEvent e) {
			((JTextField)e.getComponent()).setText("");
			e.getComponent().setForeground(Color.black);
		}

        @Override
        public void focusLost(FocusEvent e) {
        }
    }

	public void addTagChangeListener(TagChangeListener l) {
		listeners.add(l);
	}

	protected void fireTagAdded(Track track, String tag) {
		if (track != null && tag != null)
			for (TagChangeListener l : listeners) {
				l.tagAdded(track, tag);
			}
	}

	protected void fireTagRemoved(Track track, String tag) {
		if (track != null && tag != null)
			for (TagChangeListener l : listeners) {
				l.tagRemoved(track, tag);
			}
	}

    /**
     * Informs the tag view that tracks have been updated.  This also ends the busy state of this component.
     *
     * @param track track updated
     * @param tags list of tags
     */
	public void setTags(Track track, List<TagCloudEntry> tags) {
		this.track = track;
        final List<TagCloudEntry> tagsCopy = new ArrayList<>(tags);

		// sort alphabetically
		Collections.sort(tagsCopy);
		log.info("Setting tags: " + tagsCopy);

		SwingUtilities.invokeLater(() -> {
            tagHolder.setTagEntries(tagsCopy);
            tagHolder.add(addNew);
//            tagHolder.add(autoTag); // TODO implement auto-tagging based on MP3 metadata

            pack();
        });
	}


	@Override
	public Dimension getPreferredSize() {
		// TODO find some better way that isn't so component specific
		// the problem here is that tagHolder's preferred size isn't being taken into account
		// even with this "fix", the preferred size seems to lag 1 resize behind what it should be
		Dimension size = super.getPreferredSize();
		int previousHeight = size.height;

		size.height = tagHolder.getPreferredSize().height;
		size.height += isTagInputVisible() ? newTag.getPreferredSize().height : 0;

		// if size isn't within about 20%, redo the layout
		if (previousHeight * .8 > size.height || previousHeight * 1.2 < size.height) {
			invalidate();
		}

		return size;
	}

	/**
	 * Force a resize
	 *
	 */
	private void pack() {
        tagHolder.validate();
        this.validate();

		// find the top level frame
		Container parent = this.getParent();
		Window frame = null;
		while (parent != null) {
			parent = parent.getParent();
			if (parent instanceof Window) {
				frame = (Window) parent;
				break;
			}
		}
		// resize the frame
		if (frame != null) { // this should never be false
			frame.validate();
//			log.debug(String.format("tag cloud current size:   %dx%d", tagHolder.getSize().width, 			tagHolder.getSize().height));
//			log.debug(String.format("tag cloud min size:       %dx%d", tagHolder.getMinimumSize().width, 	tagHolder.getMinimumSize().height));
//			log.debug(String.format("tag cloud preferred size: %dx%d", tagHolder.getPreferredSize().width, 	tagHolder.getPreferredSize().height));
//			log.debug(String.format("tag cloud max size:       %dx%d", tagHolder.getMaximumSize().width, 	tagHolder.getMaximumSize().height));
		}
	}

	public void toggleTagInput() {
		boolean visibleState = !isTagInputVisible();
		setTagInputVisible(visibleState);
	}

	public void setTagInputVisible(boolean visible) {
		textfields.setVisible(visible);
		pack();
		if (visible) {
			newTag.requestFocus();
		} else {
			newTag.setText("");
			this.requestFocus();
		}
	}

	/**
	 * Returns whether the tag input text field is currently visible.
	 *
	 * @return true if visible
	 */
	public boolean isTagInputVisible() {
		return textfields.isVisible();
	}

	private void addAutomaticTags() {
		// get list of automatic tags
//		setBusy();

		log.info("automatic tag: {}", track.getArtist());  // split on '; '
		log.info("automatic tag: {}", track.getGenre());  // doesn't work?
//		log.info("automatic tag: {}", track.getMood());
//		log.info("automatic tag: {}", track.getAnime());
//		log.info("automatic tag: {}", track.getGame());
		// TODO if "suggested" tags ever gets implemented, include them here
	}

	private void addTag(String tag) {
		setBusy();

        setTagInputVisible(false);
		tag = tag.trim();
	    fireTagAdded(track, tag);

	    newTag.setText("");
    }

	private void removeTag(String tag) {
		setBusy();
        fireTagRemoved(track, tag);
    }

    /**
	 * Removes the "add new tag" functionality temporarily and puts a busy
	 * icon in its place.
	 */
	private void setBusy() {
		tagHolder.removeAll();
		tagHolder.add(busy);
	}

	/**
	 * Puts the tag view in an intermediate state where the user is informed
	 * that tags for the current track have not yet been loaded.
	 */
	public void setLoadingTags() {
		try {
			SwingUtilities.invokeAndWait(this::setBusy);
		} catch (InterruptedException | InvocationTargetException e) {
			log.error(e);
		}
	}

	public void shutdown() {
		log.info("shutting down TagView");
		spinningDial.setFrameInterval(0);
		log.info("finished shutting down TagView");
	}
}
