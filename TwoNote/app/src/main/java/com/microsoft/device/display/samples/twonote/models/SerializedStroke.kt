/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.models

import Defines.DEFAULT_THICKNESS
import java.io.Serializable

/**
 * Serializable format containing stroke data
 * Data can be used to recreate (deserialize) strokes saved in memory
 *
 * @param xList: collection of x coordinates in stroke
 * @param yList: collection of y coordinates in stroke
 * @param pressureList: collection of pressures associated with each path in stroke
 * @param paintColor: int value of color associated with stroke
 * @param thicknessMultiplier: constant multiplier used for width calculations
 * @param rotated: rotation flag for stroke - true if rotated, false by default
 * @param highlightStroke: highlighter flag for stroke - true if transparent, false by default
 */
data class SerializedStroke(
    val xList: List<MutableList<Float>> = listOf(),
    val yList: List<MutableList<Float>> = listOf(),
    val pressureList: List<MutableList<Float>> = listOf(),
    val paintColor: Int = 0,
    val thicknessMultiplier: Int = DEFAULT_THICKNESS,
    val rotated: Boolean = false,
    val highlightStroke: Boolean = false
) : Serializable
