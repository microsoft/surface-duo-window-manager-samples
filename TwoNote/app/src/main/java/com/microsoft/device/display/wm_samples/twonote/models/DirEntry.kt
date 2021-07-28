/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.models

import java.io.Serializable

/**
 * Entry of references to files located within a specific directory
 * The reference directory is associated with the location of a specific DirEntry saved in memory
 *
 * @param inodes: collection of notes or categories associated with a specific directory
 */
data class DirEntry(
    val inodes: MutableList<INode> = mutableListOf()
) : Serializable