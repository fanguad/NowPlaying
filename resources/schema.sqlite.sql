-- -----------------------------------------------------
-- Table tracks
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS tracks ( uuid TEXT NOT NULL , location TEXT NOT NULL , PRIMARY KEY (uuid) );

-- -----------------------------------------------------
-- Table tags
--     in SQLite, tag_id is effectively autoincrement
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS tags ( tag_id INTEGER PRIMARY KEY, name TEXT UNIQUE NOT NULL , metadata TEXT NULL, count INT ZEROFILL NOT NULL );

-- -----------------------------------------------------
-- Table track_tags
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS track_tags ( uuid TEXT NOT NULL , tag_id INT NOT NULL , PRIMARY KEY (uuid, tag_id) CONSTRAINT track_tags_uuid FOREIGN KEY (uuid ) REFERENCES tracks (uuid ) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT track_tags_id FOREIGN KEY (tag_id ) REFERENCES tags (tag_id ) ON DELETE CASCADE ON UPDATE CASCADE);

-- -----------------------------------------------------
-- Table groups
--     in SQLite, group_id is effectively autoincrement
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS groups ( group_id INTEGER PRIMARY KEY, name TEXT NOT NULL );

-- -----------------------------------------------------
-- Table track_groups
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS track_groups ( uuid TEXT NOT NULL , group_id INT NOT NULL , PRIMARY KEY (uuid) , CONSTRAINT track_group_id FOREIGN KEY (group_id ) REFERENCES groups (group_id ) ON DELETE CASCADE ON UPDATE CASCADE, CONSTRAINT track_group_uuid FOREIGN KEY (uuid ) REFERENCES tracks (uuid ) ON DELETE CASCADE ON UPDATE CASCADE);

-- -----------------------------------------------------
-- Table track_duplicates
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS track_duplicates ( uuid TEXT NOT NULL , duplicate_id INT NOT NULL , PRIMARY KEY (uuid) , CONSTRAINT track_duplicates_uuid FOREIGN KEY (uuid ) REFERENCES tracks (uuid ) ON DELETE CASCADE ON UPDATE CASCADE);

-- the following were created by hand, so it doesn't have constraints
CREATE VIEW tags_view AS SELECT uuid, name, metadata, count FROM track_tags JOIN tags ON track_tags.tag_id = tags.tag_id

CREATE TABLE IF NOT EXISTS track_id_to_guid ( uuid TEXT NOT NULL , track_id TEXT NOT NULL , PRIMARY KEY (uuid, track_id));
