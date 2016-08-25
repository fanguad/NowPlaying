/*
 * Copyright (c) 2011, dan.clark@nekocode.org
 *
 * Licensed under FreeBSD license.  See README for details.
 */

package org.nekocode.itunes.remote.connection;

import java.util.Objects;

/**
 * Calculates the difference between two iTunes Remote responses.
 * New or updated values will be retained in this object.
 * Deleted values will not be tracked (possible future work, if necessary).
 *
 * @author fanguad
 */
public class ITunesRemoteResponseDiff extends ITunesRemoteResponse {
    public ITunesRemoteResponseDiff(ITunesRemoteResponse original, ITunesRemoteResponse update) {
        storeDiff(this, original, update);
    }

    private void storeDiff(ITunesRemoteResponse storage,
                           ITunesRemoteResponse original,
                           ITunesRemoteResponse update) {

        for (ContentCode leafId : update.getLeaves()) {
            Object originalLeaf = original.getLeaf(leafId);
            Object updateLeaf = update.getLeaf(leafId);
            if (!Objects.deepEquals(originalLeaf, updateLeaf)) {
                storage.addChildObject(leafId, updateLeaf);
            }
        }

        for (ContentCode branchId : update.getBranches()) {
            ITunesRemoteResponse originalBranch = original.getBranch(branchId);
            ITunesRemoteResponse updateBranch = update.getBranch(branchId);
            ITunesRemoteResponse storageBranch = new ITunesRemoteResponse();
            storeDiff(storageBranch, originalBranch, updateBranch);

            // only add the storageBranch if it isn't empty
            if (!storageBranch.isEmpty()) {
                storage.addChild(branchId, storageBranch);
            }
        }

        // NOTE: always assume that any multi-branches have changed
        // reasoning: not sure I need it, and it's not trivial to find matches

        // possible solution: iterator over update, see if there are any
        //        exact matches, otherwise the whole thing is different
    }
}
