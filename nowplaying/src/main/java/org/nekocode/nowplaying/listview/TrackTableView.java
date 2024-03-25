/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

/*
 * Filename:   TrackTableView.java
 * Created On: Jun 20, 2008
 */
package org.nekocode.nowplaying.listview;

import lombok.extern.log4j.Log4j2;
import org.jdesktop.swingx.JXTable;
import org.nekocode.nowplaying.objects.Track;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of tracks in tabular format.
 *
 * @author dan.clark@nekocode.org
 */
@Log4j2
public class TrackTableView extends JXTable {
	private final TrackTableModel model;

	// for now, all columns are always shown
	enum Columns {
		Title, Artist, Album, Track
	}

	public TrackTableView() {
		this.model = new TrackTableModel();

		setModel(model);
	}

	public void setTracks(List<Track> tracks) {
		model.setValues(tracks);
	}

	/**
	 * A TableModel for holding Tracks.
	 */
	private static class TrackTableModel extends AbstractTableModel {

		List<Track> tracks = new ArrayList<>();

		public void setValues(List<Track> newTracks) {
			tracks.clear();
			tracks.addAll(newTracks);
			fireTableDataChanged();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
		 */
		@Override
		public String getColumnName(int column) {
			return Columns.values()[column].name();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		@Override
		public int getColumnCount() {
			return Columns.values().length;
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		@Override
		public int getRowCount() {
			return tracks.size();
		}

		/* (non-Javadoc)
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Track track = tracks.get(rowIndex);
			String value = "--INVALID--";
			switch (Columns.values()[columnIndex]) {
			case Album:
				value = track.getAlbum();
				break;
			case Artist:
				value = track.getArtist();
				break;
			case Title:
				value = track.getTitle();
				break;
			case Track:
				int number = track.getTrackNumber();
				int max = track.getTrackCount();
				if (number > 0){
					if (max > 0) {
						value = String.format("%d/%d", number, max);
					} else {
						value = String.format("%d", number);
					}
				} else {
					value = "";
				}

				break;
			}
			return value;
		}
	}
}
