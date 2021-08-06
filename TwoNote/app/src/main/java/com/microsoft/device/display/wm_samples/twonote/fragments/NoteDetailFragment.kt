/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.twonote.fragments

import Defines.DEFAULT_THICKNESS
import Defines.INODE
import Defines.LIST_FRAGMENT
import Defines.NOTE
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.microsoft.device.display.wm_samples.twonote.MainActivity
import com.microsoft.device.display.wm_samples.twonote.R
import com.microsoft.device.display.wm_samples.twonote.models.DualScreenViewModel
import com.microsoft.device.display.wm_samples.twonote.models.INode
import com.microsoft.device.display.wm_samples.twonote.models.Note
import com.microsoft.device.display.wm_samples.twonote.utils.DataProvider
import com.microsoft.device.display.wm_samples.twonote.utils.DragHandler
import com.microsoft.device.display.wm_samples.twonote.utils.FileSystem
import com.microsoft.device.ink.InkView
import com.microsoft.device.ink.InputManager
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime

/**
 * Fragment that shows a detailed view of a note and lets the user edit, share, rename, and delete
 * note contents
 */
class NoteDetailFragment : Fragment() {
    enum class PaintColors { Red, Blue, Green, Yellow, Purple }
    enum class EditingMode { Text, Image, Ink }

    // Note data attributes
    var deleted = false
    private var note: Note? = null
    private var inode: INode? = null

    // Note view attributes
    private lateinit var drawView: InkView
    private lateinit var dragHandler: DragHandler
    lateinit var noteText: TextInputEditText
    private lateinit var noteTitle: TextInputEditText
    private lateinit var rootDetailLayout: ConstraintLayout
    lateinit var imageContainer: RelativeLayout

    // Editing mode attributes
    private var inkItem: MenuItem? = null
    private var textItem: MenuItem? = null
    private var imageItem: MenuItem? = null
    private var deleteImageMode = false

    private var dualScreenVM = DualScreenViewModel()

    /**
     * Listener that communicates note changes between detail and list fragments
     */
    interface OnFragmentInteractionListener {
        fun onINodeUpdate()
    }

    companion object {
        internal fun newInstance(inode: INode, note: Note) = NoteDetailFragment().apply {
            arguments = Bundle().apply {
                this.putSerializable(NOTE, note)
                this.putSerializable(INODE, inode)
            }
        }

        lateinit var mListener: OnFragmentInteractionListener

        // Pen stroke thickness values
        const val THICKNESS_1 = 5
        const val THICKNESS_2 = 15
        const val THICKNESS_DEFAULT = 25
        const val THICKNESS_4 = 50
        const val THICKNESS_5 = 75
        const val THICKNESS_6 = 100
        const val STROKE_MAX_MIN_RATIO = 10f
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Connect listener (MainActivity) to fragment so note edits in the UI are passed back
        // to the list of note objects
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw ClassCastException("$context ${resources.getString(R.string.exception_message)}")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_note_detail, container, false)
        dualScreenVM = ViewModelProvider(requireActivity()).get(DualScreenViewModel::class.java)

        noteTitle = view.findViewById(R.id.title_input)
        noteText = view.findViewById(R.id.text_input)
        rootDetailLayout = view.findViewById(R.id.note_detail_layout)
        imageContainer = view.findViewById(R.id.image_container)

        setUpToolbar(view)
        setUpTextMode(view)
        setUpImageMode(view)
        setUpInkMode(view)
        initializeDragListener()

