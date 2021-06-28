/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.foldingvideo

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.Description


@RunWith(AndroidJUnit4::class)
class UITests {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun withHeight(size: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            public override fun matchesSafely(view: View): Boolean {
                return view.height == size
            }
            override fun describeTo(description: Description) {
                description.appendText("View should have height $size")
            }

        }
    }

    fun notWithHeight(size: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            public override fun matchesSafely(view: View): Boolean {
                return view.height != size
            }
            override fun describeTo(description: Description) {
                description.appendText("View shouldn't have height $size")
            }

        }
    }

    fun withWidth(size: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            public override fun matchesSafely(view: View): Boolean {
                return view.width == size
            }
            override fun describeTo(description: Description) {
                description.appendText("View should have height $size")
            }

        }
    }

    fun notWithWidth(size: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            public override fun matchesSafely(view: View): Boolean {
                return view.width != size
            }
            override fun describeTo(description: Description) {
                description.appendText("View shouldn't have height $size")
            }

        }
    }

    @Test
    fun dual_landscape() {

        //span app
        device.swipe(675, 1780, 1350, 900, 400)

        //check bottom background is 0 height
        onView(withId(R.id.horiz_background)).check(matches(withHeight(0)))

        //rotate to dual landscape
        device.setOrientationLeft()
        device.unfreezeRotation()

        //check bottom background is non 0 height
        onView(withId(R.id.horiz_background)).check(matches(notWithHeight(0)))

        //reset rotation
        device.setOrientationNatural()
        device.unfreezeRotation()
    }

    @Test
    fun dual_portrait_split_ctrls() {

        //span app
        device.swipe(675, 1780, 1350, 900, 400)

        //check right background is 0 width
        onView(withId(R.id.vert_background)).check(matches(withWidth(0)))

        //click split control button
        onView(withId(R.id.fab)).perform(click())

        //check right background is non 0 width
        onView(withId(R.id.vert_background)).check(matches(notWithWidth(0)))

        //click split control button
        onView(withId(R.id.fab)).perform(click())

        //check right background is 0 width
        onView(withId(R.id.vert_background)).check(matches(withWidth(0)))
    }

    @Test
    fun dual_portrait_split_ctrls_saved() {
        //span app
        device.swipe(675, 1780, 1350, 900, 400)

        //check right background is 0 width
        onView(withId(R.id.vert_background)).check(matches(withWidth(0)))

        //click split control button
        onView(withId(R.id.fab)).perform(click())

        //check right background is non 0 width
        onView(withId(R.id.vert_background)).check(matches(notWithWidth(0)))

        //rotate to dual landscape
        device.setOrientationLeft()
        device.unfreezeRotation()
        //rotate back
        device.setOrientationNatural()
        device.unfreezeRotation()

        //check right background is non 0 width - split ctrls have saved
        onView(withId(R.id.vert_background)).check(matches(notWithWidth(0)))
    }
}

