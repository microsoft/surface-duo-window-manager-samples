/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.device.display.samples.photoeditor

import android.app.Activity
import android.content.ClipData
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.DragEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import androidx.constraintlayout.utils.widget.ImageFilterView
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.drawToBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.io.IOException
import java.time.LocalDateTime

private lateinit var viewModel: PhotoEditorViewModel

// Request code for image select activity
private const val SELECT_IMAGE = 1000

// Default progress value for SeekBar controls
private const val DEFAULT_PROGRESS = 50

class ActivityFragment : Fragment() {
    private lateinit var image: ImageFilterView
    private lateinit var saturation: SeekBar
    private lateinit var brightness: SeekBar
    private lateinit var warmth: SeekBar
    private var controls: Spinner? = null
    private lateinit var rotate_left: AppCompatImageButton
    private lateinit var rotate_right: AppCompatImageButton
    private lateinit var save: AppCompatImageButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(requireActivity()).get(PhotoEditorViewModel::class.java)
        val view = (
            if (!viewModel.isDualScreen) inflater.inflate(R.layout.fragment_single_screen, container, false)
            else inflater.inflate(R.layout.fragment_dual_screen, container, false)
            )

        initializeViews(view)
        setupLayout()

        return view
    }

    /**
     * Finds all the views in the current layout
     * @param v: view group that contains all the necessary controls
     */
    private fun initializeViews(v: View) {
        image = v.findViewById(R.id.image)
        saturation = v.findViewById(R.id.saturation)
        brightness = v.findViewById(R.id.brightness)
        warmth = v.findViewById(R.id.warmth)
        controls = v.findViewById(R.id.controls)
        rotate_left = v.findViewById(R.id.rotate_left)
        rotate_right = v.findViewById(R.id.rotate_right)
        save = v.findViewById(R.id.save)
    }

    /**
     * Saves relevant control information (ex: SeekBar progress) to pass between states when orientation or spanning changes
     * @param outState: Bundle that contains the information to pass between states
     */
    override fun onSaveInstanceState(outState: Bundle) {
        // SeekBar progress data
        outState.putFloat("saturation", viewModel.saturation)
        outState.putFloat("brightness", viewModel.brightness)
        outState.putFloat("warmth", viewModel.warmth)

        // Selected control in dropdown (only present in single-screen views)
        outState.putInt("selectedControl", viewModel.selectedControl)

        // Actual edited image - saved in ViewModel
        viewModel.updateImage(image.drawable)

        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Select image to edit from photo gallery
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri: Uri = data.data!!
            image.setImageBitmap(BitmapFactory.decodeStream(requireActivity().contentResolver.openInputStream(uri)))

            resetControls(image)
        }
    }

    /**
     * Resets controls and image properties to default/original state
     * @param image: ImageFilterView object that contains current photo
     */
    private fun resetControls(image: ImageFilterView) {
        // Reset SeekBar values
        saturation.progress = DEFAULT_PROGRESS
        brightness.progress = DEFAULT_PROGRESS
        warmth.progress = DEFAULT_PROGRESS

        resetSeekBarVisibility()

        // Reset image filters
        viewModel.resetValues()
        image.saturation = viewModel.saturation
        image.brightness = viewModel.brightness
        image.warmth = viewModel.warmth
    }

    /**
     * Reset dropdown and SeekBar visibility if single-screen view
     */
    private fun resetSeekBarVisibility() {
        if (!viewModel.isDualScreen) {
            saturation.visibility = View.VISIBLE
            brightness.visibility = View.INVISIBLE
            warmth.visibility = View.INVISIBLE
            controls?.setSelection(0)
        }
    }

    /**
     * Initialize aspects of the app that users can interact with
     */
    private fun setupLayout() {
        image.let {
            // Set up click handling for importing images from photo gallery
            image.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, SELECT_IMAGE)
            }

            // Set up drag/drop handling for importing images
            image.setOnDragListener { v, event ->
                // Check file type of dragged data
                val isImage = event.clipDescription?.getMimeType(0).toString().startsWith("image/")

                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        if (isImage) showDropTarget(image)
                    }
                    DragEvent.ACTION_DROP -> {
                        if (isImage) processDrop(v, event)
                    }
                    DragEvent.ACTION_DRAG_ENDED -> {
                        hideDropTarget(image)
                    }
                    else -> {
                        // Ignore other events
                    }
                }
                true
            }

            // Set up all controls
            setUpSaturation(image, viewModel.saturation)
            setUpBrightness(image, viewModel.brightness)
            setUpWarmth(image, viewModel.warmth)
            setUpRotate(image)
            setUpSave(image)

            prepareToggle()
            restoreImage()
        }
    }

    /**
     * Update the image based on info from the viewModel.
     * Used to restore image settings after screen and orientation changes.
     */
    private fun restoreImage() {
        viewModel.getImage().value?.let {
            image.setImageDrawable(viewModel.getImage().value)
        }

        image.brightness = viewModel.brightness
        image.saturation = viewModel.saturation
        image.warmth = viewModel.warmth
    }

    private fun prepareToggle() {
        // Set up single screen control dropdown
        if (!viewModel.isDualScreen) {
            setUpToggle(viewModel.selectedControl)
        }
    }

    /**
     * Revert any appearance changes related to drag/drop
     * @param image: ImageFilterView object that needs to be reset
     */
    private fun hideDropTarget(image: ImageFilterView) {
        image.alpha = 1f
        image.setPadding(0, 0, 0, 0)
        image.cropToPadding = false
        image.setBackgroundColor(Color.TRANSPARENT)
    }

    /**
     * Set image source of ImageFilterView to dropped data
     * @param v: View that the data was dropped in (ImageFilterView)
     * @param event: DragEvent that contains the dropped data
     */
    private fun processDrop(v: View, event: DragEvent) {
        val item: ClipData.Item = event.clipData.getItemAt(0)
        val uri = item.uri
        val image = v as ImageFilterView

        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            ActivityCompat.requestDragAndDropPermissions(requireActivity(), event)
            image.setImageURI(uri)
        } else {
            image.setImageURI(uri)
        }

        resetControls(image)
    }

    /**
     * Changes appearance of ImageFilterView object to show that it's a drop target
     * @param image: ImageFilterView object/drop target
     */
    private fun showDropTarget(image: ImageFilterView) {
        image.alpha = 0.5f
        image.setPadding(20, 20, 20, 20)
        image.cropToPadding = true
        image.setBackgroundColor(Color.parseColor("grey"))
    }

    /**
     * Initialize the single screen spinner tied to saturation/brightness/warmth selection
     * @param selectedControl: state to initialize the spinner to
     */
    private fun setUpToggle(selectedControl: Int?) {
        // Set up contents of controls dropdown
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.controls_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            controls?.adapter = adapter
        }

        // Restore value from previous state if available, otherwise default to first item in list (saturation)
        controls?.setSelection(selectedControl ?: 0)

        // Set up response to changing the selected control
        controls?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                when (parent.getItemAtPosition(pos)) {
                    getString(R.string.saturation) -> {
                        saturation.visibility = View.VISIBLE
                        brightness.visibility = View.INVISIBLE
                        warmth.visibility = View.INVISIBLE
                    }
                    getString(R.string.brightness) -> {
                        saturation.visibility = View.INVISIBLE
                        brightness.visibility = View.VISIBLE
                        warmth.visibility = View.INVISIBLE
                    }
                    getString(R.string.warmth) -> {
                        saturation.visibility = View.INVISIBLE
                        brightness.visibility = View.INVISIBLE
                        warmth.visibility = View.VISIBLE
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    /**
     * Initialize the image warmth control
     * @param image: the image object affected by changes in the seekbar
     * @param progress: value to initialize the seekbar to
     */
    private fun setUpWarmth(image: ImageFilterView, progress: Float?) {
        // Restore value from previous state if available
        progress?.let {
            image.warmth = it
            warmth.progress = (it * DEFAULT_PROGRESS).toInt()
            viewModel.warmth = image.warmth
        }

        warmth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // warmth from 0.5 (cold) to 1 (original) to 2 (warm), progress from 0 to 100
                image.warmth = progress.toFloat() / DEFAULT_PROGRESS
                viewModel.warmth = image.warmth
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}

            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    /**
     * Initialize the image brightness control
     * @param image: the image object affected by changes in the seekbar
     * @param progress: value to initialize the seekbar to
     */
    private fun setUpBrightness(image: ImageFilterView, progress: Float?) {
        // Restore value from previous state if available
        progress?.let {
            image.brightness = it
            brightness.progress = (it * DEFAULT_PROGRESS).toInt()
            viewModel.brightness = image.brightness
        }

        brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // brightness from 0 (black) to 1 (original) to 2 (twice as bright), progress from 0 to 100
                image.brightness = progress.toFloat() / DEFAULT_PROGRESS
                viewModel.brightness = image.brightness
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}

            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    /**
     * Initialize the image saturation control
     * @param image: the image object affected by changes in the seekbar
     * @param progress: value to initialize the seekbar to
     */
    private fun setUpSaturation(image: ImageFilterView, progress: Float?) {
        // Restore value from previous state if available
        progress?.let {
            image.saturation = it
            saturation.progress = (it * DEFAULT_PROGRESS).toInt()
            viewModel.saturation = image.saturation
        }

        saturation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // saturation from 0 (grayscale) to 1 (original) to 2 (hyper-saturated), progress from 0 to 100
                image.saturation = progress.toFloat() / DEFAULT_PROGRESS
                viewModel.saturation = image.saturation
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}

            override fun onStopTrackingTouch(seek: SeekBar) {}
        })
    }

    /**
     * Initialize the image rotation control
     * @param image: the image object affected by button presses
     */
    private fun setUpRotate(image: ImageFilterView) {
        rotate_left.setOnClickListener {
            applyRotationMatrix(270f, image)
        }

        rotate_right.setOnClickListener {
            applyRotationMatrix(90f, image)
        }
    }

    /**
     * Rotates image by specified angle
     * @param angle: angle to rotate image
     * @param image: ImageFilterView that contains current image
     */
    private fun applyRotationMatrix(angle: Float, image: ImageFilterView) {
        // Create rotation angle with given matrix
        val matrix = Matrix()
        matrix.postRotate(angle)

        // Apply rotation matrix to the image
        val curr = image.drawable.toBitmap()
        val bm = Bitmap.createBitmap(curr, 0, 0, curr.width, curr.height, matrix, true)

        // Update ImageFilterView object with rotated bitmap
        image.setImageBitmap(bm)
    }

    private fun setUpSave(image: ImageFilterView) {
        save.setOnClickListener {
            // Get current size of drawable so entire ImageView is not drawn to bitmap
            val rect = RectF()
            image.imageMatrix.mapRect(rect, RectF(image.drawable.bounds))
            val bm = Bitmap.createBitmap(
                image.drawToBitmap(),
                rect.left.toInt(),
                rect.top.toInt(),
                rect.width().toInt(),
                rect.height().toInt()
            )

            val values = getImageInfo()
            saveToGallery(bm, values)
        }
    }

    /**
     * Creates and returns a ContentValues object with relevant image information
     * @return ContentValues object with title, description, mime type, etc.
     */
    private fun getImageInfo(): ContentValues {
        return ContentValues().apply {
            put(MediaStore.Images.Media.TITLE, getString(R.string.photo_name))
            put(MediaStore.Images.Media.DISPLAY_NAME, getString(R.string.photo_name))
            put(
                MediaStore.Images.Media.DESCRIPTION,
                "${getString(R.string.photo_description)} ${LocalDateTime.now()}"
            )
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${getString(R.string.pictures_folder)}/${getString(R.string.app_name)}"
            )
            put(MediaStore.Images.Media.IS_PENDING, true)
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000) // seconds
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis()) // milliseconds
        }
    }

    /**
     * Try to save bitmap to photo gallery or show error Toast if unsuccessful
     * @param bm: Bitmap to save to photos
     * @param values: ContentValues object that stores image information
     */
    private fun saveToGallery(bm: Bitmap, values: ContentValues) {
        try {
            val uri = requireActivity().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            ) ?: throw IOException("MainActivity: ${getString(R.string.null_uri)}")

            val stream = requireActivity().contentResolver.openOutputStream(uri)
                ?: throw IOException("MainActivity: ${getString(R.string.null_stream)}")
            if (!bm.compress(Bitmap.CompressFormat.JPEG, 100, stream))
                throw IOException("MainActivity: ${getString(R.string.bitmap_error)}")
            stream.close()

            values.put(MediaStore.Images.Media.IS_PENDING, false)
            requireActivity().contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "${getString(R.string.image_save_error)}\n${e.printStackTrace()}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}