/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.video_trivia_sample

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.example.video_trivia_sample.model.DataProvider
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var rootView: MotionLayout
    private lateinit var endTriviaView: View
    private lateinit var bottomTriviaView: View

    private lateinit var playerControlView: StyledPlayerControlView
    private lateinit var playerView: StyledPlayerView
    private lateinit var player: SimpleExoPlayer
    private lateinit var triviaEnableButton: ImageButton

    private var triviaToggle: Boolean = true
    private var spanToggle: Boolean = false
    private var spanOrientation: FoldingFeature.Orientation = FoldingFeature.Orientation.VERTICAL
    private var guidePosition: Int = 0
    private var padding: Int = 0
    private var triviaLayout: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.root)
        triviaEnableButton = findViewById(R.id.triviaEnableButton)
        endTriviaView = findViewById(R.id.end_trivia_view)
        bottomTriviaView = findViewById(R.id.bottom_trivia_view)

        playerControlView = findViewById(R.id.player_control_view)
        playerView = findViewById(R.id.player_view)

        // Initialize Exoplayer
        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        playerControlView.player = player
        setPlayerMessages(player)

        // Create a new coroutine since repeatOnLifecycle is a suspend function
        lifecycleScope.launch(Dispatchers.Main) {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Safely collect from WindowInfoTracker when the lifecycle is STARTED
                // and stops collection when the lifecycle is STOPPED
                WindowInfoTracker.getOrCreate(this@MainActivity)
                    .windowLayoutInfo(this@MainActivity)
                    .collect { newLayoutInfo ->
                        spanToggle = false

                        for (displayFeature: DisplayFeature in newLayoutInfo.displayFeatures) {
                            if (displayFeature is FoldingFeature) {
                                spanToggle = true
                                spanOrientation = displayFeature.orientation
                                if (spanOrientation == FoldingFeature.Orientation.HORIZONTAL) {
                                    guidePosition =
                                        rootView.height - rootView.paddingBottom - displayFeature.bounds.top
                                    padding = displayFeature.bounds.height()
                                } else {
                                    guidePosition =
                                        rootView.width - rootView.paddingEnd - displayFeature.bounds.left
                                    padding = displayFeature.bounds.width()
                                }
                            }
                        }
                        changeLayout()
                    }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Start MotionLayout in full screen state
        rootView.setState(R.id.fullscreen_constraints, -1, -1)

        // Start exoplayer
        val mediaItem =
            MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        player.setMediaItem(mediaItem)
        player.prepare()

        // Callback for trivia toggle button
        triviaEnableButton.setOnClickListener {
            triviaToggle = !triviaToggle
            changeLayout()
        }
    }

    companion object {
        const val STATE_TRIVIA = "triviaToggle"
        const val STATE_SPAN = "spanToggle"
        const val STATE_PLAY_WHEN_READY = "playerPlayWhenReady"
        const val STATE_CURRENT_POSITION = "playerPlaybackPosition"
        const val STATE_CURRENT_WINDOW_INDEX = "playerCurrentWindowIndex"
        const val STATE_TRIVIA_LAYOUT = "triviaLayout"
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        triviaToggle = savedInstanceState.getBoolean(STATE_TRIVIA)
        spanToggle = savedInstanceState.getBoolean(STATE_SPAN)
        player.playWhenReady = savedInstanceState.getBoolean(STATE_PLAY_WHEN_READY)
        player.seekTo(
            savedInstanceState.getInt(STATE_CURRENT_WINDOW_INDEX),
            savedInstanceState.getLong(STATE_CURRENT_POSITION)
        )
        player.prepare()
        triviaLayout = savedInstanceState.getInt(STATE_TRIVIA_LAYOUT)
        if (triviaLayout != 0)
            updateTriviaView(triviaLayout)
    }

    override fun onStop() {
        super.onStop()
        player.stop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putBoolean(STATE_TRIVIA, triviaToggle)
            putBoolean(STATE_SPAN, spanToggle)
            putBoolean(STATE_PLAY_WHEN_READY, player.playWhenReady)
            putLong(STATE_CURRENT_POSITION, player.currentPosition)
            putInt(STATE_CURRENT_WINDOW_INDEX, player.currentWindowIndex)
            putInt(STATE_TRIVIA_LAYOUT, triviaLayout)
        }

        super.onSaveInstanceState(outState)
    }

    // function to animate into "fullscreen" constraint set
    private fun setFullscreen() {
        bottomTriviaView.setPadding(0, 0, 0, 0)
        endTriviaView.setPadding(0, 0, 0, 0)
        rootView.transitionToState(R.id.fullscreen_constraints, 500)
    }

    // function to animate into "trivia enabled" constraint set, modifying guide position at the same time
    private fun setGuides(
        vertical_position: Int,
        vertical_padding: Int,
        horizontal_position: Int,
        horizontal_padding: Int,
    ) {
        bottomTriviaView.setPadding(0, vertical_padding, 0, 0)
        endTriviaView.setPadding(horizontal_padding, 0, 0, 0)

        val constraintSet = rootView.getConstraintSet(R.id.shrunk_constraints)
        constraintSet.setGuidelineEnd(R.id.vertical_guide, vertical_position)
        constraintSet.setGuidelineEnd(R.id.horizontal_guide, horizontal_position)

        if (rootView.currentState == R.id.shrunk_constraints) {
            rootView.updateStateAnimate(R.id.shrunk_constraints, constraintSet, 500)
        } else {
            rootView.transitionToState(R.id.shrunk_constraints, 500)
        }
    }

    // logic tree that decides layout
    fun changeLayout() {
        if (spanToggle) { // if app is spanned across a fold
            if (spanOrientation == FoldingFeature.Orientation.HORIZONTAL) { // if fold is horizontal
                if (triviaToggle || this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { // if trivia is enabled or device is taller than wider
                    setGuides(guidePosition, padding, 0, 0, true)
                } else {
                    setFullscreen()
                }
            } else if (triviaToggle) { // if trivia is enabled
                setGuides(0, 0, guidePosition, padding)
            } else {
                setFullscreen()
            }
        } else if (triviaToggle) { // if trivia is enabled
            if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // if the phone is landscape
                setGuides(0, 0, rootView.width / 3, 0)
            } else {
                setGuides(rootView.height / 2, 0, 0, 0)
            }
        } else {
            setFullscreen()
        }
    }

    private fun setPlayerMessages(player: ExoPlayer) {
        val funFactMessage = player.createMessage { _, _ -> updateTriviaView(R.layout.fun_fact_layout) }
        val castMessage = player.createMessage { _, _ -> updateTriviaView(R.layout.cast_layout) }
        val soundtrackMessage = player.createMessage { _, _ -> updateTriviaView(R.layout.soundtrack_layout) }
        val removeMessage = player.createMessage { _, _ -> updateTriviaView() }
        val removeMessage2 = player.createMessage { _, _ -> updateTriviaView() }

        // Times for quick testing
        funFactMessage.setPosition(1000).setDeleteAfterDelivery(false).send()
        removeMessage.setPosition(3000).setDeleteAfterDelivery(false).send()
        soundtrackMessage.setPosition(4000).setDeleteAfterDelivery(false).send()
        removeMessage2.setPosition(6000).setDeleteAfterDelivery(false).send()
        castMessage.setPosition(7000).setDeleteAfterDelivery(false).send()

        // Times that correspond with video events
//        funFactMessage.setPosition(1000).send()
//        removeMessage.setPosition(34000).send()
//        soundtrackMessage.setPosition(35000).send()
//        removeMessage2.setPosition(119000).send()
//        castMessage.setPosition(120000).send()
    }

    private fun updateTriviaView(newViewId: Int? = null) {
        val triviaViewBottom = bottomTriviaView.findViewById<LinearLayout>(R.id.trivia_layout)
        val triviaViewEnd = endTriviaView.findViewById<LinearLayout>(R.id.trivia_layout)

        runOnUiThread {
            for (view in listOf(triviaViewBottom, triviaViewEnd)) {
                if (newViewId == null) {
                    view.removeAllViews()
                    triviaLayout = 0
                } else {
                    val newView = layoutInflater.inflate(newViewId, null)
                    view.addView(newView)

                    // If cast layout, initialize cast member RecyclerView adapter
                    if (newViewId == R.layout.cast_layout) {
                        val castAdapter = CastListAdapter(DataProvider.actors)
                        newView.findViewById<RecyclerView>(R.id.cast_member_list).adapter = castAdapter
                    }

                    triviaLayout = newViewId
                }
            }
        }
    }
}
