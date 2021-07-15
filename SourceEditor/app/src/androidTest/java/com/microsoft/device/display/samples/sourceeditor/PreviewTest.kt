/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.sourceeditor

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.web.assertion.WebViewAssertions.webMatches
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import androidx.test.espresso.web.webdriver.DriverAtoms.getText
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.filters.MediumTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import com.microsoft.device.display.samples.sourceeditor.utils.ScreenInfoListenerImpl
import com.microsoft.device.display.samples.sourceeditor.utils.moveToLeft
import com.microsoft.device.display.samples.sourceeditor.utils.moveToRight
import com.microsoft.device.display.samples.sourceeditor.utils.spanFromLeft
import com.microsoft.device.display.samples.sourceeditor.utils.spanFromRight
import com.microsoft.device.display.samples.sourceeditor.utils.switchFromSingleToDualScreen
import com.microsoft.device.display.samples.sourceeditor.utils.unspanToLeft
import com.microsoft.device.display.samples.sourceeditor.utils.unspanToRight
import com.microsoft.device.dualscreen.ScreenManagerProvider
import org.hamcrest.core.StringContains.containsString
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4ClassRunner::class)
class PreviewTest {
    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java, false, false)
    private var screenInfoListener = ScreenInfoListenerImpl()
    private val testString = "Testing in a different browser"

    @Before
    fun setup() {
        val screenManager = ScreenManagerProvider.getScreenManager()
        screenManager.addScreenInfoListener(screenInfoListener)
        activityRule.launchActivity(null)
    }

    @After
    fun tearDown() {
        val screenManager = ScreenManagerProvider.getScreenManager()
        screenManager.removeScreenInfoListener(screenInfoListener)
        screenInfoListener.resetScreenInfo()
        screenInfoListener.resetScreenInfoCounter()
        activityRule.finishActivity()
    }

    @Test
    fun previewTextInSingleScreenMode() {
        screenInfoListener.waitForScreenInfoChanges()

        onView(withId(R.id.btn_switch_to_preview)).perform(click())
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "h1"))
                .check(webMatches(getText(), containsString("Testing in a browser")))
    }

    @Test
    fun previewTextInDualScreenMode() {
        screenInfoListener.waitForScreenInfoChanges()

        onView(withId(R.id.textinput_code)).perform(replaceText("<h1>$testString</h1>"))
        switchFromSingleToDualScreen()
        assert(isSpanned())
        onWebView()
                .withElement(findElement(Locator.TAG_NAME, "h1"))
                .check(webMatches(getText(), containsString(testString)))
    }

    @Test
    fun testSpanning() {
        screenInfoListener.waitForScreenInfoChanges()
        screenInfoListener.resetScreenInfo()

        spanFromLeft()
        waitForScreenInfoAndAssert { assertTrue(isSpanned()) }
        unspanToRight()
        waitForScreenInfoAndAssert { assertFalse(isSpanned()) }
        spanFromRight()
        waitForScreenInfoAndAssert { assertTrue(isSpanned()) }
        unspanToLeft()
        waitForScreenInfoAndAssert { assertFalse(isSpanned()) }
        moveToRight()
        moveToLeft()
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
