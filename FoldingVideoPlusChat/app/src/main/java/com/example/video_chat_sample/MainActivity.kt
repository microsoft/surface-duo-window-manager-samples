/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.video_chat_sample

import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.core.util.Consumer
import androidx.window.DisplayFeature
import androidx.window.FoldingFeature
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerControlView
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.util.Util
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var rootView: MotionLayout
    private lateinit var windowManager: WindowManager
    private lateinit var endChatView: View
    private lateinit var bottomChatView: View
    private val handler = Handler(Looper.getMainLooper())
    private val mainThreadExecutor = Executor { r: Runnable -> handler.post(r) }
    private val stateContainer = StateContainer()

    private lateinit var playerControlView: StyledPlayerControlView
    private lateinit var playerView: StyledPlayerView
    private lateinit var player: SimpleExoPlayer
    private lateinit var chatEnableButton: ImageButton

    private var keyboardToggle: Boolean = false
    private var chatToggle: Boolean = true
    private var spanToggle: Boolean = false
    private var spanOrientation: Int = FoldingFeature.ORIENTATION_VERTICAL
    private var guidePosition: Int = 0
    private var chatPadding: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Window Manager
        windowManager = WindowManager(this)
        setContentView(R.layout.activity_main)

        rootView = findViewById(R.id.root)
        chatEnableButton = findViewById(R.id.chatEnableButton)
        endChatView = findViewById(R.id.end_chat_view)
        bottomChatView = findViewById(R.id.bottom_chat_view)

        playerControlView = findViewById(R.id.player_control_view)
        playerView = findViewById(R.id.player_view)

        // Initialize Exoplayer
        player = SimpleExoPlayer.Builder(this).build()
        playerView.player = player
        playerControlView.player = player
    }

    override fun onStart() {
        super.onStart()

        // Start MotionLayout in full screen state
        rootView.setState(R.id.fullscreen_constraints, -1, -1)

        // Register callback with window manager
        windowManager.registerLayoutChangeCallback(mainThreadExecutor, stateContainer)

        // Start exoplayer
        val mediaItem = MediaItem.fromUri("https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4")
        player.setMediaItem(mediaItem)
        player.prepare()

        // Callback for chat toggle button
        chatEnableButton.setOnClickListener {
            chatToggle = !chatToggle
            changeLayout()
        }

        // Callback for keyboard opening
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val tempKeyboardToggle = if (Util.SDK_INT >= 30) {
                rootView.rootWindowInsets.isVisible(WindowInsets.Type.ime())
            } else {
                rootView.rootWindowInsets.systemWindowInsetBottom > 200
            }

            if (tempKeyboardToggle != keyboardToggle) {
                keyboardToggle = tempKeyboardToggle
                changeLayout()
            }
        }
    }

    // Callback for layout changing
    inner class StateContainer : Consumer<WindowLayoutInfo> {
        override fun accept(newLayoutInfo: WindowLayoutInfo) {
            spanToggle = false

            for (displayFeature: DisplayFeature in newLayoutInfo.displayFeatures) {
                if (displayFeature is FoldingFeature) {
                    spanToggle = true
                    spanOrientation = displayFeature.orientation
                    if (spanOrientation == FoldingFeature.ORIENTATION_HORIZONTAL) {
                        guidePosition = rootView.height - rootView.paddingBottom - displayFeature.bounds.top
                        chatPadding = displayFeature.bounds.height()
                    } else {
                        guidePosition = rootView.width - rootView.paddingEnd - displayFeature.bounds.left
                        chatPadding = displayFeature.bounds.width()
                    }
                }
            }
            changeLayout()
        }
    }

    companion object {
        const val STATE_CHAT = "chatToggle"
        const val STATE_SPAN = "spanToggle"
        const val STATE_PLAY_WHEN_READY = "playerPlayWhenReady"
        const val STATE_CURRENT_POSITION = "playerPlaybackPosition"
        const val STATE_CURRENT_WINDOW_INDEX = "playerCurrentWindowIndex"
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        chatToggle = savedInstanceState.getBoolean(STATE_CHAT)
        spanToggle = savedInstanceState.getBoolean(STATE_SPAN)
        player.playWhenReady = savedInstanceState.getBoolean(STATE_PLAY_WHEN_READY)
        player.seekTo(savedInstanceState.getInt(STATE_CURRENT_WINDOW_INDEX), savedInstanceState.getLong(STATE_CURRENT_POSITION))
        player.prepare()
    }

    override fun onStop() {
        super.onStop()
        player.stop()
        windowManager.unregisterLayoutChangeCallback(stateContainer)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putBoolean(STATE_CHAT, chatToggle)
            putBoolean(STATE_SPAN, spanToggle)
            putBoolean(STATE_PLAY_WHEN_READY, player.playWhenReady)
            putLong(STATE_CURRENT_POSITION, player.currentPosition)
            putInt(STATE_CURRENT_WINDOW_INDEX, player.currentWindowIndex)
        }

        super.onSaveInstanceState(outState)
    }

    // function to animate into "fullscreen" constraint set
    private fun setFullscreen() {
        bottomChatView.setPadding(0, 0, 0, 0)
        endChatView.setPadding(0, 0, 0, 0)
        rootView.transitionToState(R.id.fullscreen_constraints, 500)
    }

    // function to animate into "chat enabled" constraint set, modifying guide position at the same time
    private fun setGuides(vertical_position: Int, vertical_padding: Int, horizontal_position: Int, horizontal_padding: Int) {
        bottomChatView.setPadding(0, vertical_padding, 0, 0)
        endChatView.setPadding(horizontal_padding, 0, 0, 0)

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
            if (spanOrientation == FoldingFeature.ORIENTATION_HORIZONTAL) { // if fold is horizontal
                if (keyboardToggle) { // if keyboard is enabled
                    setGuides(0, 0, rootView.width / 3, 0)
                } else if (chatToggle || this.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) { // if chat is enabled or device is taller than wider
                    setGuides(guidePosition, chatPadding, 0, 0)
                } else {
                    setFullscreen()
                }
            } else if (chatToggle) { // if chat is enabled
                setGuides(0, 0, guidePosition, chatPadding)
            } else {
                setFullscreen()
            }
        } else if (chatToggle) { // if chat is enabled
            if (this.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) { // if the phone is landscape
                setGuides(0, 0, rootView.width / 3, 0)
            } else if (!keyboardToggle) {
                setGuides(rootView.height / 2, 0, 0, 0)
            } else { // if the keyboard is enabled
                setFullscreen()
            }
        } else {
            setFullscreen()
        }
    }
}
