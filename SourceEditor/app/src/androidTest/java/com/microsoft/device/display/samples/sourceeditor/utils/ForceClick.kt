/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.sourceeditor.utils

import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf

/**
 * Click action for a view without to check the coordinates for the view on the screen. when device is rotated.
 */
class ForceClick : ViewAction {
    override fun getConstraints(): Matcher<View> {
        return allOf(isClickable(), isEnabled())
    }

    override fun getDescription(): String {
        return "force click"
    }

    override fun perform(uiController: UiController?, view: View?) {
        view?.performClick()
        uiController?.loopMainThreadForAtLeast(500)
    }
}