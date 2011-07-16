/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.itunes.remote.connection;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import static java.lang.String.format;

/**
 * Class that generates URLs to perform various queries on iTunes.
 * TODO check that session id is set if necessary
 *
 * @author fanguad
 */
public class ITunesRemoteURLFactory {
    public static int ITUNES_PORT = 3689;
    private static final String PAIRING = "/login?pairing-guid=0x%s";
    private static final String DATABASES = "/databases?session-id=%s";
    private static final String PLAY_STATUS_UPDATE = "/ctrl-int/1/playstatusupdate?revision-number=%d&session-id=%s";
    private static final String PLAY_PAUSE = "/ctrl-int/1/playpause?session-id=%s";
    private static final String NEXT = "/ctrl-int/1/nextitem?session-id=%s";
    private static final String PREV = "/ctrl-int/1/previtem?session-id=%s";
    // TODO load the artwork for the actual song requested... not just whatever is currently playing
    private static final String NOWPLAYING_ARTWORK = "/ctrl-int/1/nowplayingartwork?mw=%d&mh=%d&session-id=%s";
// /databases/%d/groups/%d/extra_data/artwork?session-id=%s&mw=%d&mh=%d    (database id, item id, size, size)
// /databases/%d/items/%d/extra_data/artwork?session-id=%s&mw=%d&mh=%d

    private static final String GET_META_DATA = "/databases/%d/items?session-id=%s&meta=%s&type=music&query='dmap.itemid:%d'";
    private static final String SET_SONG_RATING = "/ctrl-int/1/setproperty?dacp.userrating=%d&song-spec='dmap.itemid:%d'&session-id=%s'";
    private static final String FIND_TRACKS = "/databases/%d/items?session-id=%s&meta=%s&type=music&sort=album&query=(('dmap.itemname:%s')+('daap.songartist:%s')+('daap.songalbum:%s'))";

    private static final String FIND_TRACKS_PREFIX = "/databases/%d/items?session-id=%s&meta=%s&type=music&sort=album%s";
    private static final String FIND_TRACKS_QUERY = "&query=(%s)";
    private static final String FIND_TRACKS_QUERY_ELEMENT = "('%s:%s')";
    private static final String FIND_TRACKS_QUERY_CONNECTOR = "+";
    private static final String FIND_TRACKS_QUERY_ITEMNAME = "dmap.itemname";
    private static final String FIND_TRACKS_QUERY_ARTIST = "daap.songartist";
    private static final String FIND_TRACKS_QUERY_ALBUM = "daap.songalbum";

    private String host;
    private String sessionId;
    private long databaseId;

    public ITunesRemoteURLFactory(String host) {
        this.host = host;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setDatabaseId(long databaseId) {
        this.databaseId = databaseId;
    }

    /**
     * Retrieve the URL to pair with iTunes.  This query will return a new session id if the
     * pairing GUID is valid.
     *
     * @param pairingGuid previously established pairing GUID
     * @return URL to establish a new session with iTunes
     * @throws java.net.MalformedURLException if pairingGuid causes the URL to be malformed
     */
    public URL getPairing(String pairingGuid) throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(PAIRING, pairingGuid));
    }

    public URL getDatabase() throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(DATABASES, sessionId));
    }

    public URL getPlayStatusUpdate(long revision) throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(PLAY_STATUS_UPDATE, revision, sessionId));
    }

    public URL getPlayPause() throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(PLAY_PAUSE, sessionId));
    }

    public URL getNext() throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(NEXT, sessionId));
    }

    public URL getPrev() throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(PREV, sessionId));
    }

    public URL getArtwork(int maxDimension) throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(NOWPLAYING_ARTWORK, maxDimension, maxDimension, sessionId));
    }

    public URL getGetMetaData(int songId, ContentCode... contentCodes) throws MalformedURLException {
        String metaData = createMetaDataString(contentCodes);

        return new URL("http", host, ITUNES_PORT, format(GET_META_DATA,
                databaseId, sessionId, metaData, songId));
    }

    public URL getSetSongRating(int songId, int rating) throws MalformedURLException {
        return new URL("http", host, ITUNES_PORT, format(SET_SONG_RATING, rating, songId, sessionId));
    }

    /**
     * Search for tracks according to the specified criteria.  title, artist or album may be null, in which case
     * that field is not used in the search.
     *
     * @param title
     * @param artist
     * @param album
     * @param contentCodes
     * @return
     * @throws MalformedURLException
     */
    public URL getTracks(String title, String artist, String album, ContentCode[] contentCodes) throws MalformedURLException {
        StringBuilder sb = new StringBuilder();
        appendQueryElement(sb, FIND_TRACKS_QUERY_ITEMNAME, title);
        appendQueryElement(sb, FIND_TRACKS_QUERY_ARTIST, artist);
        appendQueryElement(sb, FIND_TRACKS_QUERY_ALBUM, album);
        String query = "";
        if (sb.length() > 0) {
            query = format(FIND_TRACKS_QUERY, sb);
        }

        String metaData = createMetaDataString(contentCodes);

        return new URL("http", host, ITUNES_PORT, format(FIND_TRACKS_PREFIX, databaseId, sessionId, metaData, query));
    }

    private StringBuilder appendQueryElement(StringBuilder sb, String fieldname, String searchParameter) throws MalformedURLException {
        if (searchParameter != null) {
            if (sb.length() > 0) {
                sb.append(FIND_TRACKS_QUERY_CONNECTOR);
            }
            try {
                String encodedSearchParameter = encodeString(searchParameter);
                sb.append(format(FIND_TRACKS_QUERY_ELEMENT, fieldname, encodedSearchParameter));
            } catch (UnsupportedEncodingException e) {
                throw new MalformedURLException("cannot encode search criteria: " + e.getMessage());
            }
        }
        return sb;
    }

    private String createMetaDataString(ContentCode[] contentCodes) throws MalformedURLException {
        StringBuilder contentCodeString = new StringBuilder();
        for (ContentCode code : contentCodes) {
            if (code.getFullName() == null) {
                throw new MalformedURLException(format("ContentCode %s does not have a fullname", code));
            }
            contentCodeString.append(code.getFullName()).append(",");
        }
        contentCodeString.deleteCharAt(contentCodeString.length() - 1);
        return contentCodeString.toString();
    }

    /**
     * Encodes a string for use in a URL.  This isn't a universal function - it has some iTunes remote
     * specific additional steps.
     *
     * @param original original string
     * @return encoded version of string
     * @throws UnsupportedEncodingException if the string can't be encoded (shouldn't actually happen)
     */
    private String encodeString(String original) throws UnsupportedEncodingException {
        // encode string as a URL, input is UTF-8
        String result = URLEncoder.encode(original, "UTF-8");
        // replaces slashes with spaces
        result = result.replaceAll("\\+", "%20");
        // replace apostrophes with an escaped apostrophe (weird, but this is what iTunes wants)
        result = result.replaceAll("%27", "%5C'");
        return result;
    }
}
