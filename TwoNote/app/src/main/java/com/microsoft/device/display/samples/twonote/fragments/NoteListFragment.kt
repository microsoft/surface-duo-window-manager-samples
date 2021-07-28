/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.fragments

import Defines.LIST_VIEW
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.microsoft.device.display.samples.twonote.MainActivity
import com.microsoft.device.display.samples.twonote.R
import com.microsoft.device.display.samples.twonote.models.DirEntry
import com.microsoft.device.display.samples.twonote.models.INode
import com.microsoft.device.display.samples.twonote.models.Note
import com.microsoft.device.display.samples.twonote.utils.DataProvider
import com.microsoft.device.display.samples.twonote.utils.FileSystem
import com.microsoft.device.display.samples.twonote.utils.NoteSelectionListener
import com.microsoft.device.display.samples.twonote.utils.buildDetailTag
import com.microsoft.device.dualscreen.ScreenInfoProvider

/**
 * Fragment that shows a list view of the user's notes and lets the user add, delete, and rename
 * categories, as well as create and delete notes
 */
class NoteListFragment : Fragment(), AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, AdapterView.OnItemSelectedListener {
    // Lists and list adapters
    private var categoriesListAdapter: ArrayAdapter<INode>? = null
    private var notesListAdapter: ArrayAdapter<INode>? = null
    private lateinit var inodes: MutableList<INode>
    private lateinit var categories: MutableList<INode>

    // Fragment view elements
    private var listView: ListView? = null
    private var categoryView: Spinner? = null
    private lateinit var editText: TextInputEditText

    private val root = ""
    private var selectedFlag = false
    private var noteSelectionListener: NoteSelectionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load note and category list data
        inodes = DataProvider.getINodes()
        categories = DataProvider.getCategories()
        FileSystem.loadCategories(requireContext(), root)

        activity?.let {
            // Override getView function so that note list displays both note title and date modified
            notesListAdapter = object : ArrayAdapter<INode>(it, android.R.layout.simple_list_item_2, android.R.id.text1, inodes) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)

                    val text1 = view.findViewById<TextView>(android.R.id.text1)
                    text1.text = inodes[position].title
                    text1.setTypeface(null, Typeface.BOLD)

                    val text2 = view.findViewById<View>(android.R.id.text2) as TextView
                    text2.text = inodes[position].dateModifiedString()
                    text2.setTextColor(it.getColor(R.color.colorOnBackgroundVariant))

