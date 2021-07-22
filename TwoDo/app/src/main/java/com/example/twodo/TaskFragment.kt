/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.example.twodo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.slidingpanelayout.widget.SlidingPaneLayout

class TaskFragment : Fragment(), View.OnClickListener {

    private val taskViewModel: TaskViewModel by activityViewModels()

    private lateinit var title: TextView
    private lateinit var taskName: EditText
    private lateinit var taskNotes: EditText
    private lateinit var taskComplete: CheckBox

    private lateinit var task: Task

    lateinit var slidingPane: SlidingPaneLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_task, container, false)

        // task fields
        title = view.findViewById<TextView>(R.id.create_edit_title)
        taskName = view.findViewById<EditText>(R.id.task_name_input)
        taskNotes = view.findViewById<EditText>(R.id.task_notes_input)
        taskComplete = view.findViewById<CheckBox>(R.id.task_complete)

        // get slidingpanelayout
        slidingPane = requireActivity().findViewById(R.id.sliding_pane_layout)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // if editing, get selected task and update fields
        if (taskViewModel.editing) {
            taskViewModel.selectedTaskUid?.let {
                taskViewModel.getTask(it).observe(
                    viewLifecycleOwner,
                    Observer<Task> { item ->
                        // Update the UI using new item data
                        title.setText(R.string.edit_task_title)
                        task = item
                        taskName.setText(task.name)
                        taskNotes.setText(task.notes)
                        taskComplete.isChecked = task.complete
                    }
                )
            }
        }

        val saveButton = view.findViewById<Button>(R.id.save_button)
        saveButton.setOnClickListener(this)

        val deleteButton = view.findViewById<Button>(R.id.delete_button)
        deleteButton.setOnClickListener(this)
        // delete button only visible when editing
        if (taskViewModel.editing) {
            deleteButton.visibility = View.VISIBLE
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.save_button -> {
                if (taskName.text.isEmpty()) {
                    Toast.makeText(
                        activity?.applicationContext,
                        R.string.invalid_entry,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {

                    if (taskViewModel.editing) {
                        task.name = taskName.text.toString()
                        task.notes = taskNotes.text.toString()
                        task.complete = taskComplete.isChecked
                        taskViewModel.updateTask(task)
                        taskViewModel.editing = false
                    } else {
                        val task = Task(
                            name = taskName.text.toString(),
                            notes = taskNotes.text.toString(),
                            complete = taskComplete.isChecked
                        )
                        taskViewModel.insertTask(task)
                    }
                    activity?.onBackPressed()
                }
            }
            R.id.delete_button -> {
                taskViewModel.deleteTask(task)
                activity?.onBackPressed()
            }
        }
    }
}
