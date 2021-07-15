package com.microsoft.device.display.samples.sourceeditor

import android.app.Application
import com.microsoft.device.dualscreen.ScreenManagerProvider
import com.microsoft.device.dualscreen.fragmentshandler.FragmentManagerStateHandler

class SourceEditorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenManagerProvider.init(this)
        FragmentManagerStateHandler.init(this)
    }
}