        return view
    }

    /**
     * Initialize toolbar buttons and content
     *
     * @param view: the fragment's view
     */
    private fun setUpToolbar(view: View) {
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_note_detail)

        // Set up overflow menu
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        toolbar.overflowIcon?.setTint(requireContext().getColor(R.color.colorOnPrimary))

        // Set up editing modes
        for (item in toolbar.menu) {
            when (item.title) {
                getString(R.string.action_ink_on), getString(R.string.action_ink_off) -> inkItem = item
                getString(R.string.action_text_on), getString(R.string.action_text_off) -> textItem = item
                getString(R.string.action_image_on), getString(R.string.action_image_off) -> imageItem = item
            }
        }

        // Set up navigation
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener { closeFragment() }
    }

    /**
     * Initialize text mode and load note contents
     *
     * @param view: the fragment's view
     */
    private fun setUpTextMode(view: View) {
        // Retrieve note/inode from bundle arguments
        arguments?.let {
            val n = it.getSerializable(NOTE)
            val i = it.getSerializable(INODE)

            note = if (n is Note) n else null
            inode = if (i is INode) i else null
        }

        // Update view elements with note contents
        noteTitle.setText(note?.title)
        noteText.setText(note?.text)

        // Set default mode to text mode
        view.findViewById<ScrollView>(R.id.text_mode)?.bringToFront()
    }

    /**
     * Initialize button for image mode
     *
     * @param view: the fragment's view
     */
    private fun setUpImageMode(view: View) {
        // Set up delete button
        val deleteImageButton = view.findViewById<ImageButton>(R.id.delete_image)
        deleteImageButton.setOnClickListener {
            toggleDeleteImageMode()
            toggleButtonColor(deleteImageButton, deleteImageMode)
        }
    }

    /**
     * Initialize buttons for ink mode
     *
     * @param view: the fragment's view
     */
    private fun setUpInkMode(view: View) {
        drawView = view.findViewById(R.id.draw_view)

        // Set up pen tools buttons
        view.findViewById<ImageButton>(R.id.undo).setOnClickListener { drawView.undo() }
        setUpColorButtons(view)
        setUpThicknessBar(view)
        setUpErasingAndHighlighting(view)
        setUpClearButton(view)

        setUpDrawView()
    }

    /**
     * Initialize settings for ink color selection
     *
     * @param view: the fragment's view
     */
    private fun setUpColorButtons(view: View) {
        val colorButton = view.findViewById<ImageButton>(R.id.color)
        val colorButtonsLayout = view.findViewById<LinearLayout>(R.id.color_buttons)
        colorButton.setOnClickListener {
            toggleViewVisibility(colorButtonsLayout)
            toggleButtonColor(colorButton, colorButtonsLayout?.visibility == View.VISIBLE)
        }

        // Set up color choice buttons
        view.findViewById<Button>(R.id.button_red).setOnClickListener { chooseColor(PaintColors.Red.name) }
        view.findViewById<Button>(R.id.button_blue).setOnClickListener { chooseColor(PaintColors.Blue.name) }
        view.findViewById<Button>(R.id.button_green).setOnClickListener { chooseColor(PaintColors.Green.name) }
        view.findViewById<Button>(R.id.button_yellow).setOnClickListener { chooseColor(PaintColors.Yellow.name) }
        view.findViewById<Button>(R.id.button_purple).setOnClickListener { chooseColor(PaintColors.Purple.name) }

        // Set up custom color button and dialog
        val chooseButton = view.findViewById<ImageButton>(R.id.button_choose)
        chooseButton.setOnClickListener {
            val textInput = TextInputEditText(requireContext())

            AlertDialog.Builder(requireContext())
                .setMessage(resources.getString(R.string.choose_color_message))
                .setView(textInput)
                .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                    val result = stringToColor(textInput.text.toString())
                    if (result != -1) {
                        chooseColor("", result)
                        toggleButtonColor(chooseButton, true, result)
                    } else {
                        toggleButtonColor(chooseButton, false)
                    }
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .setTitle(resources.getString(R.string.choose_color))
                .create()
                .show()
        }
    }

    /**
     * Update inking color for canvas
     *
     * @param color: name of color to change to
     * @param colorInt: if non-null, int value of color to change to (defaults to null)
     */
    private fun chooseColor(color: String, colorInt: Int? = null) {
        // Reset the background color of the custom color button
        view?.findViewById<ImageButton>(R.id.button_choose)?.clearColorFilter()

        when (color) {
            PaintColors.Red.name -> drawView.color = ContextCompat.getColor(requireActivity().applicationContext, R.color.red)
            PaintColors.Blue.name -> drawView.color = ContextCompat.getColor(requireActivity().applicationContext, R.color.blue)
            PaintColors.Green.name -> drawView.color = ContextCompat.getColor(requireActivity().applicationContext, R.color.green)
            PaintColors.Yellow.name -> drawView.color = ContextCompat.getColor(requireActivity().applicationContext, R.color.yellow)
            PaintColors.Purple.name -> drawView.color = ContextCompat.getColor(requireActivity().applicationContext, R.color.purple)
            else -> if (colorInt != null) drawView.color = colorInt
        }
    }

    /**
     * Converts user-inputted color to Color object using parseColor method
     *
     * Accepted hexadecimal color formats: #RRGGBB or #AARRGGBB
     *
     * Accepted color names: red, blue, green, black, white, gray, cyan, magenta, yellow,
     * lightgray, darkgray, grey, lightgrey, darkgrey, aqua, fuchsia, lime, maroon,
     * navy, olive, purple, silver, and teal.
     *
     * @param string: string to try to parse into a color
     * @return int value of color or -1 if parse was unsuccessful
     */
    private fun stringToColor(string: String): Int {
        return try {
            Color.parseColor(string.trim())
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Initialize settings for ink thickness
     *
     * @param view: the fragment's view
     */
    private fun setUpThicknessBar(view: View) {
        val thickness = view.findViewById<SeekBar>(R.id.thickness_slider)
        thickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seek: SeekBar, progress: Int, fromUser: Boolean) {
                // Progress: [0, 6] (default 3), Thicknesses: [5, 100] (default 25)
                val newThickness =
                    when (progress) {
                        1 -> THICKNESS_1
                        2 -> THICKNESS_2
                        3 -> THICKNESS_DEFAULT
                        4 -> THICKNESS_4
                        5 -> THICKNESS_5
                        6 -> THICKNESS_6
                        else -> THICKNESS_DEFAULT
                    }
                drawView.strokeWidth = newThickness.toFloat() / STROKE_MAX_MIN_RATIO
                drawView.strokeWidthMax = newThickness.toFloat()
            }

            override fun onStartTrackingTouch(seek: SeekBar) {}
            override fun onStopTrackingTouch(seek: SeekBar) {}
        })

        val thicknessButton = view.findViewById<ImageButton>(R.id.thickness)
        thicknessButton.setOnClickListener {
            toggleViewVisibility(thickness)
            toggleButtonColor(thicknessButton, thickness?.visibility == View.VISIBLE)
        }
    }

    /**
     * Initialize settings for ink highlighting and erasing
     *
     * @param view: the fragment's view
     */
    private fun setUpErasingAndHighlighting(view: View) {
        val eraseButton = view.findViewById<ImageButton>(R.id.erase)
        val highlightButton = view.findViewById<ImageButton>(R.id.highlight)

        highlightButton.setOnClickListener {
            val activate: Boolean
            if (drawView.dynamicPaintHandler !is HighlighterPaintHandler) {
                activate = true
                drawView.dynamicPaintHandler = HighlighterPaintHandler()
            } else {
                activate = false
                drawView.dynamicPaintHandler = FancyPaintHandler()
            }
            toggleButtonColor(highlightButton, activate)

            // Update button description and turn off eraser mode if activating highlighting mode
            if (activate) {
                toggleButtonColor(eraseButton, false)
                it.contentDescription = resources.getString(R.string.action_highlight_off)
            } else {
                it.contentDescription = resources.getString(R.string.action_highlight_on)
            }
        }

        eraseButton.setOnClickListener {
            val activate: Boolean

            if (drawView.dynamicPaintHandler !is EraserPaintHandler) {
                activate = true
                drawView.dynamicPaintHandler = EraserPaintHandler()
            } else {
                activate = false
                drawView.dynamicPaintHandler = FancyPaintHandler()
            }
            toggleButtonColor(eraseButton, activate)

            // Update button description and turn off highlight button if activating eraser mode
            if (activate) {
                toggleButtonColor(highlightButton, false)
                it.contentDescription = resources.getString(R.string.action_erase_off)
            } else {
                it.contentDescription = resources.getString(R.string.action_erase_on)
            }
        }
    }

    /**
     * Initialize settings for clearing drawings
     *
     * @param view: the fragment's view
     */
    private fun setUpClearButton(view: View) {
        view.findViewById<ImageButton>(R.id.clear).setOnClickListener {
            // Create confirmation dialog before user clears all ink
            AlertDialog.Builder(requireContext())
                .setMessage(resources.getString(R.string.confirm_clear_message))
                .setPositiveButton(resources.getString(android.R.string.ok)) { dialog, _ ->
                    drawView.clearInk()
                    dialog.dismiss()
                }
                .setNegativeButton(resources.getString(android.R.string.cancel)) { dialog, _ -> dialog.dismiss() }
                .setTitle(resources.getString(R.string.confirm_clear))
                .create()
                .show()
        }
    }

    /**
     * Initialize the drawing canvas with any existing note drawings and the device's current rotation
     */
    private fun setUpDrawView() {
        note?.let { n ->
            drawView.strokeWidth = DEFAULT_THICKNESS.toFloat()
            val inkingList: MutableList<InkView.DynamicPaintHandler?> = mutableListOf()

            for (stroke in n.drawings) {
                if (stroke.inkingMode == InputManager.InkingType.INKING) {
                    inkingList.add(FancyPaintHandler())
                } else if (stroke.inkingMode == InputManager.InkingType.HIGHLIGHTING) {
                    inkingList.add(HighlighterPaintHandler())
                } else if (stroke.inkingMode == InputManager.InkingType.ERASING) {
                    inkingList.add(EraserPaintHandler())
                } else {
                    inkingList.add(null)
                }
            }
            drawView.loadDrawing(n.drawings, inkingList)
        }
    }

    /**
     * Add drag and drop handling to note view
     */
    private fun initializeDragListener() {
        dragHandler = DragHandler(this)

        // Main target will trigger when textField has content
        noteText.setOnDragListener { _, event ->
            dragHandler.onDrag(event)
        }

        // Sub-target will trigger when textField is empty
        rootDetailLayout.setOnDragListener { _, event ->
            dragHandler.onDrag(event)
        }
    }

    /**
     * Change view visibility from visible to invisible or vice versa
     *
     * @param view: View to change visibility of
     * @param hide: if true, makes the view invisible regardless of current visibility (defaults to false)
     */
    private fun toggleViewVisibility(view: View?, hide: Boolean = false) {
        if (view?.visibility == View.VISIBLE || hide) {
            view?.visibility = View.INVISIBLE
        } else {
            view?.visibility = View.VISIBLE
        }
    }

    /**
     * Change ImageButton background color to indicate whether it's active or not
     *
     * @param button: ImageButton to change background color of
     * @param activated: if true, set background color, if false, clear background color
     * @param color: int value of background color (defaults to theme's primary color)
     */
    private fun toggleButtonColor(
        button: ImageButton?,
        activated: Boolean,
        color: Int = resources.getColor(R.color.colorPrimary, requireActivity().theme)
    ) {
        if (activated) {
            button?.background?.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC)
        } else {
            button?.background?.clearColorFilter()
        }
    }

    /**
     * Change ability to delete images on touch
     *
     * @param force: if non-null, true -> deletion is enabled, false -> deletion is disabled (defaults to null)
     */
    private fun toggleDeleteImageMode(force: Boolean? = null) {
        deleteImageMode = force ?: !deleteImageMode

        // Visually indicate that deletion is active by changing image appearances
        val alpha = if (deleteImageMode) 0.5f else 1f
        val color = if (deleteImageMode) Color.GRAY else Color.TRANSPARENT

        for (image in dragHandler.getImageViewList()) {
            image.alpha = alpha
            image.setBackgroundColor(color)
        }

        // Tell drag handler that deletion is active so it won't move the image on touch
        dragHandler.setDeleteMode(deleteImageMode)
    }

    override fun onResume() {
        super.onResume()
        // Update image list in drag handler
        note?.let { n ->
            val imageList = n.images
            dragHandler.setImageList(imageList, MainActivity.isRotated(requireContext(), dualScreenVM.isDualScreen))
        }
    }

    override fun onPause() {
        super.onPause()
        updateNoteContents()
        save()
    }

    /**
     * Save note changes in the view to note object
     */
    fun updateNoteContents() {
        if (!deleted) {
            val text = noteText.text.toString()
            val title = noteTitle.text.toString()

            if (this::drawView.isInitialized) {
                note?.drawings = drawView.getDrawing()
            }
            if (this::dragHandler.isInitialized) {
                note?.images = dragHandler.getImageList()
                dragHandler.clearImages()
            }

            note?.text = text
            note?.title = title
            inode?.title = title
            inode?.dateModified = LocalDateTime.now()

            // Tell listener that note contents have been updated
            mListener.onINodeUpdate()
        }
    }

    /**
     * Save note data to file system
     */
    fun save() {
        note?.let { n ->
            if (!deleted) {
                FileSystem.save(requireContext(), DataProvider.getActiveSubDirectory(), n)
            }
        }
    }

    /**
     * Set up overflow menu item actions
     *
     * @param item: selected item from overflow menu
     * @return true if handled successfully, false otherwise
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share -> {
                shareNoteContents()
                true
            }
            R.id.action_delete -> {
                inode?.let { i ->
                    FileSystem.delete(requireContext(), DataProvider.getActiveSubDirectory(), i)
                    deleted = true
                }
                closeFragment()
                true
            }
            R.id.action_text -> {
                changeEditingMode(EditingMode.Text)
                true
            }
            R.id.action_image -> {
                changeEditingMode(EditingMode.Image)
                true
            }
            R.id.action_ink -> {
                changeEditingMode(EditingMode.Ink)
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    /**
     * Take a screenshot of the current note and allow user to share it through an Intent
     */
    private fun shareNoteContents() {
        view?.let {
            // Create path for note image file
            val path = requireContext().getExternalFilesDir(null)?.absolutePath + "/$inode.jpg"

            // Get location of NoteDetailFragment view within window
            val coords = IntArray(2)
            it.getLocationInWindow(coords)

            // Screenshot NoteDetailFragment and store it as a bitmap
            val bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
            PixelCopy.request(
                requireActivity().window,
                Rect(coords[0], coords[1], coords[0] + it.width, coords[1] + it.height),
                bitmap,
                { copyResult: Int ->
                    // If stored successfully, open an Intent for sharing the note as an image
                    if (copyResult == PixelCopy.SUCCESS) {
                        openShareIntent(bitmap, path)
                    }
                },
                it.handler
            )
        }
    }

    /**
     * Start a share intent for an image
     *
     * @param bitmap: bitmap/image to be shared
     * @param path: location of bitmap
     */
    private fun openShareIntent(bitmap: Bitmap, path: String) {
        // Write bitmap to file (with parameter 100 for max quality)
        val outputStream = FileOutputStream(path)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.close()

        // Open Intent for sharing the image
        val file = File(path)
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(requireContext(), requireContext().packageName + ".provider", file))
        startActivity(Intent.createChooser(intent, resources.getString(R.string.share_intent)))
    }

    /**
     * Change current note editing mode
     *
     * @param mode: editing mode to change to
     */
    fun changeEditingMode(mode: EditingMode) {
        when (mode) {
            EditingMode.Text -> {
                activateText(true)
                activateImage(false)
                activateInk(false)
            }
            EditingMode.Image -> {
                activateText(false)
                activateImage(true)
                activateInk(false)
            }
            EditingMode.Ink -> {
                activateText(false)
                activateImage(false)
                activateInk(true)
            }
        }
    }

    /**
     * Enable or disable text mode
     *
     * @param enable: if true, enable, if false, disable
     */
    private fun activateText(enable: Boolean) {
        if (enable) {
            textItem?.setIcon(R.drawable.ic_fluent_text_field_24_filled)
            textItem?.title = getString(R.string.action_text_off)
            view?.findViewById<ScrollView>(R.id.text_mode)?.bringToFront()
        } else {
            textItem?.setIcon(R.drawable.ic_fluent_text_field_24_regular)
            textItem?.title = getString(R.string.action_text_on)
        }
    }

    /**
     * Enable or disable image mode
     *
     * @param enable: if true, enable, if false, disable
     */
    private fun activateImage(enable: Boolean) {
        val imageTools = view?.findViewById<LinearLayout>(R.id.image_tools)
        if (enable) {
            imageItem?.setIcon(R.drawable.ic_fluent_image_24_filled)
            imageItem?.title = getString(R.string.action_image_off)
            imageContainer.bringToFront()

            // Show image tools over canvas
            imageTools?.visibility = View.VISIBLE
            imageTools?.bringToFront()
        } else {
            imageItem?.setIcon(R.drawable.ic_fluent_image_24_regular)
            imageItem?.title = getString(R.string.action_image_on)

            // Close image tools and reset button states
            imageTools?.visibility = View.INVISIBLE
            toggleButtonColor(view?.findViewById(R.id.delete_image), false)
            toggleDeleteImageMode(false)
        }
    }

    /**
     * Enable or disable ink mode
     *
     * @param enable: if true, enable, if false, disable
     */
    private fun activateInk(enable: Boolean) {
        val penTools = view?.findViewById<LinearLayout>(R.id.pen_tools)
        if (enable) {
            inkItem?.setIcon(R.drawable.ic_fluent_inking_tool_24_filled)
            inkItem?.title = getString(R.string.action_ink_off)
            view?.findViewById<ConstraintLayout>(R.id.ink_mode)?.bringToFront()

            // Enable drawing and show pen tools over canvas
            drawView.inkingEnabled = true
            drawView.dynamicPaintHandler = FancyPaintHandler()
            penTools?.visibility = View.VISIBLE
            penTools?.bringToFront()
        } else {
            inkItem?.setIcon(R.drawable.ic_fluent_inking_tool_24_regular)
            inkItem?.title = getString(R.string.action_ink_on)

            // Disable drawing, close pen tools, and reset button states
            drawView.inkingEnabled = false
            drawView.dynamicPaintHandler = null
            penTools?.visibility = View.INVISIBLE
            toggleViewVisibility(view?.findViewById<SeekBar>(R.id.thickness_slider), true)
            toggleViewVisibility(view?.findViewById<LinearLayout>(R.id.color_buttons), true)
            toggleButtonColor(view?.findViewById(R.id.thickness), false)
            toggleButtonColor(view?.findViewById(R.id.color), false)
            toggleButtonColor(view?.findViewById(R.id.highlight), false)
            toggleButtonColor(view?.findViewById(R.id.erase), false)
        }
    }

    fun isDualScreen(): Boolean {
        return dualScreenVM.isDualScreen
    }

    /**
     * Close NoteDetailFragment after deletion and open either the NoteListFragment (unspanned)
     * or the GetStartedFragment (spanned)
     */
    fun closeFragment() {
        activity?.let { activity ->
            if (dualScreenVM.isDualScreen &&
                !MainActivity.isRotated(activity, dualScreenVM.isDualScreen)
            ) {
                // Tell NoteListFragment that list data has changed
                (parentFragmentManager.findFragmentByTag(LIST_FRAGMENT) as? NoteListFragment)
                    ?.updateNotesList()

                // If spanned and not rotated (list/detail view), show GetStartedFragment in second container
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.secondary_fragment_container,
                        GetStartedFragment(),
                        null
                    ).commit()
            } else {
                // If unspanned, or spanned and rotated (extended canvas), show NoteListFragment in first container
                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.primary_fragment_container,
                        NoteListFragment(),
                        LIST_FRAGMENT
                    ).commit()
            }
        }
    }

    // ------------------------- Custom paint handler classes --------------------------- \\

    /**
     * Renders the ink with transparency linked to the pressure on the pen.
     */
    inner class FancyPaintHandler : InkView.DynamicPaintHandler {
        override fun generatePaintFromPenInfo(penInfo: InputManager.PenInfo): Paint {
            val paint = Paint()
            val alpha = penInfo.pressure * 255

            paint.color = Color.argb(
                alpha.toInt(),
                drawView.color.red,
                drawView.color.green,
                drawView.color.blue
            )
            paint.isAntiAlias = true
            // Set stroke width based on display density.
            paint.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                penInfo.pressure * (drawView.strokeWidthMax - drawView.strokeWidth) + drawView.strokeWidth,
                resources.displayMetrics
            )
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND

            return paint
        }

        override fun selectInkingMode(): InputManager.InkingType {
            return InputManager.InkingType.INKING
        }
    }

    /**
     * Renders the ink as though from a highlighter: permanently transparent
     */
    inner class HighlighterPaintHandler : InkView.DynamicPaintHandler {
        override fun generatePaintFromPenInfo(penInfo: InputManager.PenInfo): Paint {
            val paint = Paint()
            val alpha = 80

            paint.color = ColorUtils.setAlphaComponent(drawView.color, alpha)
            paint.isAntiAlias = true
            // Set stroke width based on display density.
            paint.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                penInfo.pressure * (drawView.strokeWidthMax - drawView.strokeWidth) + drawView.strokeWidth,
                resources.displayMetrics
            )
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.BEVEL
            paint.strokeCap = Paint.Cap.BUTT

            return paint
        }

        override fun selectInkingMode(): InputManager.InkingType {
            return InputManager.InkingType.HIGHLIGHTING
        }
    }

    /**
     * Renders the ink as though from an eraser
     */
    inner class EraserPaintHandler : InkView.DynamicPaintHandler {
        override fun generatePaintFromPenInfo(penInfo: InputManager.PenInfo): Paint {
            val paint = Paint()
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            paint.isAntiAlias = true
            // Set stroke width based on display density.
            paint.strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                penInfo.pressure * (drawView.strokeWidthMax - drawView.strokeWidth) + drawView.strokeWidth,
                resources.displayMetrics
            )
            paint.style = Paint.Style.STROKE
            paint.strokeJoin = Paint.Join.ROUND
            paint.strokeCap = Paint.Cap.ROUND

            return paint
        }

        override fun selectInkingMode(): InputManager.InkingType {
            return InputManager.InkingType.ERASING
        }
    }
}