                    return view
                }
            }
            categoriesListAdapter = object : ArrayAdapter<INode>(it, android.R.layout.simple_spinner_item, android.R.id.text1, categories) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)

                    if (categories.isNotEmpty()) {
                        val text1 = view.findViewById<TextView>(android.R.id.text1)
                        text1.text = ""
                    }

                    return view
                }
            }
            categoriesListAdapter?.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelable(LIST_VIEW, listView?.onSaveInstanceState())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_note_list, container, false)

        editText = view.findViewById(R.id.title_list_input)
        editText.setText(DataProvider.getActiveCategoryName())
        setOnChangeListenerForTextInput(editText)

        listView = view.findViewById(R.id.list_view)
        listView?.let {
            it.adapter = notesListAdapter
            it.onItemClickListener = this
            it.onItemLongClickListener = this
            noteSelectionListener = NoteSelectionListener(this, it, notesListAdapter!!)
            it.setMultiChoiceModeListener(noteSelectionListener)
            it.choiceMode = ListView.CHOICE_MODE_SINGLE

            if (savedInstanceState != null)
                listView?.onRestoreInstanceState(savedInstanceState.getParcelable(LIST_VIEW))
        }

        categoryView = view.findViewById(R.id.dropdown_spinner)
        categoryView?.let {
            it.adapter = categoriesListAdapter
            it.onItemSelectedListener = this
        }

        view.findViewById<FloatingActionButton>(R.id.add_fab).setOnClickListener {
            // Open newly created note (first element in list)
            FileSystem.addInode(requireContext())
            updateNotesList()
            startNoteFragment(0)
        }

        // Set up toolbar icons and actions
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.inflateMenu(R.menu.menu_note_list)
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

        requireContext().let {
            toolbar.setNavigationIcon(R.drawable.ic_icon_unfilled)
            toolbar.navigationIcon?.setTint(it.getColor(R.color.colorOnPrimary))

            // Set overflow icon color
            toolbar.overflowIcon?.setTint(it.getColor(R.color.colorOnPrimary))
        }

        return view
    }

    override fun onPause() {
        super.onPause()
        FileSystem.writeDirEntry(requireContext(), DataProvider.getActiveSubDirectory(), DirEntry(inodes))
        FileSystem.writeDirEntry(requireContext(), root, DirEntry(categories))
    }

    /**
     * Listener for changes to a specified text field
     *
     * @param field: text field to set a listener for
     */
    private fun setOnChangeListenerForTextInput(field: TextInputEditText) {
        field.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                DataProvider.setActiveCategoryName(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /**
     * Close out old category and switch to new category
     *
     * @param inode: new category to switch to
     * @param deleting: true if the currently active category is being deleted, causing the switch
     *                  false otherwise
     */
    private fun setNewCategory(inode: INode?, deleting: Boolean) {
        exitDetailFragment(deleting)
        FileSystem.writeDirEntry(requireContext(), DataProvider.getActiveSubDirectory(), DirEntry(inodes))
        FileSystem.switchCategory(requireContext(), inode)
        updateCategoriesList()
        updateNotesList()
        categoryView?.setSelection(0)
    }

    /**
     * Remove active category from the file system
     */
    private fun deleteCategory() {
        if (DataProvider.getCategories().size > 1) {
            DataProvider.clearInodes()
            setNewCategory(DataProvider.getCategories()[1], true)

            val categoryToDelete = DataProvider.getCategories()[1]
            FileSystem.delete(requireContext(), root, categoryToDelete)
            DataProvider.removeCategory(categoryToDelete)

            editText.setText(DataProvider.getActiveCategoryName())
            updateCategoriesList()
        }
    }

    /**
     * Open detail fragment for specified note (selected from notes list)
     *
     * @param position: selected item's position in the note list
     */
    private fun startNoteFragment(position: Int) {
        notesListAdapter?.getItem(position)?.let { inode ->
            DataProvider.moveINodeToTop(inode)
            updateNotesList()
            listView?.setItemChecked(position, true)

            var note = FileSystem.loadNote(requireContext(), DataProvider.getActiveSubDirectory(), inode.descriptor + inode.id)
            if (note == null)
                note = Note(inode.id, resources.getString(R.string.default_note_name))

            if (ScreenInfoProvider.getScreenInfo(requireActivity()).isDualMode() &&
                !MainActivity.isRotated(requireActivity())
            ) {
                // If spanned and not rotated (list view), open NoteDetailFragment in second container
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.second_container_id,
                        NoteDetailFragment.newInstance(inode, note),
                        buildDetailTag(R.id.second_container_id, inode.id, note.id)
                    ).commit()
            } else {
                // If unspanned or spanned and rotated (extended canvas), open NoteDetailFragment in first container
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.first_container_id,
                        NoteDetailFragment.newInstance(inode, note),
                        buildDetailTag(R.id.first_container_id, inode.id, note.id)
                    ).addToBackStack(null)
                    .commit()
            }
        }
    }

    /**
     * Save and exit detail fragment of currently active note
     *
     * @param deleting: true if the currently active category is being deleted
     *                  false otherwise
     */
    fun exitDetailFragment(deleting: Boolean) {
        activity?.let {
            if (ScreenInfoProvider.getScreenInfo(it).isDualMode() && !MainActivity.isRotated(it)) {
                val fragment = parentFragmentManager.findFragmentById(R.id.second_container_id) as? NoteDetailFragment

                fragment?.let { detail ->
                    if (!deleting) {
                        detail.updateNoteContents()
                        detail.save()
                    }
                    detail.deleted = true // set flag so file isn't resaved on destroy
                    detail.closeFragment()
                }
            }
        }
    }

    /**
     * Indicate change made to note list
     */
    fun updateNotesList() {
        notesListAdapter?.notifyDataSetChanged()
    }

    /**
     * Indicate change made to categories list
     */
    private fun updateCategoriesList() {
        categoriesListAdapter?.notifyDataSetChanged()
    }

    /**
     * Select all the items in the notes list
     */
    private fun selectAllNotes() {
        listView?.let {
            it.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            for (i in 0 until it.count) {
                it.setItemChecked(i, true)
            }
        }
    }

    /**
     * Item in category list is clicked, switch to appropriate category
     *
     * @param adapterView: adapter that contains list of categories
     * @param item: category that was selected
     * @param position: selected item's position in the list
     * @param id: view id of the selected item
     */
    override fun onItemSelected(adapterView: AdapterView<*>, item: View?, position: Int, id: Long) {
        if (selectedFlag) {
            categoriesListAdapter?.let {
                it.getItem(position)?.let { inode ->
                    setNewCategory(inode, false)
                    editText.setText(inode.title)
                }
            }
        }
        selectedFlag = !selectedFlag
    }

    /**
     * Category list was opened, but no item was selected
     *
     * @param parent: adapter that contains list of categories
     */
    override fun onNothingSelected(parent: AdapterView<*>?) {
        // Do nothing
    }

    /**
     * Option from overflow menu selected
     *
     * @param item: option that was selected from the overflow menu
     * @return true if valid item, false otherwise
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_category -> {
                setNewCategory(null, false)
                editText.setText(DataProvider.getActiveCategoryName())
                true
            }
            R.id.action_delete_category -> {
                deleteCategory()
                true
            }
            R.id.action_select -> {
                selectAllNotes()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * Item in note list is clicked, switch to appropriate note
     *
     * @param adapterView: adapter that contains list of categories
     * @param item: note that was selected
     * @param position: selected item's position in the list
     * @param rowId: view id of the selected item
     */
    override fun onItemClick(adapterView: AdapterView<*>, item: View, position: Int, rowId: Long) {
        if (listView?.choiceMode == ListView.CHOICE_MODE_SINGLE) {
            startNoteFragment(position)
        } else {
            listView?.setItemChecked(position, true)
        }
    }

    /**
     * Enable multi-selection of notes on long click
     *
     * @param adapterView: adapter that contains list of categories
     * @param item: note that was selected
     * @param position: selected item's position in the list
     * @param rowId: view id of the selected item
     * @return always return true to consume click event
     */
    override fun onItemLongClick(adapterView: AdapterView<*>, item: View, position: Int, rowId: Long): Boolean {
        listView?.let {
            it.clearChoices()
            it.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL
            it.setItemChecked(position, true)
        }
        return true
    }
}
