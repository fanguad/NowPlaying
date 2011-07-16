/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.tags;

import org.apache.log4j.Logger;
import org.nekocode.nowplaying.NowPlayingProperties;
import org.nekocode.nowplaying.events.TagChangeListener;
import org.nekocode.nowplaying.internals.DaemonThreadFactory;
import org.nekocode.nowplaying.objects.FileTrack;
import org.nekocode.nowplaying.objects.MP3Track;
import org.nekocode.nowplaying.objects.Track;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;
import org.nekocode.nowplaying.tags.cloud.TagCloudGroup;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static org.nekocode.nowplaying.tags.TagModel.StatementName.*;

/**
 * Tag model. Keeps track of tags.
 * <p>
 * Each public method is back by a private method. The public method enforces
 * single-threaded behavior (on the dbAccess executor's thread). The private
 * method does the actual work. Methods that are not publicly accessible do not
 * need to wrapped in a Runnable/Callable, since they will only be access from
 * within the dbAccess thread.
 *
 * @author fanguad@nekocode.org
 */
public class TagModel
{
	private static final Logger log = Logger.getLogger(TagModel.class);
	private final Set<TagChangeListener> tagChangeListeners;
	private final Executor tagChangeExecutor;
	private final ExecutorService dbAccess;
	private final Map<StatementName, PreparedStatement> statementCache;

	private Connection conn;

	public TagModel() throws ClassNotFoundException, SQLException {
		tagChangeListeners = new CopyOnWriteArraySet<>();
		tagChangeExecutor = Executors.newSingleThreadExecutor(new DaemonThreadFactory());
		dbAccess = Executors.newSingleThreadExecutor();

		statementCache = new HashMap<>();

		String tagDatabase = NowPlayingProperties.loadProperties().getProperty(NowPlayingProperties.TAG_DATABASE.name());

        Class.forName("org.sqlite.JDBC");
        log.info("opening database connection");
        conn = DriverManager.getConnection("jdbc:sqlite:" + tagDatabase);
    }

	/**
	 * Adds a listener that will be notified when tags are updated.
	 *
	 * @param l listener to add
	 */
	public void addTagChangeListener(TagChangeListener l) {
		tagChangeListeners.add(l);
	}

    public void removeTagChangeListener(TagChangeListener l) {
        tagChangeListeners.remove(l);
    }

	private void __addTag(Track track, String tag, String metadata) {
		// don't allow leading or trailing whitespace (it's probably a mistake, anyway)
		tag = tag.trim();
		try {
			String uuid = getTrackUUID(track);

			// first, get the tag id from the tags table
			int tagId = getOrAddTagId(tag, metadata);

			// insert the entry into the track_tags table
			PreparedStatement addTagStmt = getStatementFromCache(addTrackTag,
				"INSERT OR IGNORE INTO track_tags(uuid, tag_id) VALUES (?, ?)");
			addTagStmt.setString(1, uuid);
			addTagStmt.setInt(2, tagId);
			addTagStmt.executeUpdate();
			addTagStmt.close();

			updateTagCount(tagId);

			log.debug(String.format("Added [%s] to %s", tag, uuid));
			fireTagAddedEvent(track, tag);

		} catch (SQLException e) {
			// let listeners know that we tried to add something
			fireTagAddedEvent(track, null);
			log.error("SQLException.errorCode = " + e.getErrorCode());
			log.error("SQLException", e);
		}
	}

	private void __addTag(Collection<Track> tracks, String tag, String metadata) {
		// don't allow leading or trailing whitespace (it's probably a mistake, anyway)
		tag = tag.trim();
		try {
			// first, get the tag id from the tags table
			int tagId = getOrAddTagId(tag, metadata);

			PreparedStatement addTagStmt = getStatementFromCache(addTrackTag,
					"INSERT OR IGNORE INTO track_tags(uuid, tag_id) VALUES (?, ?)");
			for (Track track : tracks) {
				String uuid = getTrackUUID(track);
				// insert the entry into the track_tags table
				addTagStmt.setString(1, uuid);
				addTagStmt.setInt(2, tagId);
				addTagStmt.addBatch();

				log.debug(String.format("Added [%s] to %s", tag, uuid));
			}
			addTagStmt.executeBatch();
			addTagStmt.close();
			log.debug("Finished updating tracks");

			updateTagCount(tagId);
			log.debug(String.format("Updated tag count for [%s]", tag));

            for (Track track : tracks) {
                fireTagAddedEvent(track, tag);
            }
		} catch (SQLException e) {
			// let listeners know that we tried to add something
			for (Track track : tracks) {
				fireTagAddedEvent(track, null);
			}
			log.error("SQLException", e);
		}
	}

