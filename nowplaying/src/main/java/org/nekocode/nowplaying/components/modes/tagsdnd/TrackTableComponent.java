/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.apache.log4j.Logger;
import org.divxdede.swing.busy.BusyModel;
import org.divxdede.swing.busy.JBusyComponent;
import org.divxdede.swing.busy.ui.BasicBusyLayerUI;
import org.divxdede.swing.busy.ui.BusyLayerUI;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.nekocode.nowplaying.MediaPlayer;
import org.nekocode.nowplaying.components.icons.SpinningDialBusyIcon;
import org.nekocode.nowplaying.internals.DaemonThreadFactory;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.objects.UnknownTrack;
import org.nekocode.nowplaying.tags.TagModel;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A table of tracks.  This component already has quite a bit of customized behavior, reducing
 * coding of components that want to inherit that behavior.
 * <ul>
 * <li>Supports drag and drop of tiles onto the component.</li>
 * <li>Able to lock the component in a busy state using @{link #setBusy(boolean)}</li>
 * <li>Automatically resizes the columns to match the input length.</li>
 * <li>Functions to access the tracks contained in the table.</li>
 * <li>Table is contained in a JScrollPane</li>
 * </ul>
 */
public class TrackTableComponent extends JPanel {
    private static final Logger log = Logger.getLogger(TrackTableComponent.class);

    private Executor workerThread = Executors.newFixedThreadPool(1, new DaemonThreadFactory());
    private MediaPlayer mediaPlayer;
    private TrackTableModel dataModel;
    private BusyModel busyModel;

    private JScrollPane scrollpane;
    private Border defaultBorder;
    private Border highlightBorder;
    private TagModel tagModel;
    private TrackTable trackTable;
    private Collection<ChangeListener> changeListeners;

    /**
     * Constructor.
     *
     * @param mediaPlayer nowplaying... media play
     * @param tagModel tag model
     */
    public TrackTableComponent(MediaPlayer mediaPlayer, TagModel tagModel) {
        super(new BorderLayout());

        this.mediaPlayer = mediaPlayer;
        this.tagModel = tagModel;
        changeListeners = new HashSet<ChangeListener>();
        dataModel = new TrackTableModel(tagModel);
        tagModel.addTagChangeListener(dataModel);
        trackTable = new TrackTable(dataModel);

        scrollpane = new JScrollPane(trackTable);
        scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        defaultBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        highlightBorder = BorderFactory.createLineBorder(Color.blue, 2, false);
        setBorder(false);

        BusyLayerUI busyLayerUI = new BasicBusyLayerUI(0, 0.85f, Color.WHITE);
        JBusyComponent<JComponent> busyComponent = new JBusyComponent<JComponent>(scrollpane, busyLayerUI);

        // use the BusyModel to control the busy state of this component
        busyModel = busyComponent.getBusyModel();
        busyModel.setCancellable(false);
        busyModel.setDeterminate(false);
        BasicBusyLayerUI ui = new BasicBusyLayerUI();
        ui.setBusyIcon(new SpinningDialBusyIcon(64, 64));
        busyComponent.setBusyLayerUI(ui);

        this.add(busyComponent, BorderLayout.CENTER);
        @SuppressWarnings("unused") // the drop target needs to be created, but doesn't need to be attached to anything
        DropTarget dt = new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, new FileDropListener());

        // create an action that will delete the selected rows if the delete key is pressed
        Action deleteRow = new AbstractAction() {
              /* (non-Javadoc)
               * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
               */
              @Override
              public void actionPerformed(ActionEvent e) {
                  trackTable.deleteSelectedRows();
              }};
        trackTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("DELETE"), "deleteRow");
        trackTable.getActionMap().put("deleteRow", deleteRow);

    }

    /**
     * Reset the table - delete all rows and resize columns back to their default widths.
     */
    public void clear() {
        dataModel.clear();
        trackTable.shrinkColumns();
        fireChangeEvent(new TrackTableChangeEvent(TrackTableChangeEvent.ChangeType.CLEAR, null, this));
    }

    /**
     * Change the border of this component to indicate that a drag-and-drop operation is in progress.
     *
     * @param dragInProgress is dnd operation beginning?  (false means it is ending)
     */
    private void setBorder(boolean dragInProgress) {
        if (dragInProgress) {
            scrollpane.setBorder(highlightBorder);
        } else {
            scrollpane.setBorder(defaultBorder);
        }
    }

    /**
     * Return all tracks stored in this component.
     *
     * @return unsorted list of tracks
     */
    public List<Track> getTracks() {
        return dataModel.getTracks();
    }

    /**
     * Add a track to this component.
     *
     * @param track track to add.  duplicates allowed
     */
    public void addTrack(Track track) {
        dataModel.addTrack(track);
        fireChangeEvent(new TrackTableChangeEvent(TrackTableChangeEvent.ChangeType.ADD, track, this));
    }

    /**
     * Set this component as busy or not.  Not interactions will be possible while this component
     * is set to busy.
     *
     * @param locked busy state
     */
    public void setBusy(boolean locked) {
        busyModel.setBusy(locked);
    }

    /**
     * Free resources held by this object.
     */
    public void dispose() {
        tagModel.removeTagChangeListener(dataModel);
    }

    /**
     * Registers a ChangeListener to receive TrackTableChangeEvents.
     *
     * @param l change listener
     */
    public void addChangeListener(ChangeListener l) {
        changeListeners.add(l);
    }

    /**
     * Unregisters a ChangeListener so it no longer receives TrackTableChangeEvents.
     *
     * @param l change listener
     */
    public void removeChangeListener(ChangeListener l) {
        changeListeners.remove(l);
    }

    /**
     * Notify all registered listeners that a change has occured.
     *
     * @param e change event
     */
    public void fireChangeEvent(TrackTableChangeEvent e) {
        for (ChangeListener l : changeListeners) {
            l.stateChanged(e);
        }
    }

    /**
     * Handles dragging and dropping into this window.
     */
    private class FileDropListener implements DropTargetListener {

        /**
         * Approves lists of files.  Changes border to indicate drag is in progress.
         *
         * @param dtde drag event
         */
        @Override
        public void dragEnter(DropTargetDragEvent dtde) {
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                setBorder(true);
                dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
            } else {
                setBorder(false);
                dtde.rejectDrag();
            }
        }

        /**
         * Changes border to indicate no drag is in progress.
         *
         * @param dte drop event
         */
        @Override
        public void dragExit(DropTargetEvent dte) {
            setBorder(false);
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            setBorder(false);
            if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                try {
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    workerThread.execute(new AddFiles(files));
                } catch (UnsupportedFlavorException e) {
                    log.error("error processing file drop", e);
                } catch (IOException e) {
                    log.error("error processing file drop", e);
                }
            } else {
                dtde.rejectDrop();
            }
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
            log.debug("dropActionChanged: " + dtde.toString());
        }
    }

    /**
     * Adds files collected via drag-and-drop to the component.
     */
    private class AddFiles implements Runnable {
        private List<File> files;

        public AddFiles(List<File> files) {
            this.files = new ArrayList<File>(files);
        }

        public void run() {
            setBusy(true);
            final Map<Tuple3<String, String, String>, File> trackTags = new HashMap<Tuple3<String, String, String>, File>();

            // dig into directories recursively
            for (int i = 0; i < files.size(); i++) {
                if (files.get(i).isDirectory()) {
                    log.debug("Adding subdirectories of " + files.get(i));
                    files.addAll(Arrays.asList(files.get(i).listFiles()));
                    files.remove(i);
                    i--; // to account for the fact that we removed the directory
                }
            }

            for (final File file : files) {
                try {
                    log.debug("Loading file: " + file.getAbsolutePath());

                    AudioFile f = AudioFileIO.read(file);
                    Tag tag = f.getTag();

                    String title = tag.getFirst(FieldKey.TITLE);
                    String artist = tag.getFirst(FieldKey.ARTIST);
                    String album = tag.getFirst(FieldKey.ALBUM);

                    final Tuple3<String, String, String> searchKey = Tuple3.of(title, artist, album);

                    List<Track> tracks = mediaPlayer.findTracks(searchKey.a, searchKey.b, searchKey.c);
                    if (tracks.isEmpty()) {
                        addTrack(new UnknownTrack() {
                            @Override
                            public String getTitle() {
                                return searchKey.a;
                            }

                            @Override
                            public String getAlbum() {
                                return searchKey.b;
                            }

                            @Override
                            public String getArtist() {
                                return searchKey.c;
                            }
                        });
                    } else {
                        if (tracks.size() != 1) {
                            log.warn("Search resulted in multiple tracks: " + searchKey);
                        }
                        for (Track track : tracks) {
                            addTrack(track);
                        }
                    }
                } catch (Exception e) {
                    // not a big deal - jaudiotagger didn't recognize this file as having any tags
                    addTrack(new UnknownTrack() {
                        @Override
                        public String getTitle() {
                            return file.getAbsolutePath();
                        }
                    });
                }
            }
            setBusy(false);
        }
    }

    /**
     * A Listener interface for TrackTableComponent that is notified of track additions and removals.
     */
    public static class TrackTableChangeEvent extends ChangeEvent {
        private ChangeType type;
        private Track track;

        public static enum ChangeType { ADD, REMOVE, CLEAR }

        /**
         * Constructs a ChangeEvent object.
         *
         * @param type type of track change (add, remove or clear all)
         * @param track the track affected (null if type is CLEAR)
         * @param source the Object that is the source of the event
         *               (typically <code>this</code>)
         */
        public TrackTableChangeEvent(ChangeType type, Track track, TrackTableComponent source) {
            super(source);
            this.type = type;
            this.track = track;
        }

        public ChangeType getType() {
            return type;
        }

        public Track getTrack() {
            return track;
        }
    }

}
