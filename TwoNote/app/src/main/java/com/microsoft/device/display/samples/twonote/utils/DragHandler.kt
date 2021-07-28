/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.samples.twonote.utils

import Defines.IMAGE_PREFIX
import Defines.TEXT_PREFIX
import android.content.ContentResolver
import android.view.DragEvent
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.microsoft.device.display.samples.twonote.MainActivity
import com.microsoft.device.display.samples.twonote.fragments.NoteDetailFragment
import com.microsoft.device.display.samples.twonote.models.SerializedImage

/**
 *  Class used to update app contents when an appropriate drag event occurs
 *
 *  @param fragment: NoteDetailFragment in which drag event results should be displayed
 */
class DragHandler(private val fragment: NoteDetailFragment) {
    private val imageHandler = ImageHandler(fragment)
    private val textHandler = TextHandler(fragment.requireActivity())

    /**
     * After drag event has occurred, handle the event if it's relevant to app
     *
     * @param event: drag event that occurred
     * @return true if valid drag event, false otherwise
     */
    fun onDrag(event: DragEvent): Boolean {
        val isText = event.clipDescription?.getMimeType(0).toString().startsWith(TEXT_PREFIX)
        val isImage = event.clipDescription?.getMimeType(0).toString().startsWith(IMAGE_PREFIX)
        val isRotated = MainActivity.isRotated(fragment.requireContext())

        return when (event.action) {
            DragEvent.ACTION_DROP -> processDrop(event, isText, isImage, isRotated)

            DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_STARTED,
            DragEvent.ACTION_DRAG_LOCATION, DragEvent.ACTION_DRAG_ENDED,
            DragEvent.ACTION_DRAG_EXITED -> true

            else -> false
        }
    }

    /**
     * Process drop event if clip data is a text or image file
     *
     * @param event: drop event
     * @param isText: true if item type is text, false otherwise
     * @param isImage: true if item type is image, false otherwise
     * @param isRotated: true if device is was rotated at the time of the drop event, false otherwise
     *
     * @return true if drop was processed, false otherwise
     */
    private fun processDrop(event: DragEvent, isText: Boolean, isImage: Boolean, isRotated: Boolean): Boolean {
        val item = event.clipData?.getItemAt(0)

        item?.let {
            val uri = item.uri

            if (uri != null) {
                if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
                    // Request permission to read file
                    ActivityCompat.requestDragAndDropPermissions(fragment.activity, event)

                    when {
                        isText -> {
                            textHandler.processTextFileData(uri, fragment.noteText)
                            fragment.changeEditingMode(NoteDetailFragment.EditingMode.Text)
                            return true
                        }
                        isImage -> {
                            imageHandler.addImageToView(uri, isRotated)
                            fragment.changeEditingMode(NoteDetailFragment.EditingMode.Image)
                            return true
                        }
                        else -> {
                            // Dropped item type not supported
                            return false
                        }
                    }
                }
            } else {
                // Item from inside the app was dragged and dropped
                return imageHandler.handleReposition(event)
            }
        }
        return false
    }

    /**
     * Initialize ImageHandler with serialized image data
     *
     * @param list: list to initialize ImageHandler with
     * @param isRotated: true if device is currently rotated, false otherwise
     */
    fun setImageList(list: List<SerializedImage>, isRotated: Boolean) {
        imageHandler.setImageList(list, isRotated)
    }

    /**
     * Get list of images in serialized format
     *
     * @return list of serialized images
     */
    fun getImageList(): List<SerializedImage> {
        return imageHandler.getImageList()
    }

    /**
     * Get list of images in view format
     *
     * @return list of ImageView objects
     */
    fun getImageViewList(): List<ImageView> {
        return imageHandler.getImageViewList()
    }

    /**
     * Change ability to delete images on touch
     *
     * @param value: if true, enable deletion, if false, disable deletion
     */
    fun setDeleteMode(value: Boolean) {
        imageHandler.setDeleteMode(value)
    }

    /**
     * Remove all images from the view
     */
    fun clearImages() {
        for (image in imageHandler.getImageViewList()) {
            fragment.view?.let {
                fragment.imageContainer.removeView(image)
            }
        }
    }
}