	private void updateTagCount(int tagId) throws SQLException {
		PreparedStatement updateTagCountStmt = getStatementFromCache(updateTagCount,
			"UPDATE tags SET count = (SELECT COUNT(tag_id) FROM track_tags WHERE tag_id = ?) WHERE tag_id = ?");
		updateTagCountStmt.setInt(1, tagId);
		updateTagCountStmt.setInt(2, tagId);
		updateTagCountStmt.executeUpdate();
		updateTagCountStmt.close();
	}

	private int getOrAddTagId(String tag, String metadata) throws SQLException {
		int tagId = getTagId(tag);

		if (tagId < 0) {
			// we need to add the tag
			PreparedStatement addTagStmt = getStatementFromCache(addTag,
				"INSERT INTO tags(name, metadata) VALUES (?, ?)");
			addTagStmt.setString(1, tag);
			addTagStmt.setString(2, metadata);
			addTagStmt.executeUpdate();

			tagId = getTagId(tag);
			addTagStmt.close();
			if (tagId < 0) {
				throw new SQLException("unable to retrieve tag ID or create a new entry");
			}
		}
		return tagId;
	}

	private int getTagId(String tag) throws SQLException {
		int tagId;
		PreparedStatement stmt = getStatementFromCache(getTagId,
			"SELECT tag_id FROM tags WHERE name = ?");
		stmt.setString(1, tag);
		ResultSet rs = stmt.executeQuery();

		if (rs.next()) {
			tagId = rs.getInt("tag_id");
		} else {
			tagId = -1;
		}
		stmt.close();
		return tagId;
	}

	private Collection<TagCloudEntry> __getAllTags(int minimum) {
		List<TagCloudEntry> ret = new ArrayList<>();

		try {
			int max = __getMaxTagCount();
			// doesn't include tags with less than minimum number of entries
			PreparedStatement stmt = getStatementFromCache(getTagCounts,
				"SELECT name, metadata, count FROM tags WHERE count >= ?");
			stmt.setInt(1, minimum);

			ResultSet rs = stmt.executeQuery();

			while (rs.next()) {
				ret.add(new TagCloudEntry(rs.getString("name"),
						rs.getString("metadata"), rs.getInt("count"), max));
			}
			rs.close();
		} catch (SQLException e) {
			log.error("SQLException", e);
		}
		return ret;
	}

	private int __getMaxTagCount() {
		int ret = 0;
		try {
			PreparedStatement stmt = getStatementFromCache(getMaxTagCount,
					"SELECT MAX(count) AS max_count FROM tags");
			ResultSet rs = stmt.executeQuery();
			rs.next();
			ret = rs.getInt("max_count");
			rs.close();
		} catch (SQLException e) {
			log.error("SQLException", e);
		}
		return ret;
	}

	private List<TagCloudEntry> __getTags(String uuid) {
		List<TagCloudEntry> ret = new ArrayList<>();
		if (uuid == null) {
			return ret;
        }

		try {
			Collection<String> duplicateUUIDs = __getDuplicateTracks(uuid);

			Set<TagCloudEntry> allEntries = new HashSet<>();
			allEntries.addAll(getTags(uuid));
			for (String duplicateUUID : duplicateUUIDs) {
				allEntries.addAll(getTags(duplicateUUID));
			}

			Collection<String> groups = __getGroups(uuid);
			for (String group : groups) {
				allEntries.add(new TagCloudGroup(group));
			}

			ret.addAll(allEntries);
		} catch (SQLException e) {
			log.error("SQLException", e);
		}
		return ret;
	}

    /**
     * Internal method for retrieving tags for multiple tracks at once.
     *
     * @param tracks tracks to get tags for
     * @return map of tracks to tags
     */
    private Map<Track, List<TagCloudEntry>> __getTags(Collection<Track> tracks) {
        Map<Track, List<TagCloudEntry>> ret = new HashMap<>();
        if (tracks == null || tracks.isEmpty())
            return ret;

        for (Track track : tracks) {
            try {
                String uuid = getTrackUUID(track);
                ret.put(track, __getTags(uuid));
            } catch (SQLException e) {
                log.error("SQLException", e);
            }
        }
        return ret;
    }

    /**
     * Internal method for retrieving tags for multiple tracks (by id) at once.
     *
     * @param tracks track ids to get tags for
     * @return map of track ids to tags
     */
    private Map<String, List<TagCloudEntry>> __getTagsById(Collection<String> tracks) {
        Map<String, List<TagCloudEntry>> ret = new HashMap<>();
        if (tracks == null || tracks.isEmpty())
            return ret;

        for (String trackId : tracks) {
            try {
                String uuid = getUUIDFromTrackId(trackId);
                ret.put(trackId, __getTags(uuid));
            } catch (SQLException e) {
                log.error("SQLException", e);
            }
        }
        return ret;
    }

