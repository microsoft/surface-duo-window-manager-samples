/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(taskEvents: TaskEvents) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = arrayListOf()
    private val listener: TaskEvents = taskEvents

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskAdapter.TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_row_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    override fun onBindViewHolder(holder: TaskAdapter.TaskViewHolder, position: Int) {
        holder.bind(tasks[position], listener)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(task: Task, listener: TaskEvents) {

            val textView = itemView.findViewById<TextView>(R.id.task_name)
            val checkBox = itemView.findViewById<CheckBox>(R.id.task_item_complete)

            // set text and apply strikethrough if complete
            textView.apply {

                if (task.complete) {
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    paintFlags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }
                text = task.name
            }

            checkBox.isChecked = task.complete

            // set up click listener for each item
            textView.setOnClickListener {
                listener.onTaskClicked(task)
            }

            // set up checkbox listener for each item
            checkBox.setOnClickListener {
                listener.onCheckBoxClicked(task, checkBox.isChecked)
            }
        }
    }

    fun setTasks(tasks: List<Task>) {
        this.tasks = tasks
        notifyDataSetChanged()
    }

    interface TaskEvents {
        fun onTaskClicked(task: Task)
        fun onCheckBoxClicked(task: Task, complete: Boolean)
    }
}
