/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

import android.graphics.Matrix

/**
 * Object used to define constant variables used throughout the rest of the application
 */
object Defines {
    // Fragment name constants //
    const val DETAIL_FRAGMENT = "detail"
    const val TAG_DELIMITER = "_"
    const val FIRST_CONTAINER = "first"
    const val SECOND_CONTAINER = "second"
    const val LIST_FRAGMENT = "list"
    const val GET_STARTED_FRAGMENT = "started"
    const val INODE = "inode"
    const val NOTE = "note"

    // Instance state constants //
    const val LIST_VIEW = "list view"

    // Pen Event constants //
    const val DEFAULT_THICKNESS = 25
    const val ERASER_RADIUS = 50f
    const val OPAQUE = 255
    const val TRANSPARENT = 100

    // File Handler references //
    const val IMAGE_PREFIX = "image/"
    const val TEXT_PREFIX = "text/"

    // Image Handler constants //
    const val MIN_DIMEN = 250
    const val RENDER_TIMER = 50L
    const val RESIZE_SPEED = 15
    const val THRESHOLD = 0.2

    // Rotation scaling constants //
    // Each screen is 1800 by 1350 px:
    //     --> PORT_TO_LAND = 1800 / 1350 = 4/3
    //     --> LAND_TO_PORT = 1350 / 1800 = 3/4
    const val SCALE_RATIO = 4f / 3
    val LAND_TO_PORT = Matrix().apply {
        postScale(1 / SCALE_RATIO, 1 / SCALE_RATIO)
    }
    val PORT_TO_LAND = Matrix().apply {
        postScale(SCALE_RATIO, SCALE_RATIO)
    }

    // even with no toolbar, the hinge is offset by a default amount
    const val DEFAULT_TOOLBAR_OFFSET = 18
}