	/**
	 * Returns a list of tracks that have been marked as duplicates of the
	 * selected track.
	 *
	 * @param uuid ID of track
	 * @return UUIDs of all duplicate tracks (does not include input UUID)
     * @throws java.sql.SQLException if database errors occur
	 */
	private Collection<String> __getDuplicateTracks(String uuid) throws SQLException {
		List<String> duplicateIds = new ArrayList<>();

		PreparedStatement stmt = getStatementFromCache(getDuplicates,
				"SELECT uuid FROM track_duplicates WHERE duplicate_id IN " +
				"(SELECT duplicate_id FROM track_duplicates WHERE uuid = ?)");
		stmt.setString(1, uuid);
		ResultSet results = stmt.executeQuery();
		while (results.next()) {
			duplicateIds.add(results.getString("uuid"));
		}
		results.close();
		stmt.close();
		// the query will include the original uuid, which we don't want
		duplicateIds.remove(uuid);

		return duplicateIds;
	}

	/**
	 * Attempts to place all the given UUIDs in a duplicates group.  Returns
	 * true if this was successful.
	 *
	 * @param tracks collection of duplicate tracks
     * @return true if the operation succeeded
     * @throws java.sql.SQLException if database errors occur
	 */
	private boolean __setDuplicateTracks(Collection<Track> tracks) throws SQLException {
		try {
			Set<String> uuids = new HashSet<>();
			for (Track track : tracks) {
				uuids.add(getTrackUUID(track));
			}

			// first, check for any tracks in this collection that are already
			//        registered in the duplicates table
			Set<Integer> duplicateIds = new HashSet<>();
			{
				PreparedStatement stmt = getStatementFromCache(getDuplicateId,
				"SELECT duplicate_id FROM track_duplicates WHERE uuid = ?");
				for (String uuid : uuids) {
					stmt.setString(1, uuid);
					ResultSet results = stmt.executeQuery();
					if (results.next()) {
						duplicateIds.add(results.getInt("duplicate_id"));
					}
					results.close();
				}
				stmt.close();
			}

			// get the group id, or fail if there are already multiple duplicate
			//     groups in this collection of tracks
			int duplicateGroupId;
			switch (duplicateIds.size()) {
			case 0:
				// need to make a new group
				PreparedStatement stmt = getStatementFromCache(getMaxDuplicateId,
				"SELECT MAX(duplicate_id) AS max FROM track_duplicates");
				ResultSet results = stmt.executeQuery();
				if (results.next()) {
					duplicateGroupId = results.getInt("max") + 1;
				} else {
					// the very first duplicate group id
					duplicateGroupId = 1;
				}
				results.close();
				stmt.close();
				break;
			case 1:
				// use existing group
				duplicateGroupId = duplicateIds.iterator().next();
				break;
			default:
				// some of these song are in existing groups.  cannot proceed
				return false;
			}

			// finally, store the group in the database
			PreparedStatement stmt = getStatementFromCache(setDuplicate,
					"INSERT OR IGNORE INTO track_duplicates (duplicate_id, uuid) VALUES (?, ?)");
			stmt.setInt(1, duplicateGroupId);

			for (String uuid : uuids) {
				stmt.setString(2, uuid);
				stmt.execute();
				log.debug(format("added %s to duplicate group #%s", uuid, duplicateGroupId));
			}
			stmt.close();
			log.debug(format("added %s tracks to duplicate group", uuids.size()));

            for (Track track : tracks) {
                fireTagsChangedEvent(track);
            }

			return true;
		} catch (SQLException e) {
			log.error("SQLException", e);
			return false;
		}
	}

	private Collection<String> __getGroups(String uuid) {
		try {
			PreparedStatement stmt = getStatementFromCache(getGroups,
					"SELECT name FROM groups WHERE group_id IN " +
			"(SELECT group_id FROM track_groups WHERE uuid = ?)");
			stmt.setString(1, uuid);

			List<String> groups = new ArrayList<>();
			ResultSet results = stmt.executeQuery();
			while (results.next()) {
				groups.add(results.getString("name"));
			}
			return groups;
		} catch (SQLException e) {
			log.error("SQLException", e);
			return Collections.emptySet();
		}
	}

