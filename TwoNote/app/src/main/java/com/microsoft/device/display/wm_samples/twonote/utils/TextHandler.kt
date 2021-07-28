/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.utils

import android.app.Activity
import android.net.Uri
import com.google.android.material.textfield.TextInputEditText
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 *  Class used to read and process text files
 *
 *  @param activity: activity that can give access to content resolvers
 */
class TextHandler(private val activity: Activity) {

    /**
     * Read and display text from file located at specified uri
     *
     * @param uri: uri of file
     * @param textField: TextInputEditText to display file contents in
     */
    @Throws(IOException::class)
    fun processTextFileData(uri: Uri, textField: TextInputEditText) {
        val stringBuilder = StringBuilder()
        activity.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.appendln(line)
                    line = reader.readLine()
                }
            }
        }
        textField.setText(stringBuilder.toString())
    }
}