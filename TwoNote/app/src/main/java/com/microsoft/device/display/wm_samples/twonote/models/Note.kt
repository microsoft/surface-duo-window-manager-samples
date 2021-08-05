/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.models

import com.microsoft.device.ink.InputManager
import java.io.Serializable

/**
 * Serializable format containing note data
 * Notes define the text, images, and drawings contained in an instance of a NoteDetailFragment
 *
 * @param id: unique identification number associated with this note
 * @param localizedTitle: title of the note - use a localized string for default note name
 */
class Note(var id: Int, localizedTitle: String) : Serializable {
    var title: String = "$localizedTitle $id"
    var text: String = ""
    var drawings: List<InputManager.ExtendedStroke> = listOf()
    var images: List<SerializedImage> = listOf()
}
