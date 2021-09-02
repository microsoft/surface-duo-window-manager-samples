package com.example.travelplanner.detail_fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.travelplanner.DateUtil
import com.example.travelplanner.R
import com.example.travelplanner.RangeValidator
import com.example.travelplanner.Travel
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class TravelFragment : Fragment(), View.OnClickListener {
    private lateinit var rootView: View

    private lateinit var modeInput: AutoCompleteTextView
    private lateinit var notesInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText

    private lateinit var modeLayout: TextInputLayout
    private lateinit var startDateLayout: TextInputLayout
    private lateinit var endDateLayout: TextInputLayout

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()
    private var destId: Long = 0

    private var datePickerBuilder =
        MaterialDatePicker.Builder.dateRangePicker().setTitleText(activity?.getString(R.string.text_datepicker_range))
    private var dateChanged: Boolean = false

    private lateinit var travel: Travel
    private var travelExists: Boolean = false

    private lateinit var travelModes: Array<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_travel, container, false)

        modeInput = rootView.findViewById(R.id.travel_mode_input)
        notesInput = rootView.findViewById(R.id.travel_notes)
        startDateInput = rootView.findViewById(R.id.travel_start_date)
        endDateInput = rootView.findViewById(R.id.travel_end_date)

        modeLayout = rootView.findViewById(R.id.travel_mode_menu)
        startDateLayout = rootView.findViewById(R.id.travel_start_date_layout)
        endDateLayout = rootView.findViewById(R.id.travel_end_date_layout)

        startDateInput.setOnClickListener(this)
        endDateInput.setOnClickListener(this)

        val saveButton: MaterialButton = rootView.findViewById(R.id.button_save_travel)
        saveButton.setOnClickListener(this)

        tpViewModel.selectedDestId?.let {
            destId = it
            tpViewModel.getDestination(it).observe(
                viewLifecycleOwner,
                Observer { destWithStops ->

                    (activity as TravelPlannerInterface).tpToolbar.title =
                        activity?.getString(R.string.activity_title_travel)?.let { str ->
                            String.format(
                                str, destWithStops.destination.destName
                            )
                        }

                    var minDay = destWithStops.destination.destStartDay
                    var maxDay = destWithStops.destination.destEndDay
                    // block of other destination date ranges?
                    val constraintBuilder = CalendarConstraints.Builder().setStart(minDay).setEnd(maxDay)
                    constraintBuilder.setValidator(RangeValidator(minDay, maxDay))
                    datePickerBuilder.setCalendarConstraints(constraintBuilder.build())

                    if (destWithStops.travel != null) {
                        travelExists = true
                        travel = destWithStops.travel

                        modeInput.setText(travel.travelMode)
                        notesInput.setText(travel.travelNotes)
                        startDateInput.setText(DateUtil.longToDate(travel.travelStartDay))
                        endDateInput.setText(DateUtil.longToDate(travel.travelEndDay))
                        datePickerBuilder.setSelection(androidx.core.util.Pair(travel.travelStartDay, travel.travelEndDay))
                    }

                    travelModes = resources.getStringArray(R.array.travel_modes)
                    val adapter = ArrayAdapter(requireContext(), R.layout.travel_list_item, travelModes)
                    rootView.findViewById<AutoCompleteTextView>(R.id.travel_mode_input).setAdapter(adapter)
                }
            )
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
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
        modeLayout.isErrorEnabled = true
        startDateLayout.isErrorEnabled = true
        endDateLayout.isErrorEnabled = true

        modeLayout.error = getString(R.string.text_required_field)
        startDateLayout.error = getString(R.string.text_required_field)
        endDateLayout.error = getString(R.string.text_required_field)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_save_travel -> {
                if (modeInput.text.toString().isEmpty() ||
                    startDateInput.text.toString().isEmpty() ||
                    endDateInput.text.toString().isEmpty()
                ) {
                    showInputErrors()
                    Toast.makeText(context, activity?.getString(R.string.toast_invalid_travel), Toast.LENGTH_SHORT).show()
                } else {

                    if (travelExists) {
                        travel.travelMode = modeInput.text.toString()
                        travel.travelNotes = notesInput.text.toString()
                        travel.travelStartDay = DateUtil.dateToLong(startDateInput.text.toString())
                        travel.travelEndDay = DateUtil.dateToLong(endDateInput.text.toString())

                        tpViewModel.insertTravel(travel)
                    } else {
                        travel = Travel(
                            travelMode = modeInput.text.toString(),
                            travelNotes = notesInput.text.toString(),
                            travelStartDay = DateUtil.dateToLong(startDateInput.text.toString()),
                            travelEndDay = DateUtil.dateToLong(endDateInput.text.toString()),
                        )
                        travel.parentDestId = destId
                        tpViewModel.insertTravel(travel)
                    }
                    activity?.onBackPressed()
                }
            }
            R.id.travel_start_date, R.id.travel_end_date -> {
                showDateRangePicker()
            }
        }
    }
}
