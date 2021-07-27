/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.sourceeditor

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.microsoft.device.display.wm_samples.sourceeditor.includes.Defines
import com.microsoft.device.display.wm_samples.sourceeditor.includes.DragHandler
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.DualScreenViewModel
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.ScrollViewModel
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.WebViewModel
import java.io.BufferedReader
import java.io.InputStreamReader

/* Fragment that defines functionality for source code editor */
class CodeFragment : Fragment() {
    private lateinit var buttonToolbar: LinearLayout
    private lateinit var codeLayout: LinearLayout
    private lateinit var scrollView: ScrollView

    private lateinit var previewBtn: Button
    private lateinit var textField: TextInputEditText

    private lateinit var dualScreenVM: DualScreenViewModel
    private lateinit var scrollVM: ScrollViewModel
    private lateinit var webVM: WebViewModel

    private var scrollingBuffer: Int = Defines.DEFAULT_BUFFER_SIZE
    private var scrollRange: Int = Defines.DEFAULT_RANGE
    private var rangeFound: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_item_code, container, false)

        activity?.let {
            // initialize ViewModels (find existing or create a new one)
            dualScreenVM = ViewModelProvider(requireActivity()).get(DualScreenViewModel::class.java)
            scrollVM = ViewModelProvider(requireActivity()).get(ScrollViewModel::class.java)
            webVM = ViewModelProvider(requireActivity()).get(WebViewModel::class.java)

            textField = view.findViewById(R.id.textinput_code)
            scrollView = view.findViewById(R.id.scrollview_code)

            val isDualScreen = dualScreenVM.getIsDualScreen().value ?: false
            handleSpannedModeSelection(view, isDualScreen)
            dualScreenVM.getIsDualScreen().observe(
                requireActivity(),
                { bool -> handleSpannedModeSelection(view, bool) }
            )

            if (webVM.getText().value == null) {
                webVM.setText(readFile(Defines.DEFAULT_SOURCE_PATH, context))
            }

            webVM.getText().observe(
                requireActivity(),
                { str ->
                    if (str != textField.text.toString()) {
                        textField.setText(str)
                    }
                }
            )

            textField.setText(webVM.getText().value)

            setOnChangeListenerForTextInput(textField)
        }

        return view
    }

    // read from a base file in the assets folder
    private fun readFile(file: String, context: Context?): String {
        return BufferedReader(InputStreamReader(context?.assets?.open(file))).useLines { lines ->
            val results = StringBuilder()
            lines.forEach { results.append(it + System.getProperty("line.separator")) }
            results.toString()
        }
    }

    // mirror scrolling logic
    private fun handleScrolling(observing: Boolean, int: Int) {
        if (!rangeFound) {
            calibrateScrollView()
        } else {
            if (observing) {
                autoScroll(int)
            } else {
                updateScrollValues(int, Defines.CODE_KEY)
            }
        }
    }

    // single screen vs. dual screen logic
    private fun handleSpannedModeSelection(view: View, isDualMode: Boolean) {
        activity?.let {
            codeLayout = view.findViewById(R.id.code_layout)
            previewBtn = view.findViewById(R.id.btn_switch_to_preview)
            buttonToolbar = view.findViewById(R.id.button_toolbar)

            if (isDualMode) {
                initializeDualScreen()
            } else {
                initializeSingleScreen()
            }
        }
    }

    // spanned selection helper
    private fun initializeSingleScreen() {
        buttonToolbar.visibility = View.VISIBLE
        initializeDragListener()

        previewBtn.setOnClickListener {
            startPreviewFragment()
        }
    }

    // spanned selection helper
    private fun initializeDualScreen() {
        buttonToolbar.visibility = View.GONE

        scrollingBuffer = Defines.DEFAULT_BUFFER_SIZE
        scrollRange = Defines.DEFAULT_RANGE
        rangeFound = false

        // set event and data listeners
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            handleScrolling(false, scrollY)
        }

        scrollVM.getScroll().observe(
            requireActivity(),
            { state ->
                if (state.scrollKey != Defines.CODE_KEY) {
                    handleScrolling(true, state.scrollPercentage)
                }
            }
        )
    }

    // method that triggers transition to preview fragment
    private fun startPreviewFragment() {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.primary_fragment_container,
                PreviewFragment(),
                "PreviewFragment"
            ).addToBackStack("PreviewFragment")
            .commit()
    }

    // listener for changes to text in code editor
    private fun setOnChangeListenerForTextInput(field: TextInputEditText) {
        field.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                webVM.setText(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // get bounds of scroll window
    private fun calibrateScrollView() {
        if (scrollView.scrollY > Defines.MIN_RANGE_THRESHOLD) {
            scrollRange = scrollView.scrollY // successfully calibrated
            rangeFound = true
        } else {
            scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    // mirror scrolling events triggered on preview window
    private fun autoScroll(int: Int) {
        scrollingBuffer = Defines.EMPTY_BUFFER_SIZE

        val y = (scrollRange * int) / 100
        scrollView.scrollTo(scrollView.scrollX, y)
    }

    // handle scroll events triggered on editor window
    private fun updateScrollValues(int: Int, str: String) {
        if (scrollingBuffer >= Defines.DEFAULT_BUFFER_SIZE) {
            val percentage = (int * 100) / scrollRange
            scrollVM.setScroll(str, percentage)
        } else {
            // filter out scrolling events caused by auto scrolling
            scrollingBuffer++
        }
    }

    // create drop targets for the editor screen
    private fun initializeDragListener() {
        val handler = DragHandler(requireActivity(), webVM, requireActivity().contentResolver)

        // Main target will trigger when textField has content
        textField.setOnDragListener { _, event ->
            handler.onDrag(event)
        }

        // Sub target will trigger when textField is empty
        codeLayout.setOnDragListener { _, event ->
            handler.onDrag(event)
        }
    }
}