	private boolean __setGroup(String name, Collection<Track> tracks) {
		try {
			Set<String> uuids = new HashSet<>();
			for (Track track : tracks) {
				uuids.add(getTrackUUID(track));
			}

			// does a group with name already exist?
			int groupId = -1;
			boolean createNewGroup = false;
			{
				PreparedStatement stmt = getStatementFromCache(getGroupId,
					"SELECT group_id FROM groups WHERE name = ?");
				stmt.setString(1, name);
				ResultSet results = stmt.executeQuery();
				if (results.next()) {
					groupId = results.getInt("group_id");
				}
				results.close();
				stmt.close();

				// doesn't exist, pick the next id
				if (groupId < 0) {
					createNewGroup = true;
					stmt = getStatementFromCache(getGroupId,
						"SELECT MAX(group_id) AS max FROM groups");
					results = stmt.executeQuery();
					if (results.next()) {
						groupId = results.getInt("max") + 1;
					}
					results.close();
					stmt.close();
				}

				// if the table was empty, start from 1
				if (groupId < 0) {
					createNewGroup = true;
					groupId = 1;
				}
			}

			if (createNewGroup) {
				PreparedStatement stmt = getStatementFromCache(setGroup,
						"INSERT OR IGNORE INTO groups (group_id, name) " +
						"VALUES (?, ?)");
				stmt.setInt(1, groupId);
				stmt.setString(2, name);
				stmt.execute();
				stmt.close();
				log.debug(format("created new group \"%s\"", name));
			}

			// finally, store the track/group mapping in the database
			PreparedStatement stmt = getStatementFromCache(setTrackGroup,
					"INSERT OR IGNORE INTO track_groups (group_id, uuid) " +
					"VALUES (?, ?)");
			stmt.setInt(1, groupId);

			for (String uuid : uuids) {
				stmt.setString(2, uuid);
				stmt.execute();
				log.debug(format("added %s to group \"%s\"", uuid, name));
			}
			stmt.close();
			log.debug(format("added %s tracks to group", uuids.size()));
			return true;
		} catch (SQLException e) {
			log.error("SQLException", e);
			return false;
		}
	}

	private void __removeTag(Track track, String tag) {
		try {
			String uuid = getTrackUUID(track);
			int tagId = getTagId(tag);
			PreparedStatement stmt = getStatementFromCache(removeTag,
				"DELETE FROM track_tags WHERE uuid = ? AND tag_id = ?");
			stmt.setString(1, uuid);
			stmt.setInt(2, tagId);
			stmt.execute();

            updateTagCount(tagId);

			fireTagRemovedEvent(track, tag);
		} catch (SQLException e) {
			log.error("SQLException", e);
		}
	}

    private void __removeTag(Collection<Track> tracks, String tag) {
        try {
            int tagId = getTagId(tag);
            PreparedStatement stmt = getStatementFromCache(removeTag,
                "DELETE FROM track_tags WHERE uuid = ? AND tag_id = ?");

            for (Track track : tracks) {
                String uuid = getTrackUUID(track);
                // insert the entry into the track_tags table
                stmt.setString(1, uuid);
                stmt.setInt(2, tagId);
                stmt.addBatch();

                log.debug(String.format("Added [%s] to %s", tag, uuid));
            }

            stmt.executeBatch();
            stmt.close();
            log.debug("Finished updating tracks");

            updateTagCount(tagId);
            log.debug(String.format("Updated tag count for [%s]", tag));

            for (Track track : tracks) {
                fireTagRemovedEvent(track, tag);
            }
        } catch (SQLException e) {
            log.error("SQLException", e);
        }
    }

    private void __deleteTags(Collection<String> tagsToDelete) {
        try {
            PreparedStatement updateTagCountStmt = getStatementFromCache(updateTagCount,
                    "UPDATE tags SET count = (SELECT COUNT(tag_id) FROM track_tags WHERE tag_id = ?) WHERE tag_id = ?");
            PreparedStatement deleteTagStmt = getStatementFromCache(deleteTag,
                "DELETE FROM tags WHERE tag_id == ? AND count == 0");

            for (String tagName : tagsToDelete) {
                int tagId = getTagId(tagName);
                updateTagCountStmt.setInt(1, tagId);
                updateTagCountStmt.setInt(2, tagId);
                deleteTagStmt.setInt(1, tagId);

                updateTagCountStmt.addBatch();
                deleteTagStmt.addBatch();
                log.debug(String.format("Deleting tag %s (%d)", tagName, tagId));
            }

            // first, update all counts
            updateTagCountStmt.executeBatch();
            // then, execute delte
            deleteTagStmt.executeBatch();

            updateTagCountStmt.close();
            deleteTagStmt.close();
        } catch (SQLException e) {
            log.error("SQLException", e);
        }
    }

	private void __shutdown() {
		try {
			log.info("closing database connection");
			conn.close();
			dbAccess.shutdown();
			log.info("closed database connection");
		} catch (SQLException e) {
			// there's not a whole lot of recovery that can be done if we can't close the connection
			log.error("SQLException", e);
		}
	}

