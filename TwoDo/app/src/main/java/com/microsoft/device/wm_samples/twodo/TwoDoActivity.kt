/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 */

package com.microsoft.device.wm_samples.twodo

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.microsoft.device.wm_samples.twodo.databinding.ActivityTwoDoBinding

enum class TwoDoFragments {
    SPLASH, EDIT
}

class TwoDoActivity : AppCompatActivity(), TaskAdapter.TaskEvents {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var binding: ActivityTwoDoBinding

    // current fragment in FragmentContainerView
    private var currentFragment = TwoDoFragments.SPLASH

    companion object {
        const val FRAGMENT_NAME = "FRAGMENT_NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTwoDoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpRecyclerView()
        showSplashFragment()
        setupViewModelAndObserve()

        binding.fab.setOnClickListener {
            taskViewModel.editing = false
            showNewEditFragment()
        }
    }

    override fun onBackPressed() {
        binding.root.close()
        showSplashFragment()
        hideKeyboard()
    }

    override fun onTaskClicked(task: Task) {
        taskViewModel.editing = true
        taskViewModel.updatedSelectedTask(task)
        showNewEditFragment()
    }

    override fun onCheckBoxClicked(task: Task, complete: Boolean) {
        task.complete = complete
        taskViewModel.updateTask(task)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(FRAGMENT_NAME, currentFragment.name)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        var fragmentStr = savedInstanceState.getString(FRAGMENT_NAME)
        fragmentStr?.let {
            when (TwoDoFragments.valueOf(it)) {
                TwoDoFragments.SPLASH -> {
                    showSplashFragment()
                }
                TwoDoFragments.EDIT -> {
                    showNewEditFragment()
                }
            }
        }
    }

    private fun hideKeyboard() {
        // Check if no view has focus:
        val view = currentFocus
        if (view != null) {
            val inputManager =
                getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(
                view.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    private fun setUpRecyclerView() {
        taskAdapter = TaskAdapter(this)
        val rclList = findViewById<RecyclerView>(R.id.recycler_view)
        rclList.adapter = taskAdapter
        rclList.layoutManager = LinearLayoutManager(this)
        rclList.addItemDecoration(DividerItemDecoration(rclList.context, DividerItemDecoration.VERTICAL))
    }

    private fun setupViewModelAndObserve() {
        taskViewModel = ViewModelProvider(this).get(TaskViewModel::class.java)
        // when the task list changes, update the recyclerview
        taskViewModel.getTaskList().observe(
            this,
            Observer { task ->
                task.let { updateTaskList(task) }
            }
        )
    }

    private fun updateTaskList(tasks: List<Task>) {
        taskAdapter.setTasks(tasks)
    }

    private fun showSplashFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.task_container, SplashFragment())
        transaction.commit()
        currentFragment = TwoDoFragments.SPLASH
    }

    private fun showNewEditFragment() {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.task_container, TaskFragment())
        transaction.commit()
        binding.root.open()
        currentFragment = TwoDoFragments.EDIT
    }
}
