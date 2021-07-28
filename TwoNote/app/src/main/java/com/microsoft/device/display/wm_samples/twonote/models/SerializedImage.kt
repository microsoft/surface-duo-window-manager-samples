/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.models

import java.io.Serializable

/**
 * Serializable format containing image data
 * Data can be used to recreate (deserialize) image bitmaps saved in memory
 *
 * @param name: unique file name associated with an image
 * @param image: compressed bitmap string defining an image
 * @param coords: x and y coordinates associated with an image
 * @param dimens: width and height associated with an image
 * @param rotated: rotation flag for an image - true if the image is rotated, false otherwise
 */
data class SerializedImage(
    val name: String,
    val image: String,
    val coords: List<Float> = listOf(),
    val dimens: List<Int> = listOf(),
    val rotated: Boolean
) : Serializable