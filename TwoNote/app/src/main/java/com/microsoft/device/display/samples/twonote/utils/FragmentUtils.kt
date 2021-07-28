/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.utils

import Defines.DETAIL_FRAGMENT
import Defines.FIRST_CONTAINER
import Defines.SECOND_CONTAINER
import Defines.TAG_DELIMITER
import com.microsoft.device.display.samples.twonote.R

/**
 * Build the unique tag for the detail view fragment based on the container and the content
 *
 * @param container: container to start fragment in
 * @param inodeId: ID of inode associated with note to display in fragment
 * @param noteId: ID of note to display in fragment
 */
fun buildDetailTag(container: Int, inodeId: Int, noteId: Int): String {
    val result = StringBuilder()
    if (container == R.id.first_container_id) {
        result.append(FIRST_CONTAINER)
    } else if (container == R.id.second_container_id) {
        result.append(SECOND_CONTAINER)
    }
    result.append(TAG_DELIMITER)
    result.append(DETAIL_FRAGMENT)
    result.append(TAG_DELIMITER)
    result.append(inodeId)
    result.append(TAG_DELIMITER)
    result.append(noteId)

    return result.toString()
}
