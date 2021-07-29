/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.utils

import Defines.LAND_TO_PORT
import Defines.MIN_DIMEN
import Defines.PORT_TO_LAND
import Defines.RENDER_TIMER
import Defines.RESIZE_SPEED
import Defines.SCALE_RATIO
import Defines.THRESHOLD
import android.annotation.SuppressLint
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.drawToBitmap
import com.microsoft.device.display.wm_samples.twonote.MainActivity
import com.microsoft.device.display.wm_samples.twonote.fragments.NoteDetailFragment
import com.microsoft.device.display.wm_samples.twonote.models.SerializedImage
import java.io.ByteArrayOutputStream
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Class that handles reading and displaying image files
 *
 * @param fragment: NoteDetailFragment in which images should be displayed
 */
class ImageHandler(private val fragment: NoteDetailFragment) {
    // Image data for serialization
    private val compressedImages: MutableList<String?> = mutableListOf()
    private val images: MutableList<ImageView> = mutableListOf()
    private val names: MutableList<String> = mutableListOf()
    private val rotations: MutableList<Boolean> = mutableListOf()

    // Reference values for resizing events
    private var initialSpacing = 0f
    private var initialHeight = 0
    private var initialWidth = 0
    private var clickStartTime = 0L

    // Deletion mode flag
    private var deleteMode = false

