/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.objects;

import java.util.UUID;

public interface MP3Track extends FileTrack {
	public UUID getUUID();
}
