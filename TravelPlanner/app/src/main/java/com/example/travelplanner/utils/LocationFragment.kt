package com.example.travelplanner.utils

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.travelplanner.R
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.models.SerializableLocation
import com.microsoft.maps.search.MapLocationFinder
import com.microsoft.maps.search.MapLocationFinderResult
import com.microsoft.maps.search.MapLocationOptions
import com.microsoft.maps.search.OnMapLocationFinderResultListener
import kotlin.math.max
import kotlin.math.min

class LocationFragment(var onSelectLocationListener: OnSelectLocationListener? = null) : Fragment(), View.OnClickListener, OnMapLocationFinderResultListener {
    private lateinit var rootView: View
    private lateinit var addressText: TextView
    private lateinit var labelText: TextView
    private lateinit var counterText: TextView
    private lateinit var queryInput: EditText

    private var locationList = ArrayList<SerializableLocation>()
    private var selectedLocationIndex = 0
        set(value) {
            field = max(min(value, locationList.size - 1), 0)
            if (locationList.isNotEmpty()) {
                addressText.text = locationList[selectedLocationIndex].formattedAddress
                counterText.text = activity?.getString(R.string.text_map_search_results)?.let {
                    String.format(
                        it, selectedLocationIndex + 1, locationList.size
                    )
                }
                onSelectLocationListener?.onSelectLocation(locationList[selectedLocationIndex])
            } else {
                addressText.text = activity?.getString(R.string.text_no_address)
                counterText.text = activity?.getString(R.string.text_no_map_results)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_location, container, false)

        addressText = rootView.findViewById(R.id.location_text_address)
        labelText = rootView.findViewById(R.id.location_text_label)
        counterText = rootView.findViewById(R.id.location_text_counter)
        rootView.findViewById<Button>(R.id.location_button_search).setOnClickListener(this)
        rootView.findViewById<Button>(R.id.location_button_previous).setOnClickListener(this)
        rootView.findViewById<Button>(R.id.location_box_next).setOnClickListener(this)
        queryInput = rootView.findViewById(R.id.location_query_input)

        return rootView
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.location_button_search -> {
                if (queryInput.text.length > 2) {
                    val imm = rootView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(rootView.windowToken, 0)

                    val mapLocationOptions = MapLocationOptions()
                    mapLocationOptions.setMaxResults(20)
                    val mapReferencePoint = (activity as TravelPlannerInterface).tpMapFragment.mapView.center
                    MapLocationFinder.findLocations(
                        queryInput.text.toString(),
                        mapReferencePoint,
                        mapLocationOptions,
                        this
                    )
                    Toast.makeText(activity, activity?.getString(R.string.toast_bingmaps_searching), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, activity?.getString(R.string.toast_bingmaps_invalid), Toast.LENGTH_SHORT).show()
                }
            }
            R.id.location_box_next -> {
                selectedLocationIndex += 1
            }
            R.id.location_button_previous -> {
                selectedLocationIndex -= 1
            }
        }
    }

    override fun onMapLocationFinderResult(mapLocationFinderResult: MapLocationFinderResult) {
        activity?.runOnUiThread {
            locationList = ArrayList()
            for (mapLocation in mapLocationFinderResult.locations) {
                locationList.add(SerializableLocation(mapLocation))
            }
            selectedLocationIndex = 0
        }
    }

    fun setLabel(inStr: String) {
        labelText.text = inStr
    }

    fun setLocation(serializableLocation: SerializableLocation?) {
        locationList = ArrayList()
        if (serializableLocation != null) {
            locationList.add(serializableLocation)
        }
        selectedLocationIndex = 0
        queryInput.text.clear()
    }

    fun getLocation(): SerializableLocation? {
        return if (locationList.isNotEmpty()) {
            locationList[selectedLocationIndex]
        } else {
            null
        }
    }

    interface OnSelectLocationListener {
        fun onSelectLocation(serializableLocation: SerializableLocation) {}
    }
}
