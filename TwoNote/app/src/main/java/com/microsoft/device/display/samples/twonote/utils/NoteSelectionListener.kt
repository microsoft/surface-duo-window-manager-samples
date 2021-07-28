/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.utils

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import com.microsoft.device.display.samples.twonote.R
import com.microsoft.device.display.samples.twonote.fragments.NoteListFragment
import com.microsoft.device.display.samples.twonote.models.INode

/**
 * Class that handles multiple choice mode for the list of notes
 *
 * @param host: NoteListFragment that contains the list of notes
 * @param listView: ListView object that displays the list of notes
 * @param arrayAdapter: ArrayAdapter connected to the ListView object
 */
class NoteSelectionListener(
    private var host: NoteListFragment,
    private var listView: ListView,
    private var arrayAdapter: ArrayAdapter<INode>
) : AbsListView.MultiChoiceModeListener {

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                // Delete all selected notes
                val list = listView.checkedItemPositions
                for (i in arrayAdapter.count - 1 downTo 0) {
                    if (list.get(i)) {
                        arrayAdapter.getItem(i)?.let { inode ->
                            host.context?.let { cntx ->
                                FileSystem.delete(cntx, DataProvider.getActiveSubDirectory(), inode)
                            }
                        }
                    }
                }
                arrayAdapter.notifyDataSetChanged()
                host.exitDetailFragment(true)
                onDestroyActionMode(mode)
            }
        }
        return true
    }

    override fun onItemCheckedStateChanged(mode: ActionMode, pos: Int, id: Long, checked: Boolean) {
        updateTitle(mode)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.menu_note_selection, menu)
        updateTitle(mode)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        listView.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    /**
     * Update ActionMode toolbar title based on how many items are selected
     *
     * @param mode: ActionMode toolbar to update
     */
    private fun updateTitle(mode: ActionMode) {
        mode.title = "${listView.checkedItemCount} ${host.getString(R.string.selected)}"
    }
}