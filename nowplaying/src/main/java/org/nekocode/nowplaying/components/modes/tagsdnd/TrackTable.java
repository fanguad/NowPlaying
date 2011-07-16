/*
 * Copyright (c) 2011, fanguad@nekocode.org
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.nekocode.nowplaying.resources.images.Icons;

import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Component;

import static java.lang.Math.max;

/**
 * A subclass of JTable for displaying track information.
 */
public class TrackTable extends JTable {

    /**
     * Setting it to exactly the preferred width still leaves ellipis at the end, so add this buffer
     */
    private static final int BUFFER = 4;
    private static final int MIN_COLUMN_WIDTH = 150;

    /**
     * Constructor.  This table can only be used with a TrackTableModel.
     *
     * @param model track model
     */
    public TrackTable(TrackTableModel model) {
        super(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);


        /*
         * the first column indicates whether the tracks exists in the media player
         * show a red or green icon to indicate this, rather than text
         */
        {
            TableColumn foundInMediaPlayerColumn = getColumnModel().getColumn(TrackTableModel.Columns.FoundInMediaPlayer.ordinal());
            foundInMediaPlayerColumn.setCellRenderer(new RedGreenRenderer());
            foundInMediaPlayerColumn.setMaxWidth(RedGreenRenderer.ICON_WIDTH);
        }

        TableCellRenderer resizeRenderer = new ResizeRenderer();
        TableCellRenderer headerRenderer = getTableHeader().getDefaultRenderer();
//        DefaultTableCellHeaderRenderer headerRenderer = new DefaultTableCellHeaderRenderer();
        for (TrackTableModel.Columns columnName : TrackTableModel.Columns.values()) {
            switch (columnName) {
                case FoundInMediaPlayer:
                    // don't resize columns in this case
                    break;
                default:
                    getColumnModel().getColumn(columnName.ordinal()).setHeaderRenderer(headerRenderer);
                case Track:
                    // fall through here - Track won't get a header renderer
                    getColumnModel().getColumn(columnName.ordinal()).setCellRenderer(resizeRenderer);
                    break;
            }
        }

        // set columns to default width
        shrinkColumns();
    }

    /**
     * Deletes all rows which are currently selected.
     */
    public void deleteSelectedRows() {
        getModel().deleteRows(getSelectedRows());
        setPreferredWith();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Covariant return type because this class can only accept {@link TrackTableModel}s.
     */
    @Override
    public TrackTableModel getModel() {
        return (TrackTableModel) super.getModel();
    }

    /**
     * Set all columns to their default widths.
     */
    public void shrinkColumns() {
        for (TrackTableModel.Columns columnName : TrackTableModel.Columns.values()) {
            switch (columnName) {
                case FoundInMediaPlayer:
                case Track:
                    // don't do anything for these columns
                    break;
                default:
                    // shrink the rest back to their default sizes
                    TableColumn column = getColumnModel().getColumn(columnName.ordinal());
                    column.setPreferredWidth(MIN_COLUMN_WIDTH);
            }
        }
    }

    /**
     * Set all columns to their preferred widths, based on their contents.
     */
    public void setPreferredWith() {
        for (TrackTableModel.Columns columnName : TrackTableModel.Columns.values()) {
            switch (columnName) {
                case FoundInMediaPlayer:
                case Track:
                    // don't do anything for these columns
                    break;
                default:
                    // shrink the rest back to their default sizes
                    TableColumn column = getColumnModel().getColumn(columnName.ordinal());
                    int width = MIN_COLUMN_WIDTH;
                    for (int i = 0; i < getRowCount(); i++) {
                        Component component = column.getCellRenderer().getTableCellRendererComponent(
                                this, getValueAt(i, columnName.ordinal()),
                                false, false, i, columnName.ordinal());
                        width = max(width, component.getPreferredSize().width + BUFFER);
                    }
                    column.setPreferredWidth(width);
            }
        }
    }

    private static class ResizeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(final JTable table, Object value, boolean isSelected, boolean hasFocus, int row, final int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            final int width = component.getPreferredSize().width;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    int preferred = table.getColumnModel().getColumn(column).getPreferredWidth();
                    if (width > preferred) {
                        table.getColumnModel().getColumn(column).setPreferredWidth(width + BUFFER);
                    }
                }
            });
            return component;
        }
    }

    private static class RedGreenRenderer extends DefaultTableCellRenderer {
        private static final ImageIcon redButton;
        private static final ImageIcon greenButton;
        private static final int ICON_WIDTH;
        static {
            redButton = new ImageIcon(Icons.class.getResource("red_button_14.png"));
            greenButton = new ImageIcon(Icons.class.getResource("green_button_14.png"));
            ICON_WIDTH = Math.max(redButton.getIconWidth(), greenButton.getIconWidth()) + 4;
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table, Object value, boolean isSelected, boolean hasFocus, int row, final int column) {
            Component component = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            if ((Boolean) value) {
                setIcon(greenButton);
            } else {
                setIcon(redButton);
            }
            return component;
        }
    }
}
