/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote

import Defines.GET_STARTED_FRAGMENT
import Defines.INODE
import Defines.LIST_FRAGMENT
import Defines.NOTE
import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ReactiveGuide
import androidx.core.util.Consumer
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoRepository
import androidx.window.layout.WindowInfoRepository.Companion.windowInfoRepository
import com.microsoft.device.display.wm_samples.twonote.fragments.GetStartedFragment
import com.microsoft.device.display.wm_samples.twonote.fragments.NoteDetailFragment
import com.microsoft.device.display.wm_samples.twonote.fragments.NoteListFragment
import com.microsoft.device.display.wm_samples.twonote.models.DirEntry
import com.microsoft.device.display.wm_samples.twonote.models.DualScreenViewModel
import com.microsoft.device.display.wm_samples.twonote.models.INode
import com.microsoft.device.display.wm_samples.twonote.models.Note
import com.microsoft.device.display.wm_samples.twonote.utils.DataProvider
import com.microsoft.device.display.wm_samples.twonote.utils.FileSystem
import com.microsoft.device.display.wm_samples.twonote.utils.buildDetailTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Activity that manages fragments and preservation of data through the app's lifecycle
 */
class MainActivity :
    AppCompatActivity(),
    NoteDetailFragment.OnFragmentInteractionListener {

    companion object {
        /**
         * Returns whether device is rotated (to the left or right) or not
         *
         * @param context: application context
         * @return true if rotated, false otherwise
         */
        fun isRotated(context: Context, isDualScreen: Boolean): Boolean {
            val singleScreenLandscape = !isDualScreen &&
                (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            val dualScreenLandscape = isDualScreen &&
                (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
            return singleScreenLandscape || dualScreenLandscape
        }
    }

    // Jetpack Window Manager
    private lateinit var windowInfoRep: WindowInfoRepository

    private var savedNote: Note? = null
    private var savedINode: INode? = null
    private var dualScreenVM = DualScreenViewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dualScreenVM = ViewModelProvider(this).get(DualScreenViewModel::class.java)
        dualScreenVM.isDualScreen = false

        // Get data from previously selected note (if available)
        savedNote = savedInstanceState?.getSerializable(NOTE) as? Note
        savedINode = savedInstanceState?.getSerializable(INODE) as? INode

        // Layout based setup
        windowInfoRep = windowInfoRepository()
        // Create a new coroutine since repeatOnLifecycle is a suspend function
        lifecycleScope.launch(Dispatchers.Main) {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collect from windowInfoRepo when the lifecycle is STARTED
                // and stops collection when the lifecycle is STOPPED
                windowInfoRep.windowLayoutInfo
                    .collect { newLayoutInfo ->
                        newLayoutInfo?.let {
                            dualScreenVM.isDualScreen = false
                            val noteSelected = savedNote != null && savedINode != null

                            // Check display features for an active hinge/fold
                            for (displayFeature in it.displayFeatures) {
                                val foldingFeature = displayFeature as? FoldingFeature
                                if (foldingFeature != null) {
                                    // hinge found, check to see if it should be split screen
                                    if (isDeviceSurfaceDuo() || foldingFeature.state == FoldingFeature.State.HALF_OPENED) {
                                        val hingeBounds = foldingFeature.bounds
                                        dualScreenVM.isDualScreen = true

                                        if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
                                            setBoundsVerticalHinge(hingeBounds)
                                        } else {
                                            // we don't want a split screen in the horizontal orientation with this app
                                            setBoundsNoHinge()
                                        }
                                        selectDualScreenFragments(noteSelected, savedNote, savedINode)
                                    }
                                }
                            }
                            if (!dualScreenVM.isDualScreen) {
                                selectSingleScreenFragment(noteSelected, savedNote, savedINode)
                                setBoundsNoHinge()
                            }
                            savedNote = null
                            savedINode = null
                        }
                    }
            }
        }
    }

    /**
     * Select which fragment should be inflated in single-screen mode
     *
     * @param noteSelected: true if savedInstanceState contained a specific note/inode, false otherwise
     * @param note: note from savedInstanceState
     * @param inode: inode from savedInstanceState
     */
    private fun selectSingleScreenFragment(noteSelected: Boolean, note: Note?, inode: INode?) {
        if (!supportFragmentManager.isDestroyed) {
            // Remove fragment from second container if it exists
            removeSecondFragment()

            if (noteSelected) {
                startNoteDetailFragment(R.id.primary_fragment_container, note!!, inode!!)
            } else {
                startNoteListFragment()
            }
        }
    }

    /**
     * Select which fragment(s) should be inflated in dual-screen mode
     *
     * @param noteSelected: true if savedInstanceState contained a specific note/inode, false otherwise
     * @param note: note from savedInstanceState
     * @param inode: inode from savedInstanceState
     */
    private fun selectDualScreenFragments(noteSelected: Boolean, note: Note?, inode: INode?) {
        if (!supportFragmentManager.isDestroyed) {
            // If rotated, use extended canvas pattern, otherwise use list-detail pattern
            if (isRotated(this, dualScreenVM.isDualScreen)) {
                // Remove fragment from second container if it exists
                removeSecondFragment()

                if (noteSelected) {
                    startNoteDetailFragment(R.id.primary_fragment_container, note!!, inode!!)
                } else {
                    startNoteListFragment()
                }
            } else {
                if (noteSelected) {
                    startNoteDetailFragment(R.id.secondary_fragment_container, note!!, inode!!)
                } else {
                    startGetStartedFragment()
                }
                startNoteListFragment()
            }
        }
    }

    /**
     * Remove fragment from second container
     */
    private fun removeSecondFragment() {
        supportFragmentManager.findFragmentById(R.id.secondary_fragment_container)?.let {
            supportFragmentManager.beginTransaction().remove(it).commit()
        }
    }

    /**
     * Start note list view fragment in first container
     */
    private fun startNoteListFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.primary_fragment_container, NoteListFragment(), LIST_FRAGMENT)
            .commit()
    }

    /**
     * Start note detail view fragment in specified container
     *
     * @param container: container to start fragment in
     * @param note: note to display in fragment
     * @param inode: inode associated with note to display in fragment
     */
    private fun startNoteDetailFragment(container: Int, note: Note, inode: INode) {
        val tag = buildDetailTag(container, inode.id, note.id)
        if (supportFragmentManager.findFragmentByTag(tag) == null) {
            supportFragmentManager.beginTransaction()
                .replace(container, NoteDetailFragment.newInstance(inode, note), tag)
                .commit()
        }
    }

    /**
     * Start welcome fragment in second container
     */
    private fun startGetStartedFragment() {
        if (supportFragmentManager.findFragmentByTag(GET_STARTED_FRAGMENT) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.secondary_fragment_container, GetStartedFragment(), GET_STARTED_FRAGMENT)
                .commit()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val firstFrag = supportFragmentManager.findFragmentById(R.id.primary_fragment_container)
        val secondFrag = supportFragmentManager.findFragmentById(R.id.primary_fragment_container)

        // Save data from note detail view for configuration changes
        if (secondFrag is NoteDetailFragment) {
            if (secondFrag.deleted)
                outState.clear()
            else
                saveCurrentNote(outState, secondFrag)
        } else if (secondFrag is GetStartedFragment) {
            outState.clear()
        } else if (firstFrag is NoteDetailFragment) {
            if (firstFrag.deleted)
                outState.clear()
            else
                saveCurrentNote(outState, firstFrag)
        }
    }

    /**
     * Save NoteDetailFragment's note and inode data to outState bundle
     *
     * @param outState: bundle to save data in
     * @param frag: NoteDetailFragment to extract note/inode data from
     */
    private fun saveCurrentNote(outState: Bundle, frag: NoteDetailFragment) {
        outState.putSerializable(NOTE, frag.arguments?.getSerializable(NOTE))
        outState.putSerializable(INODE, frag.arguments?.getSerializable(INODE))
    }

    /**
     * Communicate from NoteFragment to NoteListFragment that a note/inode has been edited
     */
    override fun onINodeUpdate() {
        // Write change to file system
        FileSystem.writeDirEntry(applicationContext, DataProvider.getActiveSubDirectory(), DirEntry(DataProvider.getINodes()))

        // Notify NoteListFragment (if it exists)
        (supportFragmentManager.findFragmentByTag(LIST_FRAGMENT) as? NoteListFragment)?.updateNotesList()
    }

    // -------------------------- Window Manager Section --------------------------- \\

    /**
     * Calculate total height taken up by upper toolbars
     * Add measurements here if additional status/toolbars are used
     */
    private fun upperToolbarSpacing(): Int {
        return Defines.DEFAULT_TOOLBAR_OFFSET
    }

    /**
     * Calculate the center offset between the guideline and the bounding box
     */
    private fun boundingOffset(height: Int): Int {
        return height / 2
    }

    /**
     * Set the bounding rectangle for a configuration with a vertical hinge
     */
    private fun setBoundsVerticalHinge(hingeBounds: Rect) {
        val hingeWidth = hingeBounds.right - hingeBounds.left

        val boundingRect: View = findViewById(R.id.bounding_rect)
        val params: ViewGroup.LayoutParams = boundingRect.layoutParams
        params.width = hingeWidth
        boundingRect.layoutParams = params

        // left fragment is aligned with the right side of the hinge and vice-versa
        // add padding to ensure fragments do not overlap the hinge
        val leftFragment: FragmentContainerView = findViewById(R.id.primary_fragment_container)
        leftFragment.setPadding(0, 0, hingeWidth, 0)

        val rightFragment: FragmentContainerView = findViewById(R.id.secondary_fragment_container)
        rightFragment.setPadding(hingeWidth, 0, 0, 0)
        rightFragment.visibility = View.VISIBLE
    }

    /**
     * Set the bounding rectangle for a configuration with a horizontal hinge
     */
    private fun setBoundsHorizontalHinge(hingeBounds: Rect) {
        val hingeHeight = hingeBounds.bottom - hingeBounds.top

        val boundingRect: View = findViewById(R.id.bounding_rect)
        val params: ViewGroup.LayoutParams = boundingRect.layoutParams
        params.height = hingeHeight
        boundingRect.layoutParams = params

        val guide: ReactiveGuide = findViewById(R.id.horiz_guide)
        guide.setGuidelineBegin(hingeBounds.top + boundingOffset(hingeHeight) - upperToolbarSpacing())

        // top fragment is aligned with the bottom side of the hinge and vice-versa
        // add padding to ensure fragments do not overlap the hinge
        val topFragment: FragmentContainerView = findViewById(R.id.primary_fragment_container)
        topFragment.setPadding(0, 0, 0, hingeHeight)

        val bottomFragment: FragmentContainerView = findViewById(R.id.secondary_fragment_container)
        bottomFragment.setPadding(0, hingeHeight, 0, 0)
        bottomFragment.visibility = View.VISIBLE
    }

    /**
     * Set the bounding rectangle for a configuration with no hinge (single screen)
     */
    private fun setBoundsNoHinge() {
        val boundingRect: View = findViewById(R.id.bounding_rect)
        val params: ViewGroup.LayoutParams = boundingRect.layoutParams

        // fill parent
        params.height = -1
        params.width = -1
        boundingRect.layoutParams = params

        val guide: ReactiveGuide = findViewById(R.id.horiz_guide)
        guide.setGuidelineEnd(0)
    }

    /**
     * HACK just to help with testing on Surface Duo AND foldable emulators until WM is stable
     */
    fun isDeviceSurfaceDuo(): Boolean {
        val surfaceDuoSpecificFeature = "com.microsoft.device.display.displaymask"
        val pm = this@MainActivity.packageManager
        return pm.hasSystemFeature(surfaceDuoSpecificFeature)
    }
}
