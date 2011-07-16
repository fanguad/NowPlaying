/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.itunes.remote;

import org.apache.log4j.Logger;
import org.nekocode.itunes.remote.connection.ITunesRemoteResponse;
import org.nekocode.itunes.remote.connection.RemoteSession;
import org.nekocode.itunes.remote.connection.RequestManager;
import org.nekocode.nowplaying.NowPlayingProperties;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;

/**
 * A Track that connects to iTunes to load its information automatically.  The constructor
 * blocks while the information is loading.  Artwork is an exception - this is loaded in the
 * background and a TrackChangeEvent is fired when it is complete.
 */
public class SelfLoadingTrack extends ITunesRemoteTrack implements Track {
    private static final Logger log = Logger.getLogger(SelfLoadingTrack.class);

    public SelfLoadingTrack(int trackId,
                            RemoteSession session) throws IOException {

        ITunesRemoteResponse response = session.getMetaData(trackId, DEFAULT_CONTENT_CODES);

        setValues(session.getDatabaseId(), response);

        // this is sufficiently fast that we don't need to make it asynchronous
        loadArtwork(session);
    }

    /**
     * Loads the artwork for this track (synchronously).
     *
     * @param session session to load artwork from
     */
    public void loadArtwork(RemoteSession session) {
        try {
            Properties properties = NowPlayingProperties.loadProperties();
            int artworkSize = Integer.parseInt(properties.getProperty(NowPlayingProperties.ALBUM_ART_SIZE.name(), "300"));
            URL artworkURL = session.getUrlFactory().getArtwork(artworkSize);
            log.debug("Trying to load artwork: " + artworkURL);
            byte[] data = RequestManager.requestBytes(artworkURL);

            Image cover = Toolkit.getDefaultToolkit().createImage(data);

            ImageIcon coverIcon = new ImageIcon(cover);
            SelfLoadingTrack.this.setArtwork(Collections.singleton(coverIcon));
            log.debug("Artwork loaded for \"" + getTitle() + '"');
        } catch (Exception e) {
            log.error("error loading artwork", e);
        }
    }
}
