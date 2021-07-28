package com.microsoft.device.display.wm_samples.twonote.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DualScreenViewModel : ViewModel() {
    private var isDualScreenState = MutableLiveData<Boolean>()

    var isDualScreen: Boolean
        set(value) {
            isDualScreenState.value = value
        }
        get() {
            return isDualScreenState.value ?: false
        }
}