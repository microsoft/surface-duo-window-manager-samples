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
import com.example.travelplanner.Destination
import com.example.travelplanner.DestinationWithStops
import com.example.travelplanner.MapFragment
import com.example.travelplanner.R
import com.example.travelplanner.RangeValidator
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.models.OnReverseGeocodeResultListener
import com.example.travelplanner.models.SerializableLocation
import com.example.travelplanner.models.wideReverseGeocode
import com.example.travelplanner.utils.LocationFragment
import com.example.travelplanner.utils.ObserverUtils.Companion.observeOnce
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.microsoft.maps.Geopoint

class DestEditFragment : Fragment(), View.OnClickListener, MapFragment.OnMapEventListener {
    private lateinit var rootView: View

    private lateinit var nameInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var destLocation: LocationFragment
    private lateinit var accomLocation: LocationFragment

    private lateinit var nameLayout: TextInputLayout
    private lateinit var startDateLayout: TextInputLayout
    private lateinit var endDateLayout: TextInputLayout
    private lateinit var locLayout: TextInputLayout

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()
    private lateinit var destWithStops: DestinationWithStops
    private var tripId: Long = 0

    private var datePickerBuilder = MaterialDatePicker.Builder.dateRangePicker().setTheme(R.style.ThemeOverlay_MaterialComponents_MaterialCalendar_Fullscreen).setTitleText(
        activity?.getString(
            R.string.text_datepicker_range
        )
    )
    private var dateChanged: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_dest_edit, container, false)

        nameInput = rootView.findViewById(R.id.dest_name)
        notesInput = rootView.findViewById(R.id.dest_notes)
        startDateInput = rootView.findViewById(R.id.dest_start_date)
        endDateInput = rootView.findViewById(R.id.dest_end_date)

        nameLayout = rootView.findViewById(R.id.dest_name_layout)
        startDateLayout = rootView.findViewById(R.id.dest_start_date_layout)
        endDateLayout = rootView.findViewById(R.id.dest_end_date_layout)
        locLayout = rootView.findViewById(R.id.location_input_address_frame)

        destLocation = childFragmentManager.findFragmentById(R.id.input_addr_loc) as LocationFragment
        accomLocation = childFragmentManager.findFragmentById(R.id.input_addr_accom) as LocationFragment
        activity?.getString(R.string.text_dest_address)?.let { destLocation.setLabel(it) }
        destLocation.onSelectLocationListener = DestSelectLocationListener()
        activity?.getString(R.string.text_accom_address)?.let { accomLocation.setLabel(it) }
        accomLocation.onSelectLocationListener = AccomSelectLocationListener()

        startDateInput.setOnClickListener(this)
        endDateInput.setOnClickListener(this)

        val saveButton: MaterialButton = rootView.findViewById(R.id.button_save_dest)
        saveButton.setOnClickListener(this)

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()
        (activity as TravelPlannerInterface).tpMapFragment.onMapEventListener = this

        tpViewModel.selectedTripId?.let {
            tripId = it
            tpViewModel.getTrip(it).observeOnce(
                viewLifecycleOwner,
                Observer { tripWithDestAndStop ->
                    val minDay = tripWithDestAndStop.trip.tripStartDay
                    val maxDay = tripWithDestAndStop.trip.tripEndDay

                    val constraintBuilder = CalendarConstraints.Builder().setStart(minDay).setEnd(maxDay)
                    constraintBuilder.setValidator(RangeValidator(minDay, maxDay))
                    datePickerBuilder.setCalendarConstraints(constraintBuilder.build())
                }
            )
        }

        if (tpViewModel.editing) {
            tpViewModel.selectedDestId?.let {
                tpViewModel.getDestination(it).observeOnce(
                    viewLifecycleOwner,
                    Observer { destWithStops_ ->
                        (activity as TravelPlannerInterface).tpToolbar.title =
                            activity?.getString(R.string.activity_title_dest_edit)?.let { str ->
                                String.format(
                                    str, destWithStops_.destination.destName
                                )
                            }
                        destWithStops = destWithStops_
                        val destination = destWithStops.destination
                        nameInput.setText(destination.destName)
                        notesInput.setText(destination.destNotes)

                        datePickerBuilder.setSelection(androidx.core.util.Pair(destination.destStartDay, destination.destEndDay))
                        startDateInput.setText(DateUtil.longToDate(destination.destStartDay))
                        endDateInput.setText(DateUtil.longToDate(destination.destEndDay))
                        destLocation.setLocation(SerializableLocation.decode(destination.destLocation))
                        accomLocation.setLocation(SerializableLocation.decode(destination.accomLocation))
                    }
                )
            }
        } else {
            (activity as TravelPlannerInterface).tpToolbar.title = activity?.getString(R.string.activity_title_dest_new)

            if (tpViewModel.preloadLocation != null) {
                destLocation.setLocation(tpViewModel.preloadLocation)
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    private fun showDateRangePicker() {
        if (!startDateInput.text.toString().isEmpty() && !endDateInput.text.toString().isEmpty()) {
            datePickerBuilder.setSelection(
                androidx.core.util.Pair(
                    DateUtil.dateToLong(startDateInput.text.toString()),
                    DateUtil.dateToLong(endDateInput.text.toString())
                )
            )
        }

        val datePicker = datePickerBuilder.build()
        activity?.supportFragmentManager?.let { datePicker.show(it, "dateRange") }

        datePicker.addOnPositiveButtonClickListener {
            dateChanged = true
            startDateInput.setText(DateUtil.longToDate(it.first))
            endDateInput.setText(DateUtil.longToDate(it.second))
        }
    }

    private fun showInputErrors() {
        nameLayout.isErrorEnabled = true
        startDateLayout.isErrorEnabled = true
        endDateLayout.isErrorEnabled = true
        locLayout.isErrorEnabled = true

        nameLayout.error = getString(R.string.text_required_field)
        startDateLayout.error = getString(R.string.text_required_field)
        endDateLayout.error = getString(R.string.text_required_field)
        locLayout.error = getString(R.string.text_required_field)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_save_dest -> {
                if (nameInput.text.toString().isEmpty() ||
                    startDateInput.text.toString().isEmpty() ||
                    endDateInput.text.toString().isEmpty() ||
                    destLocation.getLocation() == null
                ) {
                    showInputErrors()
                    Toast.makeText(activity, activity?.getString(R.string.toast_invalid_dest), Toast.LENGTH_SHORT).show()
                } else {
                    if (!tpViewModel.editing) {
                        val destination = Destination(
                            parentTripId = tripId,
                            destName = nameInput.text.toString(),
                            destNotes = notesInput.text.toString(),
                            destStartDay = DateUtil.dateToLong(startDateInput.text.toString()),
                            destEndDay = DateUtil.dateToLong(endDateInput.text.toString()),
                            destLocation = SerializableLocation.encode(destLocation.getLocation()),
                            accomLocation = SerializableLocation.encode(accomLocation.getLocation())
                        )

                        tpViewModel.insertDestination(destination)
                    } else {
                        destWithStops.destination.destName = nameInput.text.toString()
                        destWithStops.destination.destNotes = notesInput.text.toString()
                        destWithStops.destination.destStartDay =
                            DateUtil.dateToLong(startDateInput.text.toString())
                        destWithStops.destination.destEndDay =
                            DateUtil.dateToLong(endDateInput.text.toString())
                        destWithStops.destination.destLocation = SerializableLocation.encode(destLocation.getLocation())
                        destWithStops.destination.accomLocation = SerializableLocation.encode(accomLocation.getLocation())

                        if (dateChanged) {
                            tpViewModel.destDateChanged(destWithStops)
                        } else {
                            tpViewModel.insertDestination(destWithStops.destination)
                        }
                    }
                    tpViewModel.editing = false
                    activity?.onBackPressed()
                }
            }
            R.id.dest_start_date, R.id.dest_end_date -> {
                showDateRangePicker()
            }
        }
    }

    inner class DestSelectLocationListener : LocationFragment.OnSelectLocationListener {
        override fun onSelectLocation(serializableLocation: SerializableLocation) {
            (activity as TravelPlannerInterface).tpMapFragment.removePin("tap_location")
            (activity as TravelPlannerInterface).tpMapFragment.setPin("dest_location", serializableLocation.getGeopoint(), "HIGHLIGHT")
            (activity as TravelPlannerInterface).tpMapFragment.resetScene()
        }
    }

    inner class AccomSelectLocationListener : LocationFragment.OnSelectLocationListener {
        override fun onSelectLocation(serializableLocation: SerializableLocation) {
            (activity as TravelPlannerInterface).tpMapFragment.removePin("tap_location")
            (activity as TravelPlannerInterface).tpMapFragment.setPin("accom_location", serializableLocation.getGeopoint(), "HIGHLIGHT")
            (activity as TravelPlannerInterface).tpMapFragment.resetScene()
        }
    }

    override fun onLocationTapped(geopoint: Geopoint) {
        (activity as TravelPlannerInterface).tpMapFragment.removePin("dest_location")
        (activity as TravelPlannerInterface).tpMapFragment.setPin("tap_location", geopoint, "SEARCH")
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()

        val listener = OnReverseGeocodeResultListener {
            activity?.runOnUiThread {
                var serializableLocation = if (it != null) SerializableLocation(it) else null
                destLocation.setLocation(serializableLocation)
            }
        }
        wideReverseGeocode(listener, geopoint)
    }
}
