/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.samples.sourceeditor.utils

import com.microsoft.device.dualscreen.ScreenInfo
import com.microsoft.device.dualscreen.ScreenInfoListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Simple implementation for [ScreenInfoListener] that saves internally the last screen info data.
 */
class ScreenInfoListenerImpl : ScreenInfoListener {
    private var _screenInfo: ScreenInfo? = null
    val screenInfo: ScreenInfo?
        get() = _screenInfo
    private var screenInfoLatch: CountDownLatch? = null

    override fun onScreenInfoChanged(screenInfo: ScreenInfo) {
        _screenInfo = screenInfo
        screenInfoLatch?.countDown()
    }

    /**
     * Resets the last screen info to [null]
     */
    fun resetScreenInfo() {
        _screenInfo = null
    }

    /**
     * Resets screen info counter when waiting for a screen changes to happen before calling
     * [waitForScreenInfoChanges].
     */
    fun resetScreenInfoCounter() {
        screenInfoLatch = CountDownLatch(1)
    }

    /**
     * Blocks and waits for the next screen info changes to happen.
     * @return {@code true} if the screen info changed before the timeout count reached zero and
     *         {@code false} if the waiting time elapsed before the changes happened.
     */
    fun waitForScreenInfoChanges(): Boolean {
        return try {
            val result = screenInfoLatch?.await(10, TimeUnit.SECONDS) ?: false
            result
        } catch (e: InterruptedException) {
            false
        }
    }
}