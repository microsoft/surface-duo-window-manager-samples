/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.sourceeditor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/* ViewModel used to pass html data to preview fragment in real time */
class WebViewModel : ViewModel() {
    private var webFormState = MutableLiveData<String>()

    fun setText(htmlText: String) {
        webFormState.value = htmlText
    }

    fun getText(): LiveData<String> {
        return webFormState
    }
}
