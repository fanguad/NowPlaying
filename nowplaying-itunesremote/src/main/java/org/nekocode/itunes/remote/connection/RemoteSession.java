/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.itunes.remote.connection;

import org.apache.log4j.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static org.nekocode.itunes.remote.connection.ContentCode.*;

/**
 * A remote session with iTunes.
 */
public class RemoteSession {

    private static final Logger log = Logger.getLogger(RemoteSession.class);
    public final static String REMOTE_TYPE = "_touch-remote._tcp.local.";
    public final static int LISTEN_PORT = 49197;

    // Raw Bytes for the pairing response - thanks to tunescontrol
    public static byte[] PAIRING_RAW = new byte[]{0x63, 0x6d, 0x70, 0x61, 0x00, 0x00, 0x00, 0x3a, 0x63, 0x6d, 0x70, 0x67, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x63, 0x6d, 0x6e, 0x6d, 0x00, 0x00, 0x00, 0x16, 0x41, 0x64, 0x6d, 0x69, 0x6e, 0x69, 0x73, 0x74, 0x72,
            0x61, 0x74, 0x6f, 0x72, (byte) 0xe2, (byte) 0x80, (byte) 0x99, 0x73, 0x20, 0x69, 0x50, 0x6f, 0x64, 0x63, 0x6d, 0x74, 0x79, 0x00, 0x00, 0x00, 0x04, 0x69, 0x50, 0x6f, 0x64};

    private ITunesRemoteURLFactory urlFactory;
    private String pairingGuid;
    private AtomicLong revision;
    private long databaseId;

    /**
     * Create a RemoteSession with the specified host, which has already been paired using a specified pairing GUID.
     *
     * @param host host to connect to
     * @param pairingGuid pre-established pairing GUID
     * @throws IOException if a connection failure happens, including a failure to establish a session id
     */
    public RemoteSession(String host, String pairingGuid) throws IOException {
        this.pairingGuid = pairingGuid;
        revision = new AtomicLong(1);
        urlFactory = new ITunesRemoteURLFactory(host);

        log.debug(format("attempting to connect to [%s] with pairing GUID [%s]", host, pairingGuid));
        ITunesRemoteResponse login = RequestManager.request(urlFactory.getPairing(pairingGuid), false);
        if (!login.hasLeaf(DMAP_LOGIN_RESPONSE, DMAP_SESSION_ID)) {
            throw new IOException(format("Unable connect with pairing GUID [%s]", pairingGuid));
        }

        String sessionId = login.getString(DMAP_LOGIN_RESPONSE, DMAP_SESSION_ID);
        log.debug(format("connected to [%s]: session id: [%s]", host, sessionId));

        urlFactory.setSessionId(sessionId);
        ITunesRemoteResponse response = RequestManager.request(urlFactory.getDatabase(), false);

        ITunesRemoteResponse firstItem = response.getMultiBranch(DAAP_SERVER_DATABASES, DMAP_LIST, DMAP_LIST_ITEM).get(0);
        databaseId = firstItem.getLong(DMAP_ITEM_ID);
//        String databasePersistentId = firstItem.getHexNumber(DMAP_ITEM_ID);
        urlFactory.setDatabaseId(databaseId);
//        log.info(RequestManager.request(new URL("http://localhost:3689/content-codes"), false));
    }

    public String getPairingGuid() {
        return pairingGuid;
    }

    public ITunesRemoteResponse getPlayStatusUpdate() throws IOException {
//        log.debug("Requesting play status update");
        ITunesRemoteResponse response = RequestManager.request(urlFactory.getPlayStatusUpdate(1), false);
        // update the revision number
        updateRevision(response);
        return response;
    }

    private void updateRevision(ITunesRemoteResponse response) {
        if (response == null || response.getBranch(DMCP_STATUS).getNumber(DMCP_REVISION_NUMBER) == null) {
            log.error("play status update did not contain DMCP_REVISION_NUMBER: " + DMCP_REVISION_NUMBER.id);
        } else {
            revision.set(response.getBranch(DMCP_STATUS).getNumber(DMCP_REVISION_NUMBER).longValue());
        }
    }

