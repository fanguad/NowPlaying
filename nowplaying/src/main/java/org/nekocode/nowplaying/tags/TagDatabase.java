/*
 * Copyright (c) 2011-2024. Dan Clark
 */

package org.nekocode.nowplaying.tags;

import lombok.extern.log4j.Log4j2;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteJDBCLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Tools for interacting with the Tag Database.  This class only provides generic database operations,
 * not those specific to NowPlaying (that is the TagModel's job).
 *
 * @author fanguad
 */
@Log4j2
public class TagDatabase {
    private final Connection conn;
    private final Map<Object, PreparedStatement> preparedStatements;

    /**
     * Opens the tag database, creating a new one if necessary.
     *
     * @param database database file name
     * @param schemaLocation location of schema to use to create database
     * @throws SQLException if errors occur opening the database connection
     * @throws ClassNotFoundException if errors occur loading the database drivers
     * @throws java.io.IOException if errors occur loading the database schema
     */
    public TagDatabase(File database, URL schemaLocation) throws Exception {
        // if the tag database does not exist, need to create it
        boolean initializeDatabase = !database.exists();

        Class.forName("org.sqlite.JDBC");
        log.info("opening database connection");

        SQLiteConfig config = new SQLiteConfig();
        config.setSharedCache(true);
        config.enableRecursiveTriggers(true);
        config.enforceForeignKeys(true);

        conn = DriverManager.getConnection("jdbc:sqlite:" + database.getPath(), config.toProperties());
        preparedStatements = new HashMap<>();

        log.info(String.format("sqlite-jdbc running in %s mode", SQLiteJDBCLoader.isNativeMode() ? "native" : "pure-java"));

        if (initializeDatabase) {
            initializeDatabase(schemaLocation);
        }
    }

    /**
     * Initializes the database from the given schema file.  The schema expects commands to be one line,
     * with one command per line.  Empty lines and comments (--) are allowed.
     *
     * @param schemaLocation location of database schema
     * @throws SQLException if an error occurs creating the database
     * @throws IOException if an error occurs loading the schema
     */
    private void initializeDatabase(URL schemaLocation) throws SQLException, IOException {
        Statement stmt = conn.createStatement();

        BufferedReader in = new BufferedReader(new InputStreamReader(schemaLocation.openStream()));

        try {
            conn.setAutoCommit(false);
            String line;
            while ((line = in.readLine()) != null) {
                // chop off any line comments
                line = line.replaceFirst("--.*", "");

                if (line.matches("^\\s*$")) {
                    // blank lines and comments
                    continue;
                }
                // TODO strip trailing semi-colon

                log.info("Initialing Database: " + line);
                stmt.execute(line);
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }

        stmt.close();
    }

    /**
     * Registers a particular string for use as a prepared statement.
     *
     * @param identifier identifier for this statement
     * @param preparedStatement properly formatted statement
     * @throws SQLException if an error occurs preparing the statement
     */
    public void registerPreparedStatement(Object identifier, String preparedStatement) throws SQLException {
        preparedStatements.put(identifier, conn.prepareStatement(preparedStatement));
    }

    /**
     * Retrieve a previously registered prepared statement
     *
     * @param identifier identifier for this statement
     * @return previously registered statement
     * @throws SQLException if an error occurs preparing the statement
     */
    public PreparedStatement getPreparedStatement(Object identifier) throws SQLException {
        return preparedStatements.get(identifier);
    }

    /**
     * Shuts down the database.
     *
     * @throws SQLException if an error occurs closing the connection
     */
    public void shutdown() throws SQLException {
        for (PreparedStatement stmt : preparedStatements.values()) {
            stmt.close();
        }
        preparedStatements.clear();
        conn.close();
    }

    /**
     * Starts a transaction.
     *
     * @throws SQLException if an error occurs
     */
    public void beginTransaction() throws SQLException {
        conn.setAutoCommit(false);
    }

    /**
     * Ends a transaction.
     *
     * @throws SQLException if an error occurs
     */
    public void endTransaction() throws SQLException {
        try {
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            // even if the commit fails, still try to turn auto-commit back on
            conn.setAutoCommit(true);
        }
    }
}
