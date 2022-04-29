/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.wm_samples.photoeditor

import android.content.Intent
import android.os.Build
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.microsoft.device.dualscreen.testing.closeEnd
import com.microsoft.device.dualscreen.testing.getDeviceModel
import com.microsoft.device.dualscreen.testing.spanFromStart
import org.hamcrest.CoreMatchers.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.hamcrest.CoreMatchers.`is` as iz

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class PhotoEditorUITest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    /**
     * Tests visibility of controls when app spanned vs. unspanned
     *
     * @precondition device in portrait mode, no other applications are open
     * (so app by default opens on left screen)
     */
    @Test
    fun testControlVisibility() {
        // Lock in portrait mode
        device.setOrientationNatural()

        // App opens in single-screen mode, so dropdown and saturation slider should be visible while brightness and warmth sliders are hidden
        onView(withId(R.id.controls)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.saturation)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.brightness)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))
        onView(withId(R.id.warmth)).check(matches(withEffectiveVisibility(Visibility.INVISIBLE)))

        device.spanFromStart()

        // Switched to dual-screen mode, so dropdown should not exist and all sliders should be visible
        onView(withId(R.id.controls)).check(doesNotExist())
        onView(withId(R.id.saturation)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.brightness)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        onView(withId(R.id.warmth)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // Unlock rotation
        device.unfreezeRotation()
    }

    /**
     * Tests drag and drop capabilities of PhotoEditor
     *
     * @precondition device in portrait mode, no other applications are open
     * (so app by default opens on left screen)
     */
    @Test
    fun testDragAndDrop() {
        // Lock in portrait mode
        device.setOrientationNatural()

        // Rotate and save image file
        onView(withId(R.id.rotate_left)).perform(click())
        onView(withId(R.id.save)).perform(click())

        // Reset to unrotated image
        onView(withId(R.id.rotate_right)).perform(click())
        val prev = activityRule.activity.findViewById<ImageFilterView>(R.id.image).drawable

        val filesPackage =
            if (Build.MODEL.contains("Emulator") || Build.MODEL.contains("Image")) {
                "com.android.documentsui" // emulator apk package name
            } else {
                "com.google.android.documentsui" // device apk package name
            }

        // Open Files app
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = targetContext.packageManager.getLaunchIntentForPackage(filesPackage)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT)
        }
        targetContext.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(filesPackage).depth(0)), 3000) // timeout at 3 seconds

        // Before import, drawable is equal to prev
        assertThat(
            prev,
            iz(activityRule.activity.findViewById<ImageFilterView>(R.id.image).drawable)
        )

        // Hardcoded to select most recently saved file in Files app - must be an image file
        device.swipe(1550, 1230, 1550, 1230, 100)

        // Slowly drag selected image file to other screen for import
        device.swipe(1550, 1230, 1200, 1100, 600)

        // After import, drawable has changed
        onIdle()
        assertThat(
            prev,
            iz(not(activityRule.activity.findViewById<ImageFilterView>(R.id.image).drawable))
        )

        // Close Files app
        device.closeEnd()
        // Unlock rotation
        device.unfreezeRotation()
    }
}
