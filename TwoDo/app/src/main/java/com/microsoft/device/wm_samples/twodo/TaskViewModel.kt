/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private var taskRepository = TaskRepository(application)

    var selectedTaskUid: Long? = null
        private set

    var editing: Boolean = false

    fun getTaskList(): LiveData<List<Task>> {
        return taskRepository.getTasks()
    }

    fun getTask(uid: Long): LiveData<Task> {
        return taskRepository.getTask(uid)
    }

    fun insertTask(task: Task) {
        viewModelScope.launch {
            taskRepository.insert(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.delete(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskRepository.update(task)
        }
    }

    // keep track of selected task
    fun updatedSelectedTask(task: Task) {
        selectedTaskUid = task.uid
    }
}
