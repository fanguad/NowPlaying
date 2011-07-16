/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.itunes.remote.connection;

/**
 * Metadata about ResponseIdentifiers.
 */
public enum ContentCodeType {
    LIST, MULTI_INTERNAL,
    STRING, DATE,
    LONG, INTEGER, SHORT, BYTE, BOOLEAN,
    UNSIGNED_BYTE, UNSIGNED_SHORT,
    /**
     * Version is 4 bytes - either 4 singles (0.0.0.0) or 2 short (0.0)
     */
    VERSION,
    SPECIAL
}
