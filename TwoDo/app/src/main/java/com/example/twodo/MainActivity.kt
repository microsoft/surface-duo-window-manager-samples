/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.twodo

import android.app.Activity
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.util.Consumer
import androidx.window.DisplayFeature
import androidx.window.FoldingFeature
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import java.util.concurrent.Executor

class MainActivity : Activity() {

    // Jetpack WM
    private lateinit var motionLayout: MotionLayout
    private lateinit var windowManager: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private val mainThreadExecutor = Executor { r: Runnable -> handler.post(r) }
    private val stateContainer = StateContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowManager = WindowManager(this)
        setContentView(R.layout.activity_main)

        motionLayout = findViewById<MotionLayout>(R.id.root)
    }

    override fun onStart() {
        super.onStart()
        windowManager.registerLayoutChangeCallback(mainThreadExecutor, stateContainer)
    }

    override fun onStop() {
        super.onStop()
        windowManager.unregisterLayoutChangeCallback(stateContainer)
    }

    fun goToTwoDoActivity(view: View) {
        val intent = Intent(this, TwoDoActivity::class.java)
        startActivity(intent)
    }

    /**
     * Returns the position of the horizontal fold relative to the view
     */
    fun horizontalFoldPosition(view: View, foldingFeature: FoldingFeature): Int {
        val splitRect = getFeatureBoundsInWindow(foldingFeature, view)
        splitRect?.let {
            return view.height.minus(splitRect.top)
        }

        return 0
    }

    /**
     * Returns the position of the vertical fold relative to the view
     */
    fun verticalFoldPosition(view: View, foldingFeature: FoldingFeature): Int {
        val splitRect = getFeatureBoundsInWindow(foldingFeature, view)
        splitRect?.let {
            return view.width.minus(splitRect.right)
        }

        return 0
    }

    /**
     * Get the bounds of the display feature translated to the View's coordinate space and current
     * position in the window. This will also include view padding in the calculations.
     */
    fun getFeatureBoundsInWindow(
        displayFeature: DisplayFeature,
        view: View,
        includePadding: Boolean = true
    ): Rect? {
        // The the location of the view in window to be in the same coordinate space as the feature.
        val viewLocationInWindow = IntArray(2)
        view.getLocationInWindow(viewLocationInWindow)

        // Intersect the feature rectangle in window with view rectangle to clip the bounds.
        val viewRect = Rect(
            viewLocationInWindow[0], viewLocationInWindow[1],
            viewLocationInWindow[0] + view.width, viewLocationInWindow[1] + view.height
        )

        // Include padding if needed
        if (includePadding) {
            viewRect.left += view.paddingLeft
            viewRect.top += view.paddingTop
            viewRect.right -= view.paddingRight
            viewRect.bottom -= view.paddingBottom
        }

        val featureRectInView = Rect(displayFeature.bounds)
        val intersects = featureRectInView.intersect(viewRect)

        // Checks to see if the display feature overlaps with our view at all
        if ((featureRectInView.width() == 0 && featureRectInView.height() == 0) ||
            !intersects
        ) {
            return null
        }

        // Offset the feature coordinates to view coordinate space start point
        featureRectInView.offset(-viewLocationInWindow[0], -viewLocationInWindow[1])

        return featureRectInView
    }

    inner class StateContainer : Consumer<WindowLayoutInfo> {
        override fun accept(newLayoutInfo: WindowLayoutInfo) {

            // reset fold values
            ConstraintLayout.getSharedValues().fireNewValue(R.id.horiz_fold, 0)
            ConstraintLayout.getSharedValues().fireNewValue(R.id.vert_fold, 0)

            // Add views that represent display features
            for (displayFeature in newLayoutInfo.displayFeatures) {
                val foldFeature = displayFeature as? FoldingFeature
                if (foldFeature != null) {
                    if (foldFeature.orientation == FoldingFeature.ORIENTATION_HORIZONTAL) {
                        var fold = horizontalFoldPosition(motionLayout, foldFeature)
                        ConstraintLayout.getSharedValues().fireNewValue(R.id.horiz_fold, fold)
                    } else {
                        var fold = verticalFoldPosition(motionLayout, foldFeature)
                        ConstraintLayout.getSharedValues().fireNewValue(R.id.vert_fold, fold)
                    }
                }
            }
        }
    }
}
