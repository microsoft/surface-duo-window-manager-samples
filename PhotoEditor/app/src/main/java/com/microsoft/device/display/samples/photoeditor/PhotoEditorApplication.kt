/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.samples.photoeditor

import android.app.Application
import com.microsoft.device.dualscreen.ScreenManagerProvider

class PhotoEditorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenManagerProvider.init(this)
    }
}
