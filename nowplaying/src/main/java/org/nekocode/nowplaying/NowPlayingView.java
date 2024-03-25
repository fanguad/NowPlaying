/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying;

import lombok.extern.log4j.Log4j2;
import org.nekocode.nowplaying.components.ArtPanel;
import org.nekocode.nowplaying.components.ArtPanelProgressLayerUI;
import org.nekocode.nowplaying.components.NowPlayingControl;
import org.nekocode.nowplaying.components.ResizeUpdateTrack;
import org.nekocode.nowplaying.components.swing.*;
import org.nekocode.nowplaying.components.swing.NekoPanel.BorderPositions;
import org.nekocode.nowplaying.events.TrackChangeEvent;
import org.nekocode.nowplaying.events.TrackChangeEvent.ChangeType;
import org.nekocode.nowplaying.internals.NamedThreadFactory;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main window of the application.
 * <p>
 * The content area of this frame has a BorderLayout, but the CENTER location is already used. Any
 * other position is okay.
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
public class NowPlayingView extends NekoFrame {
    private final static EnumSet<ChangeType> shouldUpdateArtwork = EnumSet.of(
            ChangeType.CURRENT_SONG_CHANGE,
            ChangeType.FILE_CHANGE,
            ChangeType.ART_CHANGE);

    // frame elements
    private final int size;
    private ArtPanel panel;

    // track elements
    private final NekoLabel title;
    private final NekoLabel artist;
    private final NekoLabel album;
    private final NekoLabel grouping;

    // mode elements
    private final NekoButton currentMode;
    private final JPanel currentModeControls;
    private final Queue<String> modes = new LinkedList<>();
    private final Set<NowPlayingControl> modeControls = new HashSet<>();

    // miscellaneous elements
    private final ExecutorService executor;

    private Track track;

    private final Collection<ArtPanelProgressLayerUI> progressLayers = new ArrayList<>();

    private JComponent content;
    /**
     * the object that receives mouse commands
     */
    private JComponent mouseListenerTarget;

    /**
     * Creates a NowPlayingView, the GUI portion of the application.
     */
    public NowPlayingView(List<NowPlayingControl> modes) {
        super("Now Playing...");

        executor = Executors.newCachedThreadPool(new NamedThreadFactory("NowPlayingView", false));

        Properties properties = NowPlayingProperties.loadProperties();

        size = Integer.parseInt(properties.getProperty(NowPlayingProperties.ALBUM_ART_SIZE.name(), "300"));

        // create information display
        title = new NekoLabel("NowPlayingInfo", JLabel.CENTER);
        artist = new NekoLabel("by", JLabel.CENTER);
        album = new NekoLabel("fanguad", JLabel.CENTER);
        grouping = new NekoLabel("dan.clark@nekocode.org", JLabel.CENTER);

        // create mode objects
        currentMode = new NekoButton("", Rotation.COUNTER_CLOCKWISE);
        currentMode.setHorizontalAlignment(JLabel.CENTER);
        currentModeControls = new JPanel(new CardLayout());
        currentModeControls.setOpaque(false);
        // a bit of a hack - uses knowledge of ArtPanel's corner size
        int maxControlSize = (int) (0.8 * size);
        currentModeControls.setMaximumSize(new Dimension(maxControlSize, maxControlSize));
        currentModeControls.setBorder(BorderFactory.createEmptyBorder(0, 0, 3, 0));

        // create LayoutManager and position components
        layOutComponents();

        NekoPanel contentPane = new NekoPanel() {
            boolean centerSet = false;

            /**
             * Once CENTER has been set, disallows setting the CENTER component of this panel.
             */
            @Override
            public void add(Component comp, Object constraints) {
                if (constraints == BorderLayout.CENTER) {
                    if (!centerSet) {
                        centerSet = true;
                    } else {
                        throw new IllegalArgumentException(
                                "CENTER portion of NowPlayingView content pane cannot be set.");
                    }
                }
                super.add(comp, constraints);
            }
        };
        setContentPane(contentPane);

        currentMode.addActionListener(e -> rotateMode());

        // remove Space from the button so it won't consume these keys
        currentMode.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
//        InputMap keymap = currentMode.getInputMap();
//        keymap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false));
//        keymap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0, false));

        getContentPane().add(content, BorderLayout.CENTER);

        // remove the keybinding for TAB
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, new HashSet<KeyStroke>());

