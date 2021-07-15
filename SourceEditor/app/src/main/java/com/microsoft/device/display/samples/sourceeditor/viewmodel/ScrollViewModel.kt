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

/* ViewModel used to pass scroll values between fragments in real time */
class ScrollViewModel : ViewModel() {
    private var scrollPercentage = MutableLiveData<ScrollState>()

    fun setScroll(scrollKey: String, scrollVal: Int) {
        scrollPercentage.value = ScrollState(scrollKey, scrollVal)
    }

    fun getScroll(): LiveData<ScrollState> {
        return scrollPercentage
    }
}