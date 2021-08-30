package com.microsoft.device.display.wm_samples.photoeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DualScreenToolsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate view
        val view = inflater.inflate(R.layout.tools_dual_screen, container, false)

        // Set up controls
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            it.initializeViews(view, doImage = false)
            it.setupLayout(doImage = false)
        }

        return view
    }
}