        Action rotateMode = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotateMode();
            }
        };
        contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("TAB"), "rotateMode");
        contentPane.getActionMap().put("rotateMode", rotateMode);

        modes.forEach(mode -> addMode(mode.getModeName(), mode));
        modes.stream().findFirst()
                .map(NowPlayingControl::getModeName)
                .ifPresent(this::setMode);
    }

    /**
     * Lay components in the frame.
     */
    private void layOutComponents() {
        JPanel trackInfo = new JPanel(new GridLayout(0, 1));
        trackInfo.add(title);
        trackInfo.add(artist);
        trackInfo.add(album);
        trackInfo.add(grouping);
        trackInfo.setOpaque(false);
        trackInfo.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));

        panel = new ArtPanel(size, false, false);
        panel.setLayout(new BorderLayout());

        JLayer<JComponent> panelLayer;
        // add the underneath progressbar
        ArtPanelProgressLayerUI progressUI = new ArtPanelProgressLayerUI(panel, 3, Color.white);
        progressLayers.add(progressUI);
        panelLayer = new JLayer<>(panel, progressUI);

        // add another progressbar on top of that
        progressUI = new ArtPanelProgressLayerUI(panel, 2, Color.black);
        progressLayers.add(progressUI);
        panelLayer = new JLayer<>(panelLayer, progressUI);

        content = panelLayer;
        mouseListenerTarget = panelLayer.getView();

        JPanel fixedPanel = new JPanel(new BorderLayout());
        fixedPanel.setOpaque(false);
        fixedPanel.add(trackInfo, BorderLayout.PAGE_START);
        fixedPanel.add(currentModeControls, BorderLayout.PAGE_END);
        panel.add(fixedPanel, BorderLayout.CENTER);

        NekoLabel spacer = new NekoLabel(" ", Rotation.CLOCKWISE);
        spacer.setHorizontalAlignment(JLabel.CENTER);

        // wrapping "currentMode" in a JPanel will prevent it
        // from filling the full vertical height of the window
        JPanel currentModeFiller = new JPanel();
        currentModeFiller.setLayout(new BoxLayout(currentModeFiller, BoxLayout.LINE_AXIS));
        currentModeFiller.setOpaque(false);
        currentModeFiller.add(currentMode);

        panel.add(currentModeFiller, BorderLayout.LINE_START);
        panel.add(spacer, BorderLayout.LINE_END);
    }

    /**
     * Called when the gui should be updated - either because a different track
     * is playing, or because some property of the current track has been changed.
     * Initiates an update of the GUI.  This method is safe to call from any
     * thread.
     *
     * @param trackChange track change event
     */
    public void updateTrack(TrackChangeEvent trackChange) {
        track = trackChange.getTrack();
        log.info("updateTrack: " + trackChange.getType());

        if (track == null)
        {
            log.error("TrackChangeEvent received with null track (should not happen)");
            return;
        }

        // update the text labels
        Runnable updateInfo = () -> {
            title.setText(track.getTitle());
            artist.setText(track.getArtist());
            album.setText(track.getAlbum());
            grouping.setText(track.getGrouping());
        };
        SwingUtilities.invokeLater(updateInfo);

        // update all the mode controls
        modeControls.forEach(npc -> executor.execute(() -> npc.updateTrack(trackChange)));

        if (shouldUpdateArtwork.contains(trackChange.getType())) {
            // update the artwork
            ResizeUpdateTrack updateArtwork = new ResizeUpdateTrack();
            updateArtwork.setFrame(this);
            updateArtwork.setComponent(panel, size, size);
            updateArtwork.execute(track);
        }
    }

    /**
     * Add a component to the interior of the view, as opposed to the outside, which will result in
     * a drawer (in the current implementation).
     *
     * @param position LINE_START or LINE_END
     */
    public void addInternal(Component c, BorderPositions position) {
        panel.add(c, position.getLayoutPosition());
    }


    /**
     * Adds a mode to the display. Each mode has a display name, as well as
     * controls. By default, there is a "controls" mode containing
     * playback and rating controls.
     *
     * @param name     name displayed to user
     * @param controls UI interface components
     */
    public void addMode(String name, NowPlayingControl controls) {
        modes.add(name);
        modeControls.add(controls);
        currentModeControls.add(controls, name);
        controls.setOpaque(false);

        // update the nextMode label
        //   if there is only 1 mode, there is no "next" mode
        if (!modes.isEmpty()) {
            currentMode.setText(modes.peek());
        }
    }

    /**
     * Changes the mode to the next mode in the list, and rotate the list.
     */
    public void rotateMode() {
        String oldMode = modes.poll();
        if (oldMode != null) {
            modes.add(oldMode);
            setMode(modes.peek());
        }
    }

    /**
     * Changes the mode to the specified mode. If the mode does not exist,
     * nothing happens.
     *
     * @param name name of new mode
     */
    public void setMode(String name) {
        if (modes.contains(name)) {
            ((CardLayout) currentModeControls.getLayout()).show(currentModeControls, name);
            currentMode.setText(name);
        }
    }

    /**
     * Gets the track which is currently displaying in the view.
     *
     * @return the track
     */
    public Track getCurrentTrack() {
        return track;
    }

    /**
     * Updates the displayed progress of the track.  Set it to the given
     * percentage (0-1).
     *
     * @param percent progress of track [0, 1]
     */
    public void updateTrackProgress(double percent) {
        for (ArtPanelProgressLayerUI l : progressLayers) {
            l.setTrackProgress(percent);
        }
    }

    public void shutdown() {
        log.info("shutting down view");

        // save anchor and position
        Properties properties = NowPlayingProperties.loadProperties();
        String anchorProperty = getAnchor().name();
        String positionProperty = String.format("%d, %d", getAnchorX(), getAnchorY());
        log.debug("Saving window anchor: " + anchorProperty);
        log.debug("Saving window location: " + positionProperty);
        properties.put(NowPlayingProperties.WINDOW_ANCHOR.name(), anchorProperty);
        properties.put(NowPlayingProperties.WINDOW_POSITION.name(), positionProperty);

        // update all the mode controls
        modeControls.forEach(npc -> executor.execute(npc::shutdown));
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("interrupted while shutting down view", e);
        }
        log.info("finished shutting down view");
    }

    @Override
    public synchronized void addMouseListener(MouseListener l) {
        content.addMouseListener(l);
        mouseListenerTarget.addMouseListener(l);
    }

    @Override
    public synchronized void addMouseMotionListener(MouseMotionListener l) {
        content.addMouseMotionListener(l);
        mouseListenerTarget.addMouseMotionListener(l);
    }

    @Override
    public Dimension getPreferredSize() {
        return panel.getPreferredSize();
    }

    public void setInitialLocation()
    {
        Properties properties = NowPlayingProperties.loadProperties();
        String anchorPositionProperty = properties.getProperty(NowPlayingProperties.WINDOW_ANCHOR.name());
        if (anchorPositionProperty != null) {
            log.debug("Setting initial anchor to saved value: " + anchorPositionProperty);
            AnchorPosition savedAnchor = AnchorPosition.valueOf(anchorPositionProperty);
            setAnchor(savedAnchor);
        }
        String windowPositionProperty = properties.getProperty(NowPlayingProperties.WINDOW_POSITION.name());
        if (windowPositionProperty == null) {
            setLocationByPlatform(true);
        } else {
            log.debug("Setting initial location to saved value: " + windowPositionProperty);
            String[] xyPositions = windowPositionProperty.split("\\s*,\\s*");
            int xPosition = Integer.parseInt(xyPositions[0]);
            int yPosition = Integer.parseInt(xyPositions[1]);
            log.debug(String.format("preferred size: h:%d, w:%d",
                    (int) getPreferredSize().getHeight(), (int) getPreferredSize().getWidth()));

            if (getAnchor() == AnchorPosition.SOUTHEAST || getAnchor() == AnchorPosition.SOUTHWEST) {
                yPosition -= (int) getPreferredSize().getHeight();
            }
            if (getAnchor() == AnchorPosition.NORTHEAST || getAnchor() == AnchorPosition.SOUTHEAST) {
                xPosition -= (int) getPreferredSize().getWidth();
            }
            log.debug(String.format("initial location after considering anchor: %d, %d", xPosition, yPosition));

            setLocation(xPosition, yPosition);
        }
    }
}
