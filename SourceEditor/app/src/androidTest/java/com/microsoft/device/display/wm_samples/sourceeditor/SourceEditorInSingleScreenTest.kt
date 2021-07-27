/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.sourceeditor

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import com.microsoft.device.display.wm_samples.sourceeditor.utils.SimpleFragmentBackStackListener
import com.microsoft.device.display.wm_samples.sourceeditor.utils.forceClick
import com.microsoft.device.display.wm_samples.sourceeditor.utils.setOrientationLeft
import com.microsoft.device.display.wm_samples.sourceeditor.utils.setOrientationRight
import com.microsoft.device.display.wm_samples.sourceeditor.utils.unfreezeRotation
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class SourceEditorInSingleScreenTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    private var backStackChangeListener = SimpleFragmentBackStackListener()

    @Before
    fun setup() {
        activityRule.activity.supportFragmentManager.addOnBackStackChangedListener(backStackChangeListener)
    }

    @After
    fun tearDown() {
        unfreezeRotation()
    }

    @Test
    fun previewWithoutOrientation() {
        onView(withId(R.id.btn_switch_to_editor)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_switch_to_preview)).check(matches(isDisplayed()))
        onView(withId(R.id.textinput_code)).check(matches(isDisplayed()))

        backStackChangeListener.reset()
        onView(withId(R.id.btn_switch_to_preview)).perform(forceClick())
        backStackChangeListener.waitForFragmentToBeAdded()
        onView(withId(R.id.webview_preview)).check(matches(isDisplayed()))
    }

    @Test
    fun previewWithOrientationLeft() {
        setOrientationLeft()
        previewWithoutOrientation()
    }

    @Test
    fun previewWithOrientationRight() {
        setOrientationRight()
        previewWithoutOrientation()
    }
}