    public ITunesRemoteResponse waitForPlayStatusUpdate() throws IOException {
        log.debug("Waiting for play status update");
        ITunesRemoteResponse response = RequestManager.request(urlFactory.getPlayStatusUpdate(revision.get()), true);
        // update the revision number
        updateRevision(response);
        return response;
    }

    public void playPause() throws IOException {
        RequestManager.send(urlFactory.getPlayPause());
    }

    public void next() throws IOException {
        RequestManager.send(urlFactory.getNext());
    }

    public void prev() throws IOException {
        RequestManager.send(urlFactory.getPrev());
    }

//    public int getRating(int songId) throws IOException {
//        ITunesRemoteResponse response = RequestManager.request(urlFactory.getGetSongRating(songId), false);
//        // TODO make the next call more robust
//        return response.getBranch(DAAP_DATABASE_SONGS).getBranch(DMAP_LIST).getMultiBranch(DMAP_LIST_ITEM).get(0).getInt(DAAP_SONG_USER_RATING);
//    }

    public void setRating(int songId, int rating) throws IOException {
        RequestManager.send(urlFactory.getSetSongRating(songId, rating));
    }

    public ITunesRemoteURLFactory getUrlFactory() {
        return urlFactory;
    }

    /**
     * Pair with iTunes and return a RemoteSession when pairing is complete.
     * <p>
     * This is method is synchronous on user input, so expect it to take a while!
     *
     * @param host host itunes is running on
     * @return a RemoteSession ready for use
     * @throws java.io.IOException if this class was unable to pair (including errors caused by wrong codes and such)
     */
    public static RemoteSession pairWithITunes(String host) throws IOException {
        log.debug("Attempting to pair with iTunes");

        Random random = new Random();
        Hashtable<String, String> values = new Hashtable< >();
        values.put("DvNm", "NowPlaying...");
        values.put("RemV", "10000");
        values.put("DvTy", "iPod");
        values.put("RemN", "Remote");
        values.put("txtvers", "1");
        // NOTE: not GUID - that comes after we receive confirmation from server
        values.put("Pair", Long.toHexString(random.nextLong()));

        // TODO what is this name for?
        byte[] name = new byte[20];
        random.nextBytes(name);

        ServiceInfo pairservice = new ServiceInfo(REMOTE_TYPE, getHex(name), LISTEN_PORT, 0, 0, values);

        InetAddress addr = InetAddress.getLocalHost();
        JmDNS zeroConf = new JmDNS(addr);
        zeroConf.registerService(pairservice);

        // listen for a response
        ServerSocket server = new ServerSocket(LISTEN_PORT);
        Socket socket = server.accept();

        // don't actually care about the response for now: rules for interpreting the hash are online somewhere

        if (log.isDebugEnabled()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (br.ready()) {
                log.debug(br.readLine());
            }
        }

        try (OutputStream output = socket.getOutputStream()) {
            // create a new GUID and pass it to iTunes
            byte[] code = new byte[8];
            random.nextBytes(code);
            System.arraycopy(code, 0, PAIRING_RAW, 16, 8);
            String pairingGuid = getHex(code);

            byte[] header = String.format("HTTP/1.1 200 OK\r\nContent-Length: %d\r\n\r\n", PAIRING_RAW.length).getBytes();
            byte[] reply = new byte[header.length + PAIRING_RAW.length];

            System.arraycopy(header, 0, reply, 0, header.length);
            System.arraycopy(PAIRING_RAW, 0, reply, header.length, PAIRING_RAW.length);

            output.write(reply);

            return new RemoteSession(host, pairingGuid);
        } finally {
            zeroConf.unregisterService(pairservice);
        }
    }

    static final String HEXES = "0123456789ABCDEF";
    public static String getHex(byte[] raw) {
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4))
                    .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }

    public long getDatabaseId() {
        return databaseId;
    }

    public ITunesRemoteResponse getMetaData(int songId, ContentCode... contentCodes) throws IOException {
        return RequestManager.request(urlFactory.getGetMetaData(songId, contentCodes), false);
    }

    public ITunesRemoteResponse findTracks(String title, String artist, String album, ContentCode... contentCodes) throws IOException {
        return RequestManager.request(urlFactory.getTracks(title, artist, album, contentCodes), false);
    }
}
