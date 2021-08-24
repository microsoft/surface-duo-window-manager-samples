/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.hasToString
import org.junit.After
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4ClassRunner::class)
class FragmentNavigationTest {
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule
    val activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    @After
    fun resetOrientation() {
        device.setOrientationNatural()
        device.unfreezeRotation()
    }

    @Test
    fun test1_createNoteInSingleMode() {
        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.add_fab)).perform(click())

        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))

        spanApplication()

        onView(withId(R.id.note_list_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))

        rotateDevice()
        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun test2_createNoteInDualMode() {
        spanApplication()

        onView(withId(R.id.note_list_layout)).check(matches(isDisplayed()))

        onView(withId(R.id.add_fab)).check(matches(isDisplayed()))
        onView(withId(R.id.add_fab)).perform(click())

        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))

        rotateDevice()
        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun test3_openNoteFromList() {
        spanApplication()
        onView(withId(R.id.note_list_layout)).check(matches(isDisplayed()))

        onData(hasToString("Note 1"))
            .inAdapterView(withId(R.id.list_view))
            .perform(click())

        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.title_input), withText("Note 1"))).check(matches(isDisplayed()))

        rotateDevice()
        onView(withId(R.id.note_detail_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun test4_addCategoryWithoutNotes() {
        onView(withId(R.id.note_list_layout)).check(matches(isDisplayed()))

        spanApplication()

        openActionBarOverflowOrOptionsMenu(InstrumentationRegistry.getInstrumentation().targetContext)
        onView(withText(R.string.action_add_category)).check(matches(isDisplayed()))
        onView(withText(R.string.action_add_category)).perform(click())

        onView(withId(R.id.note_list_layout)).check(matches(isDisplayed()))
        onView(withId(R.id.get_started_layout)).check(matches(isDisplayed()))

        rotateDevice()
        onView(withId(R.id.note_list_layout)).check(matches(isDisplayed()))
    }

    private fun spanApplication() {
        device.swipe(675, 1780, 1350, 900, 400)
    }

    private fun rotateDevice() {
        device.setOrientationLeft()
    }
}
