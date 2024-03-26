/*
 * Copyright (c) 2024. Dan Clark
 */

package org.nekocode.nowplaying.remote.mediamonkey5;

import org.nekocode.nowplaying.objects.Track;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Log4j2
@Getter
public class MM5Track implements Track {
    private final MM5Connection connection;
    private final int persistentId;
    private final String title;
    private final String artist;
    private final String album;
    private final int rating;
    private final String genre;
    private final String comment;
    private final String grouping1;
    private final double duration;
    private final int trackNumber;
    private final Object artworkLock = new Object();
    private final Collection<ImageIcon> artwork = new ArrayList<>();
    private final Collection<String> artworkDescription = new ArrayList<>();

    @Override
    public boolean isCompilation() {
        return false;
    }

    @Override
    public String getComposer() {
        return null;
    }

    @Override
    public int getDiscCount() {
        return 0;
    }

    @Override
    public int getDiscNumber() {
        return 0;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getGrouping() {
        return grouping1;
    }

    @Override
    public int getPlayedCount() {
        return 0;
    }

    @Override
    public Date getPlayedDate() {
        return null;
    }

    @Override
    public int getTrackCount() {
        return 0;
    }

    @Override
    public int getVolumeAdjustment() {
        return 0;
    }

    @Override
    public int getYear() {
        return 0;
    }

    @Override
    public int getBitRate() {
        return 0;
    }

    @Override
    public int getBpm() {
        return 0;
    }

    @Override
    public Date getDateAdded() {
        return null;
    }

    @Override
    public Date getModificationDate() {
        return null;
    }

    @Override
    public int getSampleRate() {
        return 0;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String getTime() {
        return null;
    }

    @Override
    public int getTrackId() {
        return persistentId;
    }

    @Override
    public long getDatabaseId() {
        return 0;
    }

    @Override
    public Object getOriginal() {
        return null;
    }

    @Override
    public String getPersistentId() {
        return persistentId + "";
    }

    @Override
    public Collection<ImageIcon> getArtwork(int size) {
        synchronized (artworkLock) {
            if (!artwork.isEmpty()) {
                return artwork.stream()
                        .map(image -> resizeArt(image, size))
                        .collect(Collectors.toList());
            }
        }

        // asynchronously acquire a reference to the artwork
        // TODO figure out how to do this synchronously
        String getCover = """
                    app.getObject('track', { id: %d })
                        .then(function(track) {
                            if (track) {
                                var cover = track.getThumbAsync(%d, %d,
                                    function(thumb) {
                                        console.debug('thumbnail:%d:' + thumb);
                                    });
                            }
                        });
                """.formatted(persistentId, size, size, persistentId);
        try {
            connection.evaluateAsync(getCover);
        } catch (ScriptException e) {
            log.error("Error loading cover art", e);
        }
        return Collections.emptyList();
    }

    @Override
    public Collection<String> getArtworkDescriptions() {
        synchronized (artworkLock) {
            return artworkDescription;
        }
    }

    /**
     * only scales down
     */
    private ImageIcon resizeArt(ImageIcon imageIcon, int size) {
        if (imageIcon.getIconWidth() > size || imageIcon.getIconHeight() > size) {
            Image image = imageIcon.getImage();
            int height = imageIcon.getIconHeight() >= imageIcon.getIconWidth()
                    ? size
                    : -1;
            int width = imageIcon.getIconWidth() >= imageIcon.getIconHeight()
                    ? size
                    : -1;
            Image newimg = image.getScaledInstance(width, height, java.awt.Image.SCALE_SMOOTH);
            return new ImageIcon(newimg);
        } else {
            return imageIcon;
        }
    }

    public void addArtwork(String description, ImageIcon imageIcon) {
        synchronized (artworkLock) {
            artwork.add(imageIcon);
            artworkDescription.add(description);
        }
    }
}
