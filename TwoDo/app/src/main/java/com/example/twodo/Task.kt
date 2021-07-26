/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "twodo_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    var uid: Long = 0,

    @ColumnInfo(name = "task_title")
    var name: String,

    @ColumnInfo(name = "task_notes")
    var notes: String,

    @ColumnInfo(name = "task_complete")
    var complete: Boolean
)
