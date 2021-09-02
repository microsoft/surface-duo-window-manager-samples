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
import com.example.travelplanner.R
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.Trip
import com.example.travelplanner.TripWithDestAndStop
import com.example.travelplanner.utils.ObserverUtils.Companion.observeOnce
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TripEditFragment : Fragment(), View.OnClickListener {
    private lateinit var rootView: View
    private lateinit var nameInput: TextInputEditText
    private lateinit var notesInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText

    private lateinit var nameLayout: TextInputLayout
    private lateinit var startDateLayout: TextInputLayout
    private lateinit var endDateLayout: TextInputLayout

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()

    private lateinit var tripWithDestAndStop: TripWithDestAndStop

    private var datePickerBuilder = MaterialDatePicker.Builder.dateRangePicker().setTheme(R.style.ThemeOverlay_MaterialComponents_MaterialCalendar_Fullscreen).setTitleText(
        activity?.getString(
            R.string.text_datepicker_range
        )
    )
    private var dateChanged = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_trip_edit, container, false)

        nameLayout = rootView.findViewById<TextInputLayout>(R.id.trip_name_layout)
        startDateLayout = rootView.findViewById<TextInputLayout>(R.id.trip_start_date_layout)
        endDateLayout = rootView.findViewById<TextInputLayout>(R.id.trip_end_date_layout)

        nameInput = rootView.findViewById(R.id.trip_name)
        notesInput = rootView.findViewById(R.id.trip_notes)
        startDateInput = rootView.findViewById(R.id.trip_start_date)
        endDateInput = rootView.findViewById(R.id.trip_end_date)

        startDateInput.setOnClickListener(this)
        endDateInput.setOnClickListener(this)

        val saveButton: MaterialButton = rootView.findViewById(R.id.trip_save_button)
        saveButton.setOnClickListener(this)

        if (tpViewModel.editing) {
            tpViewModel.selectedTripId?.let {
                tpViewModel.getTrip(it).observeOnce(
                    viewLifecycleOwner,
                    Observer { tripWithDestAndStop_ ->
                        tripWithDestAndStop = tripWithDestAndStop_
                        var trip = tripWithDestAndStop.trip
                        (activity as TravelPlannerInterface).tpToolbar.title =
                            activity?.getString(R.string.activity_title_trip_edit)?.let { str ->
                                String.format(
                                    str, trip.tripName
                                )
                            }
                        nameInput.setText(trip.tripName)
                        notesInput.setText(trip.tripNotes)
                        datePickerBuilder.setSelection(androidx.core.util.Pair(trip.tripStartDay, trip.tripEndDay))
                        startDateInput.setText(DateUtil.longToDate(trip.tripStartDay))
                        endDateInput.setText(DateUtil.longToDate(trip.tripEndDay))
                    }
                )
            }
        } else {
            (activity as TravelPlannerInterface).tpToolbar.title = activity?.getString(R.string.activity_title_trip_new)
        }

        return rootView
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

        nameLayout.error = getString(R.string.text_required_field)
        startDateLayout.error = getString(R.string.text_required_field)
        endDateLayout.error = getString(R.string.text_required_field)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // save button callback
    }
    override fun onClick(view: View) {
        when (view.id) {
            R.id.trip_save_button -> {
                if (nameInput.text.toString().isEmpty() ||
                    startDateInput.text.toString().isEmpty() ||
                    endDateInput.text.toString().isEmpty()
                ) {
                    showInputErrors()
                    Toast.makeText(activity, activity?.getString(R.string.toast_invalid_trip), Toast.LENGTH_SHORT).show()
                } else {
                    if (!tpViewModel.editing) {
                        var trip: Trip = Trip(
                            tripName = nameInput.text.toString(),
                            tripNotes = notesInput.text.toString(),
                            tripStartDay = DateUtil.dateToLong(startDateInput.text.toString()),
                            tripEndDay = DateUtil.dateToLong(endDateInput.text.toString())
                        )
                        tpViewModel.insertTrip(trip)
                    } else {
                        tripWithDestAndStop.trip.tripName = nameInput.text.toString()
                        tripWithDestAndStop.trip.tripNotes = notesInput.text.toString()
                        tripWithDestAndStop.trip.tripStartDay =
                            DateUtil.dateToLong(startDateInput.text.toString())
                        tripWithDestAndStop.trip.tripEndDay =
                            DateUtil.dateToLong(endDateInput.text.toString())

                        if (dateChanged) {
                            tpViewModel.tripDateChanged(tripWithDestAndStop)
                        } else {
                            tpViewModel.insertTrip(tripWithDestAndStop.trip)
                        }
                    }
                    tpViewModel.editing = false
                    activity?.onBackPressed()
                }
            }
            R.id.trip_start_date, R.id.trip_end_date -> {
                showDateRangePicker()
            }
        }
    }
}
