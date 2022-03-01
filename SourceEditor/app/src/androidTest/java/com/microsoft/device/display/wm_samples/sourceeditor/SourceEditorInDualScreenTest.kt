package com.microsoft.device.display.wm_samples.sourceeditor

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.microsoft.device.display.wm_samples.sourceeditor.utils.childOf
import com.microsoft.device.dualscreen.testing.spanFromStart
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
@MediumTest
class SourceEditorInDualScreenTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @After
    fun tearDown() {
        device.unfreezeRotation()
    }

    @Test
    fun previewWithoutOrientation() {
        device.spanFromStart()
        testPreview()
    }

    @Test
    fun previewWithOrientationLeft() {
        device.spanFromStart()
        device.setOrientationLeft()
        testPreview()
    }

    @Test
    fun previewWithOrientationRight() {
        device.spanFromStart()
        device.setOrientationRight()
        testPreview()
    }

    private fun testPreview() {
        onView(
            childOf(withId(R.id.primary_fragment_container), withId(R.id.btn_switch_to_editor))
        ).check(matches(Matchers.not(ViewMatchers.isDisplayed())))
        onView(
            childOf(withId(R.id.primary_fragment_container), withId(R.id.btn_switch_to_preview))
        ).check(matches(Matchers.not(ViewMatchers.isDisplayed())))
        onView(withId(R.id.textinput_code)).check(matches(ViewMatchers.isDisplayed()))
        onView(withId(R.id.webview_preview)).check(matches(ViewMatchers.isDisplayed()))
    }
}
