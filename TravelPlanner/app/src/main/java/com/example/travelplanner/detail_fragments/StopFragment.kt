package com.example.travelplanner.detail_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.travelplanner.DateUtil
import com.example.travelplanner.MainActivity
import com.example.travelplanner.MapFragment
import com.example.travelplanner.R
import com.example.travelplanner.RangeValidator
import com.example.travelplanner.Stop
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.models.OnReverseGeocodeResultListener
import com.example.travelplanner.models.SerializableLocation
import com.example.travelplanner.models.narrowReverseGeocode
import com.example.travelplanner.utils.LocationFragment
import com.example.travelplanner.utils.ObserverUtils.Companion.observeOnce
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.microsoft.maps.Geopoint

class StopFragment :
    Fragment(),
    View.OnClickListener,
    LocationFragment.OnSelectLocationListener,
    MapFragment.OnMapEventListener {
    private lateinit var rootView: View

    private lateinit var nameInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var dayInput: TextInputEditText
    private lateinit var anyDayInput: MaterialCheckBox
    private lateinit var location: LocationFragment

    private lateinit var nameLayout: TextInputLayout
    private lateinit var dayLayout: TextInputLayout
    private lateinit var locLayout: TextInputLayout

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()
    private lateinit var stop: Stop
    private var destId: Long = 0

    private var datePickerBuilder = MaterialDatePicker.Builder.datePicker().setTheme(R.style.ThemeOverlay_MaterialComponents_MaterialCalendar_Fullscreen).setTitleText(
        activity?.getString(
            R.string.text_datepicker_day
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_stop, container, false)

        nameInput = rootView.findViewById(R.id.stop_name)
        notesInput = rootView.findViewById(R.id.stop_notes)
        dayInput = rootView.findViewById(R.id.stop_day)
        anyDayInput = rootView.findViewById(R.id.stop_any_day)

        nameLayout = rootView.findViewById(R.id.stop_name_layout)
        dayLayout = rootView.findViewById(R.id.stop_day_layout)
        locLayout = rootView.findViewById(R.id.location_input_address_frame)

        dayInput.setOnClickListener(this)
        anyDayInput.setOnClickListener(this)

        location = childFragmentManager.findFragmentById(R.id.input_stop_addr_loc) as LocationFragment
        activity?.getString(R.string.text_stop_address)?.let { location.setLabel(it) }
        location.onSelectLocationListener = this

        val saveButton: MaterialButton = rootView.findViewById(R.id.button_save_stop)
        saveButton.setOnClickListener(this)

        val imageButton: MaterialButton = rootView.findViewById(R.id.button_picture)
        imageButton.setOnClickListener(this)

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()
        (activity as TravelPlannerInterface).tpMapFragment.radius = 200.0 // meters
        (activity as TravelPlannerInterface).tpMapFragment.onMapEventListener = this

        tpViewModel.selectedDestId?.let {
            destId = it
            tpViewModel.getDestination(it).observeOnce(
                viewLifecycleOwner,
                Observer { destWithStops ->
                    val destStartDay = destWithStops.destination.destStartDay
                    val destEndDay = destWithStops.destination.destEndDay
                    val constraintBuilder = CalendarConstraints.Builder().setStart(destStartDay).setEnd(destEndDay)
                    constraintBuilder.setValidator(RangeValidator(destStartDay, destEndDay))
                    datePickerBuilder.setCalendarConstraints(constraintBuilder.build())
                }
            )
        }

        if (tpViewModel.editing) {
            tpViewModel.selectedStopId?.let {
                tpViewModel.getStop(it).observeOnce(
                    viewLifecycleOwner,
                    Observer { stop_ ->
                        (activity as TravelPlannerInterface).tpToolbar.title =
                            activity?.getString(R.string.activity_title_stop)?.let { str ->
                                String.format(
                                    str, stop_.stopName
                                )
                            }
                        stop = stop_
                        nameInput.setText(stop.stopName)
                        notesInput.setText(stop.stopNotes)
                        anyDayInput.isChecked = stop.stopAnyDay
                        if (!stop.stopAnyDay) {
                            datePickerBuilder.setSelection(stop.stopDay)
                            dayInput.setText(DateUtil.longToDate(stop.stopDay))
                            dayInput.isEnabled = true
                        }
                        location.setLocation(SerializableLocation.decode(stop.stopLocation))
                    }
                )
            }
        } else {
            (activity as TravelPlannerInterface).tpToolbar.title = activity?.getString(R.string.activity_title_stop_new)
            tpViewModel.selectedDayIndex.observeOnce(
                viewLifecycleOwner,
                Observer { dayIndex ->
                    val day = (activity as MainActivity).getDayFromIndex(dayIndex)
                    if (day != null) {
                        dayInput.setText(DateUtil.longToDate(day))
                        datePickerBuilder.setSelection(day)
                    }

                    if (dayIndex == TravelPlannerViewModel.SELECTED_ANY_DAY) {
                        anyDayInput.isChecked = true
                    }
                }
            )
            if (tpViewModel.preloadLocation != null) {
                location.setLocation(tpViewModel.preloadLocation)
            }
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if ((activity as TravelPlannerInterface).tpMapFragment.onMapEventListener == this) {
            (activity as TravelPlannerInterface).tpMapFragment.onMapEventListener = null
        }
    }

    private fun showDatePicker() {
        if (!dayInput.text.toString().isEmpty()) {
            datePickerBuilder.setSelection(DateUtil.dateToLong(dayInput.text.toString()))
        }

        val datePicker = datePickerBuilder.build()
        activity?.supportFragmentManager?.let { datePicker.show(it, "dateRange") }

        datePicker.addOnPositiveButtonClickListener {
            dayInput.setText(DateUtil.longToDate(it))
        }
    }

    private fun showInputErrors() {
        nameLayout.isErrorEnabled = true
        dayLayout.isErrorEnabled = true
        locLayout.isErrorEnabled = true

        nameLayout.error = getString(R.string.text_required_field)
        dayLayout.error = getString(R.string.text_required_field)
        locLayout.error = getString(R.string.text_required_field)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_save_stop -> {
                if (nameInput.text.toString().isEmpty() ||
                    (dayInput.text.toString().isEmpty() && !anyDayInput.isChecked) ||
                    location.getLocation() == null
                ) {
                    showInputErrors()
                    Toast.makeText(activity, activity?.getString(R.string.toast_invalid_stop), Toast.LENGTH_SHORT).show()
                } else {

                    if (!tpViewModel.editing) {
                        val stop = Stop(
                            parentDestId = destId,
                            stopName = nameInput.text.toString(),
                            stopNotes = notesInput.text.toString(),
                            stopDay = DateUtil.dateToLong(dayInput.text.toString()),
                            stopAnyDay = anyDayInput.isChecked,
                            stopLocation = SerializableLocation.encode(location.getLocation())
                        )

                        tpViewModel.insertStop(stop)
                    } else {
                        stop.stopName = nameInput.text.toString()
                        stop.stopNotes = notesInput.text.toString()
                        stop.stopDay = DateUtil.dateToLong(dayInput.text.toString())
                        stop.stopAnyDay = anyDayInput.isChecked
                        stop.stopLocation = SerializableLocation.encode(location.getLocation())

                        tpViewModel.insertStop(stop)
                    }
                    tpViewModel.editing = false
                    activity?.onBackPressed()
                }
            }
            R.id.stop_day -> {
                showDatePicker()
            }
            R.id.stop_any_day -> {
                dayInput.isEnabled = !anyDayInput.isChecked
            }
        }
    }

    override fun onSelectLocation(serializableLocation: SerializableLocation) {
        (activity as TravelPlannerInterface).tpMapFragment.removePin("tap_location")
        (activity as TravelPlannerInterface).tpMapFragment.setPin("stop_location", serializableLocation.getGeopoint(), "HIGHLIGHT")
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()
    }

    override fun onLocationTapped(geopoint: Geopoint) {
        (activity as TravelPlannerInterface).tpMapFragment.removePin("stop_location")
        (activity as TravelPlannerInterface).tpMapFragment.setPin("tap_location", geopoint, "SEARCH")
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()

        val listener = OnReverseGeocodeResultListener {
            activity?.runOnUiThread {
                var serializableLocation = if (it != null) SerializableLocation(it) else null
                location.setLocation(serializableLocation)
            }
        }
        narrowReverseGeocode(listener, geopoint)
    }
}
