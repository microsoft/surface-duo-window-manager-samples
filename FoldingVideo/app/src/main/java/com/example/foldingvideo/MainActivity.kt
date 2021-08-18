/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.foldingvideo

import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoRepository
import androidx.window.layout.WindowInfoRepository.Companion.windowInfoRepository
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // WM
    private lateinit var motionLayout: MotionLayout
    private lateinit var windowInfoRep: WindowInfoRepository

    // ExoPlayer
    private lateinit var playerView: StyledPlayerView
    private lateinit var connectedControls: View
    private lateinit var controlView: StyledPlayerControlView
    private lateinit var vertControlView: StyledPlayerControlView
    private lateinit var player: SimpleExoPlayer

    private lateinit var fab: FloatingActionButton
    private var lastFoldingFeature: FoldingFeature? = null
    private var isSplit: Boolean = false

    companion object {
        val PLAY_POSITION = "play_position"
        val PLAY_WHEN_READY = "play_when_ready"
        val CURRENT_WINDOW_INDEX = "current_window_index"
        val CONTROL_SPLIT = "control_split"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        motionLayout = findViewById<MotionLayout>(R.id.root)

        playerView = findViewById(R.id.player_view)
        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        // PlayerView connected controls (not external)
        connectedControls = findViewById(R.id.exo_center_controls)

        // dual-landscape controls
        controlView = findViewById(R.id.horiz_player_control_view)
        controlView.player = player

        // dual-portrait controls
        vertControlView = findViewById(R.id.vert_player_control_view)
        vertControlView.player = player

        // set up floating action button that splits controls in dual-portrait
        isSplit = false
        fab = findViewById(R.id.fab)
        fab.setOnClickListener { _ ->
            isSplit = !isSplit
            updateSplitControl(lastFoldingFeature)
        }

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
                        fab.hide()

                        // Add views that represent display features
                        for (displayFeature in newLayoutInfo.displayFeatures) {
                            val foldFeature = displayFeature as? FoldingFeature
                            if (foldFeature != null) {

                                lastFoldingFeature = foldFeature

                                // isSeparating isn't currently working - it always returns true
                                // We can use this workaround by checking if the device is a surface duo
                                // and checking the foldFeature state if it isn't
                                if (foldFeature.isSeparating) {

                                    if (foldFeature.orientation == FoldingFeature.Orientation.HORIZONTAL) {

                                        if (isDeviceSurfaceDuo()) {
                                            // if the device is a duo, it will always use a separate video controller
                                            var fold = horizontalFoldPosition(motionLayout, foldFeature)
                                            ConstraintLayout.getSharedValues().fireNewValue(R.id.horiz_fold, fold)
                                            playerView.useController = false
                                        } else {

                                            // for other devices, it will only separate controls if isSeparating is true
                                            // Since isSeparating is not working, we will just check the fold state
                                            if (foldFeature.state == FoldingFeature.State.HALF_OPENED) {
                                                var fold = horizontalFoldPosition(motionLayout, foldFeature)
                                                ConstraintLayout.getSharedValues().fireNewValue(R.id.horiz_fold, fold)
                                                playerView.useController = false
                                            } else {
                                                ConstraintLayout.getSharedValues().fireNewValue(R.id.horiz_fold, 0)
                                                playerView.useController = true // use on-video controls
                                            }
                                        }
                                    } else {
                                        // move over on-video controls so it's not cut off by hinge
                                        setMargins(connectedControls, connectedControls.width / 2, 0, 0, 0)

                                        // make split control fab visible
                                        fab.show()
                                        updateSplitControl(lastFoldingFeature)
                                    }
                                } else {
                                    // reset fold values
                                    ConstraintLayout.getSharedValues().fireNewValue(R.id.horiz_fold, 0)
                                    ConstraintLayout.getSharedValues().fireNewValue(R.id.vert_fold, 0)
                                    playerView.useController = true // use on-video controls
                                }
                            }
                        }
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        var videoUrl = "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4"
        var mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {

        // save exoplayer state vars
        outState.putLong(PLAY_POSITION, player.currentPosition)
        outState.putBoolean(PLAY_WHEN_READY, player.playWhenReady)
        outState.putInt(CURRENT_WINDOW_INDEX, player.currentWindowIndex)
        outState.putBoolean(CONTROL_SPLIT, isSplit)

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        // Load exoplayer state vars
        isSplit = savedInstanceState.getBoolean(CONTROL_SPLIT)
        player.playWhenReady = savedInstanceState.getBoolean(PLAY_WHEN_READY)
        player.seekTo(savedInstanceState.getInt(CURRENT_WINDOW_INDEX), savedInstanceState.getLong(PLAY_POSITION))
        player.prepare()
    }

    /**
     * Returns the position of the horizontal fold relative to the view
     */
    fun horizontalFoldPosition(view: View, foldingFeature: FoldingFeature?): Int {
        val splitRect = getFeatureBoundsInWindow(foldingFeature, view)
        splitRect?.let {
            return view.height.minus(splitRect.top)
        }

        return 0
    }

    /**
     * Returns the position of the vertical fold relative to the view
     */
    fun verticalFoldPosition(view: View, foldingFeature: FoldingFeature?): Int {
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
        displayFeature: DisplayFeature?,
        view: View,
        includePadding: Boolean = true
    ): Rect? {

        if (displayFeature == null) {
            return null
        }

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

    /**
     * Sets the margins of a view
     */
    private fun setMargins(view: View, left: Int, top: Int, right: Int, bottom: Int) {
        if (view.layoutParams is MarginLayoutParams) {
            val p = view.layoutParams as MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    /**
     * Update the split controls in dual-portrait when hinge is vertical
     */
    private fun updateSplitControl(foldFeature: FoldingFeature?) {
        if (foldFeature != null) {
            if (isSplit) {
                var fold = verticalFoldPosition(motionLayout, foldFeature)
                ConstraintLayout.getSharedValues().fireNewValue(R.id.vert_fold, fold)
                playerView.useController = false
            } else {
                ConstraintLayout.getSharedValues().fireNewValue(R.id.vert_fold, 0)
                playerView.useController = true
            }
        }
    }
    /**
     * HACK just to help with testing on Surface Duo AND foldable emulators until WM is stable */
    fun isDeviceSurfaceDuo(): Boolean {
        val surfaceDuoSpecificFeature = "com.microsoft.device.display.displaymask"
        val pm = this@MainActivity.packageManager
        return pm.hasSystemFeature(surfaceDuoSpecificFeature)
    }
}
