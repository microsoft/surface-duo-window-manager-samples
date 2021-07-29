/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.wm_samples.photoeditor

import android.graphics.drawable.Drawable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PhotoEditorViewModel : ViewModel() {
    companion object {
        // Default property value for ImageFilterView attributes (state of original image)
        const val DEFAULT_ATTRIBUTE_VALUE = 1f

        // Default property value for ImageFilterView attributes (state of original image)
        private const val DEFAULT_SELECTED_CONTROL = 0
    }

    private var image = MutableLiveData<Drawable>()
    private var _saturation = MutableLiveData<Float>()
    private var _brightness = MutableLiveData<Float>()
    private var _warmth = MutableLiveData<Float>()
    private var _selectedControl = MutableLiveData<Int>()
    private var _isDualScreen = MutableLiveData<Boolean>()

    fun updateImage(newImage: Drawable) {
        image.value = newImage
    }

    fun getImage(): MutableLiveData<Drawable> {
        return image
    }

    var saturation: Float
        set(value) {
            _saturation.value = value
        }
        get() {
            return _saturation.value ?: DEFAULT_ATTRIBUTE_VALUE
        }

    var brightness: Float
        set(value) {
            _brightness.value = value
        }
        get() {
            return _brightness.value ?: DEFAULT_ATTRIBUTE_VALUE
        }

    var warmth: Float
        set(value) {
            _warmth.value = value
        }
        get() {
            return _warmth.value ?: DEFAULT_ATTRIBUTE_VALUE
        }
    var selectedControl: Int
        set(value) {
            _selectedControl.value = value
        }
        get() {
            return _selectedControl.value ?: DEFAULT_SELECTED_CONTROL
        }

    var isDualScreen: Boolean
        set(value) {
            _isDualScreen.value = value
        }
        get() {
            return _isDualScreen.value ?: false
        }

    fun resetValues() {
        _brightness.value = DEFAULT_ATTRIBUTE_VALUE
        _saturation.value = DEFAULT_ATTRIBUTE_VALUE
        _warmth.value = DEFAULT_ATTRIBUTE_VALUE
    }
}
