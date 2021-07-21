/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.samples.photoeditor

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
import com.microsoft.device.display.samples.photoeditor.utils.ScreenInfoListenerImpl
import com.microsoft.device.dualscreen.ScreenManagerProvider
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Assert
import org.junit.Before
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
    private var screenInfoListener = ScreenInfoListenerImpl()

    @Before
    fun setup() {
        val screenManager = ScreenManagerProvider.getScreenManager()
        screenManager.addScreenInfoListener(screenInfoListener)
    }

    @After
    fun tearDown() {
        val screenManager = ScreenManagerProvider.getScreenManager()
        screenManager.removeScreenInfoListener(screenInfoListener)
        screenInfoListener.resetScreenInfo()
        screenInfoListener.resetScreenInfoCounter()
    }

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

        spanFromLeft()
        waitForScreenInfoAndAssert { Assert.assertTrue(isSpanned()) }

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
            if (Build.MODEL.contains("Emulator")) {
                "com.android.documentsui" // emulator apk package name
            } else {
                "com.google.android.documentsui" // device apk package name
            }

        // Open Files app
        val context = InstrumentationRegistry.getInstrumentation().context
        val intent = context.packageManager.getLaunchIntentForPackage(filesPackage)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
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
        closeRight()

        // Unlock rotation
        device.unfreezeRotation()
    }

    /**
     * HELPER FUNCTIONS FOR DUAL-SCREEN BEHAVIOR
     *
     * Use the functions below in your tests when testing dual-screen behaviors and transitions.
     * Run the "testSpanningHelperFunctions" test below to check that the methods produce
     * the expected behavior on your device.
     *
     * If the test fails, modify the swipe parameters as needed- usually either an increase in the
     * "steps" parameter or a slight shift in the "endX" parameter.
     *
     */

    /**
     * Runs helper functions and checks that they work as expected
     *
     * @precondition device in portrait mode, no other applications are open
     * (so app by default opens on left screen)
     */
    @Test
    fun testSpanningHelperFunctions() {
        spanFromLeft()
        assertThat(isSpanned(), iz(true))

        unspanToRight()
        assertThat(isSpanned(), iz(false))

        spanFromRight()
        assertThat(isSpanned(), iz(true))

        unspanToLeft()
        assertThat(isSpanned(), iz(false))

        switchToRight()
        switchToLeft()
        closeLeft()
    }

    companion object {
        // testing device
        val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Swipe constants
        const val leftX: Int = 675 // middle of left screen
        const val rightX: Int = 2109 // middle of right screen
        const val middleX: Int = 1350 // hinge area
        const val bottomY: Int = 1780 // bottom of screen
        const val middleY: Int = 900 // middle of screen
        const val spanSteps: Int = 400 // spanning swipe
        const val unspanSteps: Int = 200 // unspanning swipe
        const val switchSteps: Int = 100 // swipe to switch from one screen to the other
        const val closeSteps: Int = 50 // swipe to close app
    }

    private fun spanFromLeft() {
        device.swipe(leftX, bottomY, middleX, middleY, spanSteps)
    }

    private fun unspanToLeft() {
        device.swipe(rightX, bottomY, leftX, middleY, unspanSteps)
    }

    private fun spanFromRight() {
        device.swipe(rightX, bottomY, middleX, middleY, spanSteps)
    }

    private fun unspanToRight() {
        device.swipe(leftX, bottomY, rightX, middleY, unspanSteps)
    }

    private fun switchToLeft() {
        device.swipe(rightX, bottomY, leftX, middleY, switchSteps)
    }

    private fun switchToRight() {
        device.swipe(leftX, bottomY, rightX, middleY, switchSteps)
    }

    private fun closeLeft() {
        device.swipe(leftX, bottomY, leftX, middleY, closeSteps)
    }

    private fun closeRight() {
        device.swipe(rightX, bottomY, rightX, middleY, closeSteps)
    }

    private fun waitForScreenInfoAndAssert(assert: () -> Unit) {
        screenInfoListener.waitForScreenInfoChanges()
        assert()
        screenInfoListener.resetScreenInfo()
    }

    private fun isSpanned(): Boolean {
        return screenInfoListener.screenInfo?.isDualMode() == true
    }
}
