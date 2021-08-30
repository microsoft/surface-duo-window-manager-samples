package com.microsoft.device.display.wm_samples.photoeditor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class DualScreenPictureFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate view
        val view = inflater.inflate(R.layout.picture_dual_screen, container, false)

        // Set up controls
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            it.initializeViews(view, doTools = false)
            it.setupLayout(doTools = false)
        }

        return view
    }
}
