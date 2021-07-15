package com.microsoft.device.display.samples.sourceeditor.utils

import android.view.View
import androidx.test.espresso.ViewAction
import org.hamcrest.Matcher

/**
 * Returns A matcher that matches [View]s child's in parent matcher
 *
 * @return the matcher
 */
fun childOf(parentMatcher: Matcher<View>, childMatcher: Matcher<View>): Matcher<View> = ChildOfMatcher(parentMatcher, childMatcher)

/**
 * Returns an action that clicks the view without to check the coordinates on the screen.
 * Seems that ViewActions.click() finds coordinates of the view on the screen, and then performs the tap on the coordinates.
 * Seems tha changing the screen rotations affects these coordinates and ViewActions.click() throws exceptions.
 *
 * @return the force click action
 */
fun forceClick(): ViewAction = ForceClick()