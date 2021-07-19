package com.microsoft.device.display.samples.sourceeditor.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class DualScreenViewModel : ViewModel() {
    private var isDualScreenState = MutableLiveData<Boolean>()

    fun setIsDualScreen(isDualScreen: Boolean) {
        isDualScreenState.value = isDualScreen
    }

    fun getIsDualScreen(): LiveData<Boolean> {
        return isDualScreenState
    }
}