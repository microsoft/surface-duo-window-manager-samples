/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.wm_samples.photoeditor

import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ReactiveGuide
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.window.FoldingFeature
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.util.concurrent.Executor

// even with no toolbar, the hinge is offset by a default amount
const val DEFAULT_TOOLBAR_OFFSET = 18

class MainActivity : AppCompatActivity() {

    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainThreadExecutor = Executor { r: Runnable -> mainHandler.post(r) }
    private val wmCallback = WMCallback()

    private lateinit var viewModel: PhotoEditorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(PhotoEditorViewModel::class.java)

        // Layout based setup
        windowManager = WindowManager(this)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        viewModel.saturation = savedInstanceState.getFloat("saturation")
        viewModel.brightness = savedInstanceState.getFloat("brightness")
        viewModel.warmth = savedInstanceState.getFloat("warmth")
        viewModel.selectedControl = savedInstanceState.getInt("selectedControl")
    }

    // ---------------------------------Window Manager Setup---------------------------------- \\
    override fun onStart() {
        super.onStart()
        windowManager.registerLayoutChangeCallback(mainThreadExecutor, wmCallback)
    }

    override fun onStop() {
        super.onStop()
        windowManager.unregisterLayoutChangeCallback(wmCallback)
    }

    /**
     * Calculate total height taken up by upper toolbars
     * Add measurements here if additional status/toolbars are used
     */
    private fun upperToolbarSpacing(): Int {
        // The toolbar for this app is a child of the main activity, so we can use the default value
        return DEFAULT_TOOLBAR_OFFSET
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
        findViewById<View?>(R.id.bounding_rect)?.let { boundingRect ->
            val hingeWidth = hingeBounds.right - hingeBounds.left
            val params: ViewGroup.LayoutParams = boundingRect.layoutParams
            params.width = hingeWidth
            boundingRect.layoutParams = params

            // left fragment is aligned with the right side of the hinge and vice-versa
            // add padding to ensure fragments do not overlap the hinge
            val leftFragment: ConstraintLayout = findViewById(R.id.primary_fragment_container)
            leftFragment.setPadding(0, 0, hingeWidth, 0)

            val rightFragment: ConstraintLayout = findViewById(R.id.secondary_fragment_container)
            rightFragment.setPadding(hingeWidth, 0, 0, 0)
        }
    }

    /**
     * Set the bounding rectangle for a configuration with a horizontal hinge
     */
    private fun setBoundsHorizontalHinge(hingeBounds: Rect) {
        findViewById<View?>(R.id.bounding_rect)?.let { boundingRect ->
            val hingeHeight = hingeBounds.bottom - hingeBounds.top
            val params: ViewGroup.LayoutParams = boundingRect.layoutParams
            params.height = hingeHeight
            boundingRect.layoutParams = params

            val guide: ReactiveGuide = findViewById(R.id.horiz_guide)
            guide.setGuidelineBegin(hingeBounds.top + boundingOffset(hingeHeight) - upperToolbarSpacing())

            // top fragment is aligned with the bottom side of the hinge and vice-versa
            // add padding to ensure fragments do not overlap the hinge
            val topFragment: ConstraintLayout = findViewById(R.id.primary_fragment_container)
            topFragment.setPadding(0, 0, 0, hingeHeight)

            val bottomFragment: ConstraintLayout = findViewById(R.id.secondary_fragment_container)
            bottomFragment.setPadding(0, hingeHeight, 0, 0)
        }
    }

    /**
     * Set the bounding rectangle for a configuration with no hinge (single screen)
     */
    private fun setBoundsNoHinge() {
        findViewById<View?>(R.id.bounding_rect)?.let { boundingRect ->
            val params: ViewGroup.LayoutParams = boundingRect.layoutParams

            // fill parent
            params.height = -1
            params.width = -1
            boundingRect.layoutParams = params

            val guide: ReactiveGuide = findViewById(R.id.horiz_guide)
            guide.setGuidelineEnd(0)
        }
    }

    /**
     * Jetpack Window Manager callback
     * This callback gets triggered whenever there is a layout change (rotated, spanned, etc)
     */
    inner class WMCallback : Consumer<WindowLayoutInfo> {
        override fun accept(newLayoutInfo: WindowLayoutInfo?) {
            newLayoutInfo?.let {
                viewModel.isDualScreen = false

                // Check display features for an active hinge/fold
                for (displayFeature in it.displayFeatures) {
                    val foldingFeature = displayFeature as? FoldingFeature
                    if (foldingFeature != null) {
                        // hinge found, check to see if it should be split screen
                        if (isDeviceSurfaceDuo() || foldingFeature.state == FoldingFeature.State.HALF_OPENED) {
                            val hingeBounds = foldingFeature.bounds
                            viewModel.isDualScreen = true

                            if (foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL) {
                                setBoundsVerticalHinge(hingeBounds)
                            } else {
                                setBoundsHorizontalHinge(hingeBounds)
                            }
                        }
                    }
                }
                if (!viewModel.isDualScreen) {
                    setBoundsNoHinge()
                }
            }
        }
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