	/**
	 * Retrieves the statement by the given name, if it exists.  If it does not,
	 * it creates the statement with the given SQL, caches it, and returns it.
	 *
	 * @param name name of prepared statement
	 * @param sql SQL of statement
	 * @return cached copy of statement.  NOT threadsafe
	 * @throws SQLException
	 */
	private PreparedStatement getStatementFromCache(StatementName name, String sql) throws SQLException {
		PreparedStatement stmt = statementCache.get(name);
		// ok, it seems like sqlite-jdbc doesn't like reusing PreparedStatements
		// TODO find out why
//		if (stmt == null) {
			stmt = conn.prepareStatement(sql);
//			statementCache.put(name, stmt);
//		}
		return stmt;
	}
	/**
	 * Returns all tags for a given uuid.
	 *
	 * @param uuid
	 * @return
	 * @throws SQLException
	 */
	private List<TagCloudEntry> getTags(String uuid) throws SQLException {
		List<TagCloudEntry> ret = new ArrayList<>();

		PreparedStatement stmt = getStatementFromCache(getTags,
				"SELECT name, metadata, count FROM tags_view WHERE uuid = ?");

		stmt.setString(1, uuid);
		ResultSet rs = stmt.executeQuery();

		int maxcount = __getMaxTagCount();
		while (rs.next()) {
			ret.add(new TagCloudEntry(rs.getString("name"), rs.getString("metadata"),
					rs.getInt("count"), maxcount));
		}

		log.debug(uuid + ": " + ret);
		return ret;
	}

	private final static int UUID_CACHE_SIZE = 100;
	private Map<String, String> uuidCache = new LinkedHashMap<>(UUID_CACHE_SIZE);

	/**
	 * Adds value to cache, preventing size from growing too large
	 *
	 * @param location
	 * @param uuid
	 */
	private void addToCache(String location, String uuid) {
		uuidCache.put(location, uuid);
		if (uuidCache.size() > UUID_CACHE_SIZE) {
			Iterator<String> i = uuidCache.keySet().iterator();
			while (uuidCache.size() > UUID_CACHE_SIZE) {
				i.next();
				i.remove();
			}
		}
	}

	/**
	 * Retrieves the UUID for the specified track, or creates a new one if it
	 * does not yet exist in the database.
	 *
	 * @param track
	 * @return
	 * @throws SQLException
	 */
	private String getTrackUUID(Track track) throws SQLException {
        if (track == null) {
            return null;
        }

		String uuid = getUUIDFromFileLocation(track);

        if (uuid == null) {
            uuid = getUUIDFromTrackId(track);
        }

		return uuid;
	}

    private String getUUIDFromTrackId(Track track) throws SQLException {
        String trackId = track.getPersistentId();
        if (trackId == null) {
            String message = "Track did not have a persistent id: " + track;
            log.warn(message);
            throw new IllegalArgumentException(message);
        }

        // first, try to get the UUID from the MRU cache
        String uuid = uuidCache.get(trackId);

        if (uuid == null) {
            // try to load UUID from database
            uuid = getUUIDFromTrackId(trackId);
        }

        // create a new UUID and store in tlhe database.
        if (uuid == null) {
            log.debug(String.format("Creating new UUID for %s", trackId));
            uuid = '{' + UUID.randomUUID().toString().toUpperCase() + '}';
            log.debug("UUID created: " + uuid);
            PreparedStatement stmt = getStatementFromCache(getTrackUUIDInsert,
                    "INSERT INTO track_id_to_guid(uuid, track_id) VALUES(?, ?)");
            stmt.setString(1, uuid);
            stmt.setString(2, trackId);
            stmt.executeUpdate();
            stmt.close();
        }

        addToCache(trackId, uuid);

        return uuid;
    }

    private String getUUIDFromTrackId(String trackId) throws SQLException {
        String uuid = null;
        PreparedStatement stmt = getStatementFromCache(getTrackIdUUIDSelect,
                "SELECT uuid FROM track_id_to_guid WHERE track_id = ?");
        stmt.setString(1, trackId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            uuid = rs.getString(1);
        }
        rs.close();
        stmt.close();

        if (uuid != null) {
            log.debug(format("Found UUID for %s: %s", trackId, uuid));
        }
        return uuid;
    }

