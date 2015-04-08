/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.nowplaying.components.modes.tagsdnd;

import org.divxdede.swing.busy.BusyModel;
import org.divxdede.swing.busy.JBusyComponent;
import org.divxdede.swing.busy.ui.BasicBusyLayerUI;
import org.nekocode.nowplaying.components.icons.SpinningDialBusyIcon;
import org.nekocode.nowplaying.internals.NamedThreadFactory;
import org.nekocode.nowplaying.tags.TagModel;
import org.nekocode.nowplaying.tags.cloud.TagCloudEntry;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This panel provides utilities to recalculate the tag counts for all tags,
 * to find tags with a tag count of zero, and to delete tags.
 *
 * @author fanguad
 */
public class FindUnusedTags extends JBusyComponent<JPanel> {
    
    private ExecutorService workerThread = Executors.newSingleThreadExecutor(new NamedThreadFactory("FindUnusedTags", false));
    private BusyModel busyModel;
    private TagModel tagModel;
    private DefaultTableModel tableModel;
    private JTable tagTable;

    public FindUnusedTags(TagModel tagModel) {
        this.tagModel = tagModel;

        JPanel view = new JPanel(new BorderLayout());
        setView(view);
        BasicBusyLayerUI busyLayerUI = new BasicBusyLayerUI();
        busyLayerUI.setBusyIcon(new SpinningDialBusyIcon(64, 64));
//        BusyLayerUI busyLayerUI = new BasicBusyLayerUI(0, 0.85f, Color.WHITE);
        setBusyLayerUI(busyLayerUI);

        // use the BusyModel to control the busy state of this component
        busyModel = getBusyModel();
        busyModel.setCancellable(false);
        busyModel.setDeterminate(false);

        JButton search = new JButton("Find Unused Tags");
        JButton delete = new JButton("Delete Selected Tags");
        JPanel topRow = new JPanel();
        topRow.add(search);
        topRow.add(delete);

        tableModel = new DefaultTableModel();
        tableModel.addColumn("Tag Name");
        tagTable = new JTable(tableModel);
        JScrollPane scrollpane = new JScrollPane(tagTable);
        scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        view.add(topRow, BorderLayout.PAGE_START);
        view.add(scrollpane, BorderLayout.CENTER);

        final Runnable findUnusedTags = new FindUnusedTagsAction();
        search.addActionListener(e -> workerThread.execute(findUnusedTags));

        final Runnable deleteTags = new DeleteTagsAction();
        delete.addActionListener(e -> workerThread.execute(deleteTags));
    }

    private void getUnusedTags() {
        Collection<TagCloudEntry> allTags = tagModel.getAllTags(0);
        // making this a List because the final result will be sorted
        List<String> unusedTags = allTags.stream().
                filter(entry -> entry.getCount() < 1).
                map(TagCloudEntry::getTag).
                sorted().
                collect(Collectors.toList());

        Object[][] dataVector = new Object[unusedTags.size()][];
        for (int i = 0; i < unusedTags.size(); i++) {
            dataVector[i] = new Object[] {unusedTags.get(i)};
        }

        tableModel.setDataVector(dataVector, new Object[] {"Tag Name"});
    }

    public void shutdown()
    {
        workerThread.shutdown();
    }

    private class FindUnusedTagsAction implements Runnable {
        @Override
        public void run() {
            busyModel.setBusy(true);

            getUnusedTags();

            busyModel.setBusy(false);
        }
    }

    private class DeleteTagsAction implements Runnable {
        @Override
        public void run() {
            busyModel.setBusy(true);

            List<String> tagsToDelete = new ArrayList<>();
            for (int row : tagTable.getSelectedRows()) {
                tagsToDelete.add((String) tagTable.getValueAt(row, 0));
            }

            tagModel.deleteTags(tagsToDelete);

            getUnusedTags();

            busyModel.setBusy(false);
        }
    }
}
