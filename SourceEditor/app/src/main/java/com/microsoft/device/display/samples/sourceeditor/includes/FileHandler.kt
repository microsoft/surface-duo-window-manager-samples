/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.sourceeditor.includes

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.view.DragEvent
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import com.microsoft.device.display.samples.sourceeditor.viewmodel.WebViewModel
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.IOException
import java.nio.charset.Charset

/* Class used to make file read/write requests */
class FileHandler(
    private val activity: Activity,
    private val webVM: WebViewModel,
    private val contentResolver: ContentResolver
) {
    companion object {
        // intent request codes
        const val CREATE_FILE = 1
        const val PICK_TXT_FILE = 2
    }

    // create a window prompting user to save a new file
    // defaults to public Downloads folder if uri is empty
    fun createFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Defines.PLAIN_TEXT
            putExtra(Intent.EXTRA_TITLE, Defines.DEFAULT_FILE_NAME)

            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(activity, intent, CREATE_FILE, null)
    }

    // creating a window prompting user to choose a file to open
    // defaults to public Downloads folder if uri is empty
    fun openFile(pickerInitialUri: Uri) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = Defines.PLAIN_TEXT

            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }

        startActivityForResult(activity, intent, PICK_TXT_FILE, null)
    }

    // read text from file specified in uri path
    @Throws(IOException::class)
    fun processFileData(uri: Uri, event: DragEvent?) {
        val stringBuilder = StringBuilder()

        event?.let { ActivityCompat.requestDragAndDropPermissions(activity, event) }
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    stringBuilder.append(System.getProperty("line.separator"))
                    line = reader.readLine()
                }
            }
        }
        return webVM.setText(stringBuilder.toString())
    }

    // overwrite text from file specified in uri path
    fun alterDocument(uri: Uri) {
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    val charset: Charset = Charsets.UTF_8
                    stream.write(
                        webVM.getText().value.toString()
                            .toByteArray(charset)
                    )
                }
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}