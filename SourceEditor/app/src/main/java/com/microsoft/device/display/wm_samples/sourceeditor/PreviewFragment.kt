/*
 *
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *
 */

package com.microsoft.device.display.wm_samples.sourceeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.microsoft.device.display.wm_samples.sourceeditor.includes.Defines
import com.microsoft.device.display.wm_samples.sourceeditor.includes.DragHandler
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.DualScreenViewModel
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.ScrollViewModel
import com.microsoft.device.display.wm_samples.sourceeditor.viewmodel.WebViewModel

/* Fragment that defines functionality for the source code previewer */
class PreviewFragment : Fragment() {
    private lateinit var scrollView: ScrollView

    private lateinit var editorBtn: Button
    private lateinit var previewBtn: Button
    private lateinit var webView: WebView

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
        val view = inflater.inflate(R.layout.fragment_item_preview, container, false)

        webView = view.findViewById(R.id.webview_preview)
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.webChromeClient = WebChromeClient()

        activity?.let {
            // initialize ViewModels (find existing or create a new one)
            dualScreenVM = ViewModelProvider(requireActivity()).get(DualScreenViewModel::class.java)
            scrollVM = ViewModelProvider(requireActivity()).get(ScrollViewModel::class.java)
            webVM = ViewModelProvider(requireActivity()).get(WebViewModel::class.java)

            val str: String = (webVM.getText().value) ?: ""
            webView.loadData(str, Defines.HTML_TYPE, Defines.ENCODING)

            val isDualScreen = dualScreenVM.getIsDualScreen().value ?: false
            handleSpannedModeSelection(view, webView, isDualScreen)
            dualScreenVM.getIsDualScreen().observe(
                requireActivity(),
                { bool -> handleSpannedModeSelection(view, webView, bool) }
            )
        }

        return view
    }

    // mirror scrolling logic
    private fun handleScrolling(observing: Boolean, int: Int) {
        if (!rangeFound) {
            calibrateScrollView()
        } else {
            // code window scrolled, auto scroll to match editor
            if (observing) {
                autoScroll(int)
            } else {
                updateScrollValues(int, Defines.PREVIEW_KEY)
            }
        }
    }

    // single screen vs. dual screen logic
    private fun handleSpannedModeSelection(view: View, webView: WebView, isDualMode: Boolean) {
        activity?.let { activity ->
            editorBtn = view.findViewById(R.id.btn_switch_to_editor)
            previewBtn = view.findViewById(R.id.btn_switch_to_preview)

            // listen for changes made to the editor
            webVM.getText().observe(
                requireActivity(),
                { str ->
                    webView.loadData(str, Defines.HTML_TYPE, Defines.ENCODING)
                }
            )

            if (isDualMode) {
                val containerId = (view.parent as? ViewGroup)?.id
                if (containerId == R.id.primary_fragment_container) {
                    // Ensure a code fragment is always in the primary fragment slot when spanned
                    startCodeFragment()
                } else {
                    initializeDualScreen(view)
                }
            } else {
                initializeSingleScreen()
            }
        }
    }

    // spanned selection helper
    private fun initializeSingleScreen() {
        editorBtn.visibility = View.VISIBLE
        previewBtn.visibility = View.VISIBLE
        initializeDragListener()

        editorBtn.setOnClickListener {
            startCodeFragment()
        }
    }

    // spanned selection helper
    private fun initializeDualScreen(view: View) {
        scrollingBuffer = Defines.DEFAULT_BUFFER_SIZE
        scrollRange = Defines.DEFAULT_RANGE
        rangeFound = false

        editorBtn.visibility = View.GONE
        previewBtn.visibility = View.GONE

        // set event and data listeners
        scrollView = view.findViewById(R.id.scrollview_preview)
        scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            handleScrolling(false, scrollY)
        }

        scrollVM.getScroll().observe(
            requireActivity(),
            { state ->
                if (state.scrollKey != Defines.PREVIEW_KEY) {
                    handleScrolling(true, state.scrollPercentage)
                }
            }
        )
    }

    // method that triggers transition to code fragment
    private fun startCodeFragment() {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.primary_fragment_container,
                CodeFragment(),
                "CodeFragment"
            ).addToBackStack("CodeFragment")
            .commit()
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

    // create a drop target for the preview screen
    private fun initializeDragListener() {
        val handler = DragHandler(requireActivity(), webVM, requireActivity().contentResolver)
        val target = webView

        target.setOnDragListener { _, event ->
            handler.onDrag(event)
        }
    }
}
