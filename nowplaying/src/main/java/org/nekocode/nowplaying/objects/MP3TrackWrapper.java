/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.objects;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Frames;
import org.jaudiotagger.tag.id3.framebody.FrameBodyUFID;

/**
 * Implements certain functionality on FileTracks that relates to MP3s.
 *
 * @author fanguad@nekocode.org
 */
public class MP3TrackWrapper {
	private static final Logger log = Logger.getLogger(MP3TrackWrapper.class);
	//TODO move this to a property somewhere
	public static final String UFID_OWNER = "nekocode.org";

	private MP3File mp3file;
	private UUID uuid;

    public MP3TrackWrapper(File file) throws CannotUseMP3TrackException {
        boolean writable = file.canWrite();
		try {
	        mp3file = new MP3File(file, MP3File.LOAD_ALL, !writable);
        } catch (ReadOnlyFileException e) {
        	// ignore, the rest of the class will work around this
        } catch (IOException e) {
        	throw new CannotUseMP3TrackException(e);
        } catch (TagException e) {
        	throw new CannotUseMP3TrackException(e);
        } catch (InvalidAudioFrameException e) {
        	throw new CannotUseMP3TrackException(e);
        }

        getUUID();

        if (mp3file == null)
        	throw new CannotUseMP3TrackException("File was not an MP3 file");
        if (getUUID() == null)
        	throw new CannotUseMP3TrackException("Unable to create UUID for file. Not an MP3 file?");

        log.info("MP3Track created with UUID " + getUUID().toString());
	}

	public UUID getUUID() {
		if (uuid != null) {
			return uuid;
		}

		UUID ret = null;

		if (mp3file.hasID3v2Tag()) {
			AbstractID3v2Tag tag = mp3file.getID3v2Tag();

			Object frame = tag.getFrame(ID3v24Frames.FRAME_ID_UNIQUE_FILE_ID);
			if (frame == null) {
				log.debug("No UFID frame found for file " + mp3file.getFile().getAbsolutePath());
				// need to create a new frame
				ret = addUFID(tag);
			} else if (frame instanceof AbstractID3v2Frame) {
				// there is a single frame, check if we can understand it
				FrameBodyUFID body = ((FrameBodyUFID)((AbstractID3v2Frame)frame).getBody());
				if (UFID_OWNER.equals(body.getOwner())) {
					log.debug("UFID frame found for file " + mp3file.getFile().getAbsolutePath());
					ret = UUID.fromString(new String(body.getUniqueIdentifier()));
				} else {
					log.debug("No UFID frame found for file " + mp3file.getFile().getAbsolutePath());
					ret = addUFID(tag);
				}
			} else {
				// there are multiple frames - see if we can understand any of them
				Collection<?> c = (Collection<?>) frame;
				for (Object eachFrame : c) {
					FrameBodyUFID body = ((FrameBodyUFID)((AbstractID3v2Frame)eachFrame).getBody());
					if (UFID_OWNER.equals(body.getOwner())) {
						ret = UUID.fromString(new String(body.getUniqueIdentifier()));
						log.debug("UFID frame found for file " + mp3file.getFile().getAbsolutePath());
						break;
					}
				}
				// if ret is still null, we need to make our own
                if (ret == null) {
                    log.debug("No UFID frame found for file " + mp3file.getFile().getAbsolutePath());
                    ret = addUFID(tag);
                }
			}
		} else {
			log.error("file did not have ID3 tag to retrieve unique ID from");
			ret = null;
		}
		uuid = ret;
		return ret;
	}

	private UUID addUFID(AbstractID3v2Tag tag) {
	    FrameBodyUFID ufid = new FrameBodyUFID();
	    ufid.setOwner(UFID_OWNER);
	    UUID uuid = UUID.randomUUID();
	    ufid.setUniqueIdentifier(uuid.toString().toUpperCase().getBytes());
	    // TODO report bug that this doesn't set the identifier
//	    ID3v22Frame frame = new ID3v22Frame(ufid);

	    /* workaround */
	    ID3v24Frame frame = new ID3v24Frame(ID3v24Frames.FRAME_ID_UNIQUE_FILE_ID);
	    frame.setBody(ufid);
	    /* end workaround */

		tag.setFrame(frame);
	    try {
	    	log.debug("saving data to mp3file");
	        mp3file.save();
	    } catch (IOException e) {
	        log.error(e);
	    } catch (TagException e) {
	        log.error(e);
	    }
	    return uuid;
    }

	/**
	 * Catch-all exception that instructs the caller to use a more generic
	 * class than this.
	 *
	 * @author fanguad@nekocode.org
	 */
	@SuppressWarnings("serial")
    public class CannotUseMP3TrackException extends Exception {
		public CannotUseMP3TrackException(String message) {
			super(message);
		}
		public CannotUseMP3TrackException(Exception cause) {
			super(cause);
		}
	}

	public AbstractID3v2Tag getID3() {
		return mp3file.getID3v2Tag();
	}
}
