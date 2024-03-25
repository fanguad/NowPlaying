/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.objects;

import java.util.UUID;

public interface MP3Track extends FileTrack {
	UUID getUUID();
}
