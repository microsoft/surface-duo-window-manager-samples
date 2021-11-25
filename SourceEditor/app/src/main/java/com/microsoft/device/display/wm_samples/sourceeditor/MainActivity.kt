/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.sourceeditor

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ReactiveGuide
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.microsoft.device.display.wm_samples.sourceeditor.includes.FileHandler
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.DualScreenViewModel
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.WebViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var fileBtn: ImageView
    private lateinit var saveBtn: ImageView

    private lateinit var fileHandler: FileHandler
    private lateinit var webVM: WebViewModel
    private lateinit var dualScreenVM: DualScreenViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // App based setup
        webVM = ViewModelProvider(this).get(WebViewModel::class.java)
        fileHandler = FileHandler(this, webVM, contentResolver)

        // Layout based setup
        dualScreenVM = ViewModelProvider(this).get(DualScreenViewModel::class.java)
        dualScreenVM.setIsDualScreen(false) // assume single screen on startup

        // display action toolbar
        this.supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.component_action_toolbar)

        // action toolbar functionality
        fileBtn = findViewById(R.id.btn_file)
        fileBtn.setOnClickListener {
            fileHandler.openFile(Uri.EMPTY)
        }

        saveBtn = findViewById(R.id.btn_save)
        saveBtn.setOnClickListener {
            fileHandler.createFile(Uri.EMPTY)
        }

        // Create a new coroutine since repeatOnLifecycle is a suspend function
        lifecycleScope.launch(Dispatchers.Main) {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collect from windowInfoTracker when the lifecycle is STARTED
                // and stops collection when the lifecycle is STOPPED
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { newLayoutInfo ->
                        var isDualScreen = false

                        // Check display features for an active hinge/fold
                        for (displayFeature in newLayoutInfo.displayFeatures) {
                            val foldingFeature = displayFeature as? FoldingFeature
                            if (foldingFeature != null) {
                                // hinge found, check to see if it should be split screen
                                val hingeBounds = foldingFeature.bounds
                                isDualScreen = true

                                if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
                                    setBoundsVerticalHinge(hingeBounds)
                                } else {
                                    setBoundsHorizontalHinge(hingeBounds)
                                }
                            }
                        }
                        if (!isDualScreen) {
                            setBoundsNoHinge()
                        }
                        dualScreenVM.setIsDualScreen(isDualScreen)
                    }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        // request to save a file has been made, add data to newly created file
        if (requestCode == FileHandler.CREATE_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                fileHandler.alterDocument(uri)
            }
        }
        // request to load file contents has been made, process the file's contents
        else if (requestCode == FileHandler.PICK_TXT_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                fileHandler.processFileData(uri, null)
            }
        }
    }

    /**
     * Calculate total height taken up by upper toolbars
     * Add measurements here if additional status/toolbars are used
     */
    private fun upperToolbarSpacing(): Int {
        val toolbar: Toolbar = findViewById(R.id.list_toolbar)
        return toolbar.height
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
