package com.example.travelplanner.detail_fragments

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipDescription
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelplanner.MainActivity
import com.example.travelplanner.R
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerMode
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.TripWithDestAndStop
import com.example.travelplanner.utils.CardAdapter
import com.example.travelplanner.utils.DragDropUtils
import com.example.travelplanner.utils.RecyclerBaseAdapter
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(), RecyclerBaseAdapter.OnRecyclerEventListener, CardAdapter.OnCardEventListener, View.OnClickListener {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: RecyclerBaseAdapter

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()
    private var trips = emptyList<TripWithDestAndStop>()
    private var selectedTrip = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_home, container, false)

        rootView.findViewById<MaterialButton>(R.id.button_add).setOnClickListener(this)

        (activity as TravelPlannerInterface).tpToolbar.title = activity?.getString(R.string.activity_title_home)

        tripAdapter = RecyclerBaseAdapter(activity as AppCompatActivity, this)
        recyclerView = rootView.findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = tripAdapter

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()

        (activity as TravelPlannerInterface).tpFlyoutAdapter.reset()
        (activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener = this

        tpViewModel.getTripsWithDestAndStop().observe(
            viewLifecycleOwner,
            Observer { trips ->
                trips.let { updateTripList(trips) }
            }
        )

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if ((activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener == this) {
            (activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener = null
        }
    }

    private fun updateTripList(trips: List<TripWithDestAndStop>) {
        this.trips = trips
        tripAdapter.items = trips

        // show empty message if no trips
        val emptyText = rootView.findViewById<TextView>(R.id.empty_trips)
        if (trips.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
        }

        tripAdapter.selectedPosition = 0
        this.onItemSelected(0)
    }

    override fun onItemSelected(position: Int) {
        if (position >= 0 && position < trips.size) {
            selectedTrip = position
            (activity as TravelPlannerInterface).tpFlyoutAdapter.attach(trips[position])
        }
    }

    override fun onItemOpened(position: Int) {
        tpViewModel.setSelectedTrip(trips[position])
        (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.TRIP)
    }

    override fun onItemDeleted(position: Int) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle(
            activity?.getString(R.string.dialog_delete_trip_title)?.let {
                String.format(
                    it, trips[position].trip.tripName
                )
            }
        )
        builder.setMessage(activity?.getString(R.string.dialog_delete_trip_subtitle))

        builder.setPositiveButton(activity?.getString(R.string.dialog_yes)) { dialog, _ -> // Do nothing but close the dialog
            tpViewModel.deleteTrip(trips[position].trip)
            dialog.dismiss()

            val toastMessage = activity?.getString(R.string.toast_delete_trip)
            Toast.makeText(
                activity,
                toastMessage?.let { String.format(it, trips[position].trip.tripName) }, Toast.LENGTH_SHORT
            ).show()
        }

        builder.setNegativeButton(
            activity?.getString(R.string.dialog_no)
        ) { dialog, _ -> // Do nothing
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }

    override fun onItemLongClicked(position: Int, view: View) {
        var trip = trips[position]
        startDragAndDrop(trip, view)
    }

    private fun startDragAndDrop(trip: TripWithDestAndStop, view: View) {
        var dd = DragDropUtils((activity as MainActivity).applicationContext)
        val clipDataItem = ClipData.Item(dd.tripToText(trip), dd.tripToHtml(trip))
        val mimeType = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML)
        val clipData = ClipData("trip_drag_drop", mimeType, clipDataItem)
        val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        view.startDragAndDrop(clipData, View.DragShadowBuilder(view), view, flags)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_add -> (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.TRIP_EDIT)
        }
    }

    override fun onCardOpenClicked() = onItemOpened(selectedTrip)

    override fun onCardDeleteClicked() = onItemDeleted(selectedTrip)

    override fun onCardLongClicked(view: View) = onItemLongClicked(selectedTrip, view)
}
