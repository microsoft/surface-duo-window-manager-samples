/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.sourceeditor.utils

import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

/**
 * A matcher that matches [View]s child's in parent matcher
 */
class ChildOfMatcher(private val parentMatcher: Matcher<View>, private val childMatcher: Matcher<View>) : TypeSafeMatcher<View>() {
    override fun describeTo(description: Description) {
        parentMatcher.describeTo(description)
        childMatcher.describeTo(description)
    }

    public override fun matchesSafely(view: View): Boolean {
        return hasParentThatMatches(view) && childMatcher.matches(view)
    }

    private fun hasParentThatMatches(view: View): Boolean {
        val parent = (view.parent ?: return false) as? ViewGroup ?: return parentMatcher.matches(view.parent)
        return parentMatcher.matches(parent) || hasParentThatMatches(parent)
    }
}
