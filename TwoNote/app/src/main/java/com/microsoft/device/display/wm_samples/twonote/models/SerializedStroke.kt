/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.models

import Defines.DEFAULT_THICKNESS
import android.media.Image
import com.microsoft.device.ink.InputManager
import java.io.Serializable

/**
 * Serializable format containing stroke data
 * Data can be used to recreate (deserialize) strokes saved in memory
 *
 * @param extendedStroke: a single stroke object
 * @param thicknessMultiplier: constant multiplier used for width calculations
 * @param rotated: rotation flag for stroke - true if rotated, false by default
 */
data class SerializedStroke(
    val extendedStroke: InputManager.ExtendedStroke = InputManager.ExtendedStroke(),
    val thicknessMultiplier: Int = DEFAULT_THICKNESS,
    val rotated: Boolean = false
) : Serializable