    /**
     * Add a new image to the fragment from the specified uri
     *
     * @param uri: image uri
     * @param isRotated: true if device is rotated, false otherwise
     */
    fun addImageToView(uri: Uri, isRotated: Boolean) {
        uri.lastPathSegment?.let { seg ->
            // Create and display an ImageView object that shows the image file from the uri
            val imageView = ImageView(fragment.requireContext())
            imageView.id = View.generateViewId()
            imageView.setImageURI(uri)
            fragment.view?.let { fragment.imageContainer.addView(imageView) }

            // Set up a listener for image drag events
            createShadowDragListener(imageView)

            // Add image data to ImageHandler's lists
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    trackImageData(seg, imageView, encodeImage(imageView.drawToBitmap()), isRotated)
                },
                RENDER_TIMER
            )
        }
    }

    /**
     * Add a new image to the fragment from the specified serialized image
     *
     * @param serialized: serialized image to add
     * @param isRotated: true if device is rotated, false otherwise
     */
    private fun addImageToView(serialized: SerializedImage, isRotated: Boolean) {
        // Create a new ImageView that shows the serialized image
        val imageView = ImageView(fragment.requireContext())
        imageView.id = View.generateViewId()
        imageView.setImageBitmap(decodeImage(serialized.image))

        // Extract serialized image properties
        val coords = serialized.coords.toFloatArray()
        var w = serialized.dimens[0].toFloat()
        var h = serialized.dimens[1].toFloat()

        // Check if current rotation matches the rotation of the serialized image
        if (isRotated != serialized.rotated) {
            when (isRotated) {
                true -> {
                    // If currently rotated, rotate from portrait to landscape
                    PORT_TO_LAND.mapPoints(coords, serialized.coords.toFloatArray())
                    w *= SCALE_RATIO
                    h *= SCALE_RATIO
                }
                false -> {
                    // If not currently rotated, rotate from landscape to portrait
                    LAND_TO_PORT.mapPoints(coords, serialized.coords.toFloatArray())
                    w /= SCALE_RATIO
                    h /= SCALE_RATIO
                }
            }
        }

        // Set image view properties based on processed serialized image properties
        // (small loss of precision in rotation scaling when rounding the width/height from float to int)
        imageView.x = coords[0]
        imageView.y = coords[1]
        imageView.layoutParams = RelativeLayout.LayoutParams(w.roundToInt(), h.roundToInt())

        // Display ImageView with serialized image and add image data to ImageHandler's lists
        fragment.view?.let {
            trackImageData(serialized.name, imageView, serialized.image, isRotated)
            fragment.imageContainer.addView(imageView)
        }

        // Set up a listener for image drag events
        createShadowDragListener(imageView)
    }

    /**
     * When a new image is added, add its data to the ImageHandler's lists
     *
     * @param seg: file name segment
     * @param imageView: newly added image
     * @param compressed: compressed image string
     * @param isRotated: true if device is rotated, false otherwise
     */
    private fun trackImageData(seg: String, imageView: ImageView, compressed: String?, isRotated: Boolean) {
        if (names.contains(seg)) {
            // Add empty string to keep array indices aligned
            compressedImages.add("")
        } else {
            compressedImages.add(compressed)
        }
        images.add(imageView)
        names.add(seg)
        rotations.add(isRotated)
    }

    /**
     * Set up listener for image resizing and dragging events
     *
     * @param imageView: ImageView object to listen to
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun createShadowDragListener(imageView: ImageView) {
        imageView.setOnTouchListener { v, e ->
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // Initialize image resize
                    initResize(e, imageView, MainActivity.isRotated(fragment.requireContext(), fragment.isRotated()))
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (e.pointerCount > 1) {
                        // Resize image on pinch gesture
                        handleResize(e, imageView)
                    } else {
                        // Start dragging image on long click gesture
                        handleLongClick(v)
                    }
                    true
                }
                MotionEvent.ACTION_DOWN -> {
                    if (deleteMode) {
                        deleteImage(v)
                    } else {
                        // Start long click timer
                        clickStartTime = Calendar.getInstance().timeInMillis
                    }
                    true
                }
                else -> true
            }
        }
    }

    /**
     * Move image to position on screen when a drop event occurs
     *
     * @param event: drag/drop event
     * @return true if valid event type, false otherwise
     */
    fun handleReposition(event: DragEvent): Boolean {
        return when (event.action) {
            DragEvent.ACTION_DROP -> {
                val view: View? = event.localState as View?
                view?.let { v ->
                    v.x = event.x - (v.width / 2)
                    v.y = event.y - (v.height / 2)
                    v.visibility = View.VISIBLE
                    v.bringToFront()
                }
                true
            }
            DragEvent.ACTION_DRAG_STARTED, DragEvent.ACTION_DRAG_ENDED -> true
            else -> false
        }
    }

    /**
     * Initialize reference values need for image resizing
     *
     * @param e: motion event that started the resizing
     * @param imageView: image that's being resized
     * @param isRotated: true if device is rotated, false otherwise
     */
    private fun initResize(e: MotionEvent, imageView: ImageView, isRotated: Boolean) {
        initialSpacing = spacing(e)
        initialHeight = imageView.height
        initialWidth = imageView.width
        imageView.layoutParams = RelativeLayout.LayoutParams(initialWidth, initialHeight)

        // Update rotation value for the image that's about to be resized
        if (images.contains(imageView)) {
            val index = images.indexOf(imageView)
            rotations[index] = isRotated
        }
    }

    /**
     * Process an image resize event
     *
     * @param e: resize event
     * @param imageView: image to resize
     */
    private fun handleResize(e: MotionEvent, imageView: ImageView) {
        clickStartTime = 0

        // formula reasoning
        //     -> "spread out" will result in positive percentage (add pixels to dimen)
        //     -> "pinch in" will result in negative percentage (sub pixels from dimen)
        var percentage: Float = (spacing(e) / initialSpacing) - 1
        if (percentage < THRESHOLD && percentage > -THRESHOLD) percentage = 0f

        initialHeight = max((initialHeight + (RESIZE_SPEED * percentage)).toInt(), MIN_DIMEN)
        initialWidth = max((initialWidth + (RESIZE_SPEED * percentage)).toInt(), MIN_DIMEN)

        imageView.layoutParams = RelativeLayout.LayoutParams(initialWidth, initialHeight)
    }

    /**
     * Check for long click to start dragging image within note
     *
     * @param v: image to drag
     */
    private fun handleLongClick(v: View) {
        val clickDuration = Calendar.getInstance().timeInMillis - clickStartTime
        if (clickStartTime > 0 && clickDuration >= ViewConfiguration.getLongPressTimeout()) {
            // On long click, make a shadow of the image to drag around
            val data = ClipData.newPlainText("", "")
            val shadowBuilder = View.DragShadowBuilder(v)
            v.startDragAndDrop(data, shadowBuilder, v, 0)
            v.visibility = View.INVISIBLE
        }
    }

    /**
     * Delete specified image from ImageHandler and from note view
     *
     * @param v: image to delete
     */
    private fun deleteImage(v: View) {
        val imageView = v as? View ?: return

        // Remove ImageView from view
        fragment.view?.let {
            fragment.imageContainer.removeView(v)
        }

        // Remove image data from lists
        val index = images.indexOf(imageView)
        images.removeAt(index)
        rotations.removeAt(index)
        names.removeAt(index)
        compressedImages.removeAt(index)
    }

    /**
     * Calculate the distance between two points on the screen when pinching/zooming
     *
     * @param e: pinch/zoom motion event that contains point data
     * @return distance between the two points
     */
    private fun spacing(e: MotionEvent): Float {
        val pointer0 = MotionEvent.PointerCoords()
        e.getPointerCoords(0, pointer0)

        val pointer1 = MotionEvent.PointerCoords()
        e.getPointerCoords(1, pointer1)

        val xDist = abs(pointer0.x - pointer1.x)
        val yDist = abs(pointer0.y - pointer1.y)

        return sqrt((xDist * xDist) + (yDist * yDist))
    }

    /**
     * Get list of images in serialized format
     *
     * @return list of serialized images
     */
    fun getImageList(): List<SerializedImage> {
        val list: MutableList<SerializedImage> = mutableListOf()

        for (index in images.indices) {
            if (images[index].visibility == View.VISIBLE) {
                compressedImages[names.indexOfFirst { it == names[index] }]?.let { image ->
                    list.add(
                        SerializedImage(
                            names[index],
                            image,
                            getImageCoords(index),
                            getImageDimen(index),
                            rotations[index]
                        )
                    )
                }
            }
        }
        return list.toList()
    }

    /**
     * Get list of images in view format
     *
     * @return list of ImageView objects
     */
    fun getImageViewList(): List<ImageView> {
        return images
    }

    /**
     * Get x and y coordinates of all images in ImageHandler
     *
     * @return list of image coordinates
     */
    private fun getImageCoords(index: Int): List<Float> {
        val list: MutableList<Float> = mutableListOf()

        list.add(images[index].x)
        list.add(images[index].y)

        return list.toList()
    }

    /**
     * Get width and heights dimensions of all images in ImageHandler
     *
     * @return list of image dimensions
     */
    private fun getImageDimen(index: Int): List<Int> {
        val list: MutableList<Int> = mutableListOf()

        list.add(images[index].layoutParams.width)
        list.add(images[index].layoutParams.height)

        return list.toList()
    }

    /**
     * Initialize ImageHandler with serialized image data
     *
     * @param list: list to initialize ImageHandler with
     * @param isRotated: true if device is currently rotated, false otherwise
     */
    fun setImageList(list: List<SerializedImage>, isRotated: Boolean) {
        // Clear the existing image data from the ImageHandler's lists
        names.clear()
        compressedImages.clear()
        rotations.clear()
        images.clear()

        for (serialized in list) {
            addImageToView(serialized, isRotated)
        }
    }

    /**
     * Change ability to delete images on touch
     *
     * @param value: if true, enable deletion, if false, disable deletion
     */
    fun setDeleteMode(value: Boolean) {
        deleteMode = value
    }

    /**
     * Compress and convert a bitmap into serializable format
     *
     * @param bitmap: bitmap to compress
     * @return encoded string that represents the compressed image
     */
    private fun encodeImage(bitmap: Bitmap?): String? {
        val stream = ByteArrayOutputStream()
        // compression using 100% image quality
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bytes: ByteArray = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    /**
     * Deserialize an image string into a bitmap
     *
     * @param str: image string
     * @return bitmap representation of image
     */
    private fun decodeImage(str: String): Bitmap? {
        val bytes = Base64.decode(str, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }
}