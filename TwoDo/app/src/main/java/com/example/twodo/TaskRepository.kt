/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import android.app.Application
import androidx.lifecycle.LiveData

class TaskRepository(application: Application) {

    private val taskDao: TaskDao
    private val tasks: LiveData<List<Task>>

    init {
        val database = TaskDatabase.getInstance(application.applicationContext)
        taskDao = database!!.getTaskDao()
        tasks = taskDao.getTwoDoList()
    }

    fun getTasks(): LiveData<List<Task>> {
        return tasks
    }

    fun getTask(uid: Long): LiveData<Task> {
        return taskDao.getTask(uid)
    }

    suspend fun insert(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun update(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun delete(task: Task) {
        taskDao.deleteTask(task)
    }
}