    private String getUUIDFromFileLocation(Track track) throws SQLException {
        String uuid = null;
        if (track instanceof FileTrack) {
            FileTrack fileTrack = (FileTrack) track;
            String location;
            try {
                location = fileTrack.getLocation().getCanonicalPath();
            } catch (IOException e) {
                // not a fatal error if we get an exception, but it is unusual
                log.warn("error getting track location", e);
                return null;
            }
            if (location == null) {
                return null;
            }
            // first, try to load the uuid from the MRU cache
            uuid = uuidCache.get(location);

            // uuid was not in recently used cache, try to load from database

            // there are two places where the UUID could be loaded from:
            // 1) if it is a supported format (mp3), it probably has the UUID embedded in the file
            // 2) otherwise, the UUID will be based on the file path
            // #1 is the preferred method

            // try to load UUID from file
            if (uuid == null && track instanceof MP3Track) {
                // if the track has one built in, use that
                UUID trackUUID = ((MP3Track)track).getUUID();
                uuid = String.format("{%s}", trackUUID.toString().toUpperCase());
            }

            // try to load UUID from database
            if (uuid == null) {
                PreparedStatement stmt = getStatementFromCache(getTrackUUIDSelect,
                        "SELECT uuid FROM tracks WHERE location = ?");
                stmt.setString(1, location);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    uuid = rs.getString(1);
                }
                rs.close();
                stmt.close();
            }

            // create a new UUID and store in the database.  if the file was an
            //    mp3 file, the act of retrieving the UUID for the first time would
            //    have created one already
            if (uuid == null) {
                // otherwise, create a new, random one
                log.info(String.format("Creating new UUID for %s", location));
                uuid = '{' + UUID.randomUUID().toString().toUpperCase() + '}';
                log.debug("UUID created: " + uuid);
                PreparedStatement stmt = getStatementFromCache(getTrackUUIDInsert,
                        "INSERT INTO tracks(uuid, location) VALUES(?, ?)");
                stmt.setString(1, uuid);
                stmt.setString(2, location);
                stmt.executeUpdate();
                stmt.close();
            }

            addToCache(location, uuid);
        }
        return uuid;
    }

    /**
	 * Fires tag change events on all registered listeners.  This will occur
	 * in another thread, but the same thread will reused, so all events will
	 * be received in the same sequence they are processed by this tagModel.
     * @param track track whose tags changed
     * @param tag new tag
	 */
	protected void fireTagAddedEvent(final Track track, final String tag) {
		tagChangeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (TagChangeListener l : tagChangeListeners) {
                    l.tagAdded(track, tag);
                }
            }
        });
	}

	/**
	 * Fires tag change events on all registered listeners.  This will occur
	 * in another thread, but the same thread will reused, so all events will
	 * be received in the same sequence they are processed by this tagModel.
     * @param track track whose tags changed
     * @param tag removed tag
	 */
	protected void fireTagRemovedEvent(final Track track, final String tag) {
		tagChangeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (TagChangeListener l : tagChangeListeners) {
                    l.tagRemoved(track, tag);
                }
            }
        });
	}

    /**
     * Fires tag change events on all registered listeners.  This will occur
     * in another thread, but the same thread will reused, so all events will
     * be received in the same sequence they are processed by this tagModel.
     * @param track track whose tags changed
     */
    protected void fireTagsChangedEvent(final Track track) {
        tagChangeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (TagChangeListener l : tagChangeListeners) {
                    l.tagsChanged(track);
                }
            }});
    }

	/**
	 * Adds the <code>tag</code> (and metadata) to <code>track</code>.
	 *
	 * @param track
	 * @param tag
	 * @param metadata
	 */
	public void addTag(final Track track, final String tag, final String metadata) {
		dbAccess.execute(new Runnable() {
			@Override
			public void run() {
				__addTag(track, tag, metadata);
			}});
	}

	/**
	 * Adds the <code>tag</code> (and metadata) to each track in <code>tracks</code>.
	 *
	 * @param tracks
	 * @param tag
	 * @param metadata
	 */
	public void addTag(final Collection<Track> tracks, final String tag, final String metadata) {
		dbAccess.execute(new Runnable() {
			@Override
			public void run() {
				__addTag(tracks, tag, metadata);
			}});
	}

    /**
     * Adds the <code>tag</code> (and metadata) to each track in <code>tracks</code>.
     * <p>
     * This method does not return until all the tags have been added.
     *
     * @param tracks
     * @param tag
     * @param metadata
     */
    public void addTagAndWait(final Collection<Track> tracks, final String tag, final String metadata) throws ExecutionException, InterruptedException {
        dbAccess.submit(new Runnable() {
            @Override
            public void run() {
                __addTag(tracks, tag, metadata);
            }}).get();
    }

	/**
	 * Returns all tags in the database that have at least the minimum number of entries.
	 *
	 * @param minimum limiting parameter to ignore tags that appear very infrequently.
	 * @return
	 */
	public Collection<TagCloudEntry> getAllTags(final int minimum) {
		Callable<Collection<TagCloudEntry>> getTags = new Callable<Collection<TagCloudEntry>>() {
			@Override
			public Collection<TagCloudEntry> call() throws Exception {
				return __getAllTags(minimum);
			}};
		Collection<TagCloudEntry> tags = new ArrayList<TagCloudEntry>();

		try {
			tags = dbAccess.submit(getTags).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("getAllTags", e);
		}
        return tags;
	}

	/**
	 * Returns the number of entries that are tagged with the most popular tag.
	 *
	 * @return maximum value that would be returned by getTagCounts
	 */
	public int getMaxTagCount() {

		Callable<Integer> getTags = new Callable<Integer>() {
			@Override
			public Integer call() throws Exception {
				return __getMaxTagCount();
			}};
		int max = 0;

		try {
			max = dbAccess.submit(getTags).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("getAllTags", e);
		}
        return max;
	}

	/**
	 * Returns the tag entries for the input track.
	 *
	 * @param track
	 * @return
	 */
	public List<TagCloudEntry> getTags(final Track track) {
		Callable<List<TagCloudEntry>> getTags = new Callable<List<TagCloudEntry>>() {
			@Override
			public List<TagCloudEntry> call() throws Exception {
                String uuid = getTrackUUID(track);
				return __getTags(uuid);
			}};
		List<TagCloudEntry> tags = new ArrayList<>();

		try {
			tags = dbAccess.submit(getTags).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error(track.getTitle(), e);
		}
        return tags;
	}

    /**
     * Gets the tags for all the input tracks.
     *
     * @param tracks a collection of tracks to retrieve tags for
     * @return all tags found for the selected tracks
     */
    public Map<Track, List<TagCloudEntry>> getTags(final Collection<Track> tracks) {
        Callable<Map<Track, List<TagCloudEntry>>> getTags = new Callable<Map<Track, List<TagCloudEntry>>>() {
            @Override
            public Map<Track, List<TagCloudEntry>> call() throws Exception {
                return __getTags(tracks);
            }};
        Map<Track, List<TagCloudEntry>> tags = new HashMap<>();

        try {
            tags = dbAccess.submit(getTags).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting tags for multiple tracks", e);
        }
        return tags;
    }

    /**
     * Gets the tags for all the input tracks.
     *
     * @param tracks a collection of tracks to retrieve tags for
     * @return all tags found for the selected tracks
     */
    public Map<String, List<TagCloudEntry>> getTagsById(final Collection<String> tracks) {
        Callable<Map<String, List<TagCloudEntry>>> getTags = new Callable<Map<String, List<TagCloudEntry>>>() {
            @Override
            public Map<String, List<TagCloudEntry>> call() throws Exception {
                return __getTagsById(tracks);
            }};
        Map<String, List<TagCloudEntry>> tags = new HashMap<>();

        try {
            tags = dbAccess.submit(getTags).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting tags for multiple tracks", e);
        }
        return tags;
    }

	/**
	 * Removes the specified tag from <code>track</code>.
	 *
	 * @param track
	 * @param tag
	 */
	public void removeTag(final Track track, final String tag) {
		dbAccess.execute(new Runnable() {
            @Override
            public void run() {
                __removeTag(track, tag);
            }
        });
	}

    /**
     * Removes the specified tag from all <code>tracks</code>
     * and does not return until the operation is complete.
     *
     * @param tracks
     * @param tag
     * @throws InterruptedException if an exception occurs executing this command
     * @throws java.util.concurrent.ExecutionException if an exception occurs executing this command
     */
    public void removeTagsAndWait(final Collection<Track> tracks, final String tag) throws ExecutionException, InterruptedException {
        dbAccess.submit(new Runnable() {
            @Override
            public void run() {
                __removeTag(tracks, tag);
            }}).get();
    }

	/**
	 * Attempts to place all the given UUIDs in a duplicates group.  Returns
	 * true if this was successful.
	 *
	 * @param tracks tracks to set as duplicates of each other
	 * @return true if the operation succeeded
	 */
	public boolean setDuplicates(final Collection<Track> tracks) {
		try {
			return dbAccess.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return __setDuplicateTracks(tracks);
				}}).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Cannot set duplicates: " + tracks, e);
			return false;
		}
    }

	/**
	 * Places all the tracks in a group with the given name.  Returns true
	 * if this was successful.  It will fail if the name is not unique.
	 *
	 * @param name
	 * @param tracks
	 * @return true if operation succeeded
	 */
	public boolean setGroup(final String name, final Collection<Track> tracks) {
		try {
			return dbAccess.submit(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return __setGroup(name, tracks);
				}}).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Cannot set group: " + tracks, e);
			return false;
		}
    }

	/**
	 * Returns all the groups that the specified track belongs to.
	 *
	 * @param track
	 * @return group names this track belongs to
	 */
	public Collection<String> getGroups(final Track track) {
		try {
			return dbAccess.submit(new Callable<Collection<String>>() {
				@Override
				public Collection<String> call() throws Exception {
                    String uuid = getTrackUUID(track);
					return __getGroups(uuid);
				}}).get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Cannot get groups: " + track, e);
		}
        return Collections.emptySet();
	}

    /**
     * Deletes the specified tags the database.  It is an error to call pass in tags that
     * are referenced by tracks.
     *
     * @param tagsToDelete delete all these tags
     */
    public void deleteTags(final List<String> tagsToDelete) {
        try {
            dbAccess.submit(new Runnable() {
                @Override
                public void run() {
                    __deleteTags(tagsToDelete);
                }}).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting tags", e);
        }
    }

    /**
     * Deletes the specified tracks from the database.  Also deletes references to tags and
     * updates the counts of affected tags.
     *
     * @param tracksToDelete delete these track ids
     */
    public void deleteTracks(final List<String> tracksToDelete) {
        try {
            dbAccess.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        conn.setAutoCommit(false);

                        Set<Integer> affectedTags = new HashSet<>();
                        PreparedStatement deleteTrackTagsStmt = getStatementFromCache(deleteTrackTags,
                                "DELETE FROM track_tags WHERE uuid = ?");
                        PreparedStatement deleteTrackIdToUuidStmt = getStatementFromCache(deleteTrackIdToUUID,
                                "DELETE FROM track_id_to_guid WHERE uuid = ?");
                        PreparedStatement deleteDuplicateStmt = conn.prepareStatement(
                                "DELETE FROM track_duplicates WHERE uuid = ?");
                        PreparedStatement deleteGroupStmt = conn.prepareStatement(
                                "DELETE FROM track_groups WHERE uuid = ?");

                        PreparedStatement getTagIds = conn.prepareStatement(
                                "SELECT tag_id FROM track_tags WHERE uuid = ?");

                        for (String trackId : tracksToDelete) {
                            String uuid = getUUIDFromTrackId(trackId);
                            // delete from track_tags
                            deleteTrackTagsStmt.setString(1, uuid);
                            deleteTrackTagsStmt.addBatch();
                            // delete from track_id_to_guid
                            deleteTrackIdToUuidStmt.setString(1, uuid);
                            deleteTrackIdToUuidStmt.addBatch();
                            // delete from track_duplicates
                            deleteDuplicateStmt.setString(1, uuid);
                            deleteDuplicateStmt.addBatch();
                            // delete from track_groups
                            deleteGroupStmt.setString(1, uuid);
                            deleteGroupStmt.addBatch();

                            // find tags this track used to have
                            getTagIds.setString(1, uuid);
                            ResultSet rs = getTagIds.executeQuery();
                            while (rs.next()) {
                                affectedTags.add(rs.getInt("tag_id"));
                            }
                        }

                        deleteTrackTagsStmt.executeBatch();
                        deleteTrackTagsStmt.close();

                        deleteTrackIdToUuidStmt.executeBatch();
                        deleteTrackIdToUuidStmt.close();

                        deleteDuplicateStmt.executeBatch();
                        deleteDuplicateStmt.close();

                        deleteGroupStmt.executeBatch();
                        deleteGroupStmt.close();

                        PreparedStatement updateTagCountStmt = getStatementFromCache(updateTagCount,
                                "UPDATE tags SET count = (SELECT COUNT(tag_id) FROM track_tags WHERE tag_id = ?) WHERE tag_id = ?");
                        for (int tag_id : affectedTags) {
                            updateTagCountStmt.setInt(1, tag_id);
                            updateTagCountStmt.addBatch();
                        }
                        updateTagCountStmt.executeBatch();
                        updateTagCountStmt.close();

                        // commit all this work
                        conn.commit();

                    } catch (SQLException e) {
                        log.error("Error deleting tracks", e);
                    } finally {
                        try {
                            conn.setAutoCommit(true);
                        } catch (SQLException e) {
                            log.error("Error re-enabling autocommit mode", e);
                        }
                    }
                }}).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error deleting tracks", e);
        }
    }

    /**
     * Returns all track ids for which there are tags in the system.
     *
     * @return
     */
    public List<String> getAllTrackIds() {
        Callable<List<String>> getTags = new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT track_id FROM track_id_to_guid");
                ArrayList<String> ret = new ArrayList<>();
                while (rs.next()) {
                    ret.add(rs.getString("track_id"));
                }
                rs.close();
                stmt.close();
                return ret;
            }};
        List<String> tags = new ArrayList<>();

        try {
            tags = dbAccess.submit(getTags).get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error getting full list of track ids", e);
        }
        return tags;
    }

	/**
	 * Shuts down the database connection cleanly.
	 */
	public void shutdown() {
		dbAccess.execute(new Runnable() {
			@Override
			public void run() {
				__shutdown();
			}});
	}

    static enum StatementName {
		getTags,
		getTrackUUIDSelect, getTrackUUIDInsert,
        getTrackIdUUIDSelect,
		addTag, addTrackTag, removeTag,
		getTagCounts, getMaxTagCount, updateTagCount,
		getTagId, getDuplicates, getDuplicateId,
		getMaxDuplicateId, setDuplicate,
		getGroups, setGroup, setTrackGroup, getGroupId,
        deleteTag, deleteTrackTags, deleteTrackIdToUUID,
	}
}
