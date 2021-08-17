/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.microsoft.device.wm_samples.twodo.databinding.FragmentTaskBinding

class TaskFragment : Fragment(), View.OnClickListener {

    private val taskViewModel: TaskViewModel by activityViewModels()

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var task: Task

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        val view = binding.root
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
                        binding.createEditTitle.setText(R.string.edit_task_title)
                        task = item
                        binding.taskNameInput.setText(task.name)
                        binding.taskNotesInput.setText(task.notes)
                        binding.taskComplete.isChecked = task.complete
                    }
                )
            }
        }

        binding.saveButton.setOnClickListener(this)
        binding.deleteButton.setOnClickListener(this)

        // delete button only visible when editing
        if (taskViewModel.editing) {
            binding.deleteButton.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.save_button -> {
                if (binding.taskNameInput.text.isEmpty()) {
                    Toast.makeText(
                        activity?.applicationContext,
                        R.string.invalid_entry,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {

                    if (taskViewModel.editing) {
                        task.name = binding.taskNameInput.text.toString()
                        task.notes = binding.taskNotesInput.text.toString()
                        task.complete = binding.taskComplete.isChecked
                        taskViewModel.updateTask(task)
                        taskViewModel.editing = false
                    } else {
                        val task = Task(
                            name = binding.taskNameInput.text.toString(),
                            notes = binding.taskNotesInput.text.toString(),
                            complete = binding.taskComplete.isChecked
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
