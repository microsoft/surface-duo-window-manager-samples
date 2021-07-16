/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.sourceeditor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.window.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Consumer
import androidx.lifecycle.ViewModelProvider
import androidx.window.FoldingFeature
import androidx.window.WindowLayoutInfo
import com.microsoft.device.display.samples.sourceeditor.includes.FileHandler
import com.microsoft.device.display.samples.sourceeditor.viewmodel.WebViewModel
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var windowManager: WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val mainThreadExecutor = Executor { r: Runnable -> mainHandler.post(r)}
    private val wmCallback = WMCallback()

    private lateinit var fileBtn: ImageView
    private lateinit var saveBtn: ImageView

    private lateinit var fileHandler: FileHandler
    private lateinit var webVM: WebViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowManager = WindowManager(this)
        setContentView(R.layout.activity_main)

        webVM = ViewModelProvider(this).get(WebViewModel::class.java)
        fileHandler = FileHandler(this, webVM, contentResolver)

        // display action toolbar
        this.supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.component_action_toolbar)

        // action toolbar functionality
        fileBtn = findViewById(R.id.btn_file)
        fileBtn.setOnClickListener {
            fileHandler.openFile(Uri.EMPTY)
        }

        saveBtn = findViewById(R.id.btn_save)
        saveBtn.setOnClickListener {
            fileHandler.createFile(Uri.EMPTY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        // request to save a file has been made, add data to newly created file
        if (requestCode == FileHandler.CREATE_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                fileHandler.alterDocument(uri)
            }
        }
        // request to load file contents has been made, process the file's contents
        else if (requestCode == FileHandler.PICK_TXT_FILE && resultCode == Activity.RESULT_OK) {
            resultData?.data?.also { uri ->
                fileHandler.processFileData(uri, null)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        windowManager.registerLayoutChangeCallback(mainThreadExecutor, wmCallback)
    }

    override fun onStop() {
        super.onStop()
        windowManager.unregisterLayoutChangeCallback(wmCallback)
    }

    // Jetpack WM callback
    inner class WMCallback : Consumer<WindowLayoutInfo> {
        override fun accept(newLayoutInfo: WindowLayoutInfo?) {
            // Add views that represent display features
            newLayoutInfo?.let {
                for (displayFeature in it.displayFeatures) {
                    val foldingFeature = displayFeature as? FoldingFeature
                    if (foldingFeature != null) {
                        // do nothing for now?
                    }
                }
            }
        }
    }
}