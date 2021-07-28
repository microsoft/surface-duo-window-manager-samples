/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.models

import java.io.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Serializable format containing inode data
 * INodes act as file descriptors and pointers to notes and categories in memory
 *
 * @param title: Name of note/category visible to user
 * @param dateModified: Last recorded date user modified the associated note/category
 * @param descriptor: Appropriate prefix for associated file
 *                      /n if file is a note, /c if file is a category
 * @param id: unique id of associated note/category
 */
class INode(
    var title: String,
    var dateModified: LocalDateTime = LocalDateTime.now(),
    val descriptor: String = "/n",
    var id: Int
) : Serializable {

    /**
     * Format and return the last modified date of the associated note/category
     *
     * @return last date the file was modified
     */
    fun dateModifiedString(): String {
        return dateModified.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
    }

    /**
     * Convert this inode to a string format
     *
     * @return the name of the note/category visible to the user
     */
    override fun toString(): String {
        return title
    }
}
