/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = true)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun getTaskDao(): TaskDao

    companion object {
        val databaseName = "twodo_database"
        var taskDatabase: TaskDatabase? = null

        fun getInstance(context: Context): TaskDatabase? {
            if (taskDatabase == null) {
                taskDatabase = Room.databaseBuilder(
                    context,
                    TaskDatabase::class.java,
                    TaskDatabase.databaseName
                )
                    .allowMainThreadQueries()
                    .build()
            }
            return taskDatabase
        }
    }
}
