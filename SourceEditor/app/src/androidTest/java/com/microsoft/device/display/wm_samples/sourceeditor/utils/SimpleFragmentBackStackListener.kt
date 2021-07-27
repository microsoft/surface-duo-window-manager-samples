/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.sourceeditor.utils

import androidx.fragment.app.FragmentManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Simple implementation for [FragmentManager.OnBackStackChangedListener] t
 * hat decrease the internal [CountDownLatch] when a fragment is added to the back stack.
 */
class SimpleFragmentBackStackListener : FragmentManager.OnBackStackChangedListener {
    private var fragmentCountDownLatch: CountDownLatch? = null

    override fun onBackStackChanged() {
        fragmentCountDownLatch?.countDown()
    }

    fun reset() {
        fragmentCountDownLatch = CountDownLatch(1)
    }

    fun waitForFragmentToBeAdded(): Boolean {
        return try {
            val result = fragmentCountDownLatch?.await(10, TimeUnit.SECONDS) ?: false
            result
        } catch (e: InterruptedException) {
            false
        }
    }
}
