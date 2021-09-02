package com.example.travelplanner.detail_fragments

import android.content.ClipData
import android.content.ClipDescription
import android.content.Intent
import android.net.Uri
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
import com.example.travelplanner.DestinationWithStops
import com.example.travelplanner.MainActivity
import com.example.travelplanner.MapFragment
import com.example.travelplanner.R
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerMode
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.models.OnReverseGeocodeResultListener
import com.example.travelplanner.models.SerializableLocation
import com.example.travelplanner.models.wideReverseGeocode
import com.example.travelplanner.utils.CardAdapter
import com.example.travelplanner.utils.DragDropUtils
import com.example.travelplanner.utils.RecyclerBaseAdapter
import com.google.android.material.button.MaterialButton
import com.microsoft.maps.Geopoint

class TripFragment :
    Fragment(),
    RecyclerBaseAdapter.OnRecyclerEventListener,
    MapFragment.OnMapEventListener,
    CardAdapter.OnCardEventListener,
    View.OnClickListener {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var destAdapter: RecyclerBaseAdapter

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()
    private var dests = emptyList<DestinationWithStops>()
    private var selectedDest = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_trip, container, false)

        rootView.findViewById<MaterialButton>(R.id.button_add).setOnClickListener(this)

        destAdapter = RecyclerBaseAdapter(activity as AppCompatActivity, this)
        recyclerView = rootView.findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = destAdapter

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()
        (activity as TravelPlannerInterface).tpMapFragment.radius = 10000.0 // meters
        (activity as TravelPlannerInterface).tpMapFragment.onMapEventListener = this

        (activity as TravelPlannerInterface).tpFlyoutAdapter.reset()
        (activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener = this

        (activity as TravelPlannerInterface).tpHeaderTitleText.text = activity?.getString(R.string.text_header_dests)

        tpViewModel.selectedTripId?.let {
            tpViewModel.getTrip(it).observe(
                viewLifecycleOwner,
                Observer { tripWithDestAndStop ->
                    (activity as TravelPlannerInterface).tpToolbar.title =
                        activity?.getString(R.string.activity_title_trip)?.let { str ->
                            String.format(
                                str, tripWithDestAndStop.trip.tripName
                            )
                        }
                    (activity as TravelPlannerInterface).tpHeaderNotesText.text = tripWithDestAndStop.trip.tripNotes

                    // var importExportTrip = ImportExportTrip((activity as MainActivity).applicationContext, tpViewModel)
                    // importExportTrip.exportTrip(tripWithDestAndStop, "exported_trip.json")

                    updateDestList(tripWithDestAndStop.destinations)
                }
            )
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if ((activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener == this) {
            (activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener = null
        }
        if ((activity as TravelPlannerInterface).tpMapFragment.onMapEventListener == this) {
            (activity as TravelPlannerInterface).tpMapFragment.onMapEventListener = null
        }
    }

    private fun updateDestList(dests: List<DestinationWithStops>) {
        this.dests = dests

        // show empty message if no dests
        val emptyText = rootView.findViewById<TextView>(R.id.empty_dests)
        if (dests.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
        }

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()
        for ((index, dest) in dests.withIndex()) {
            val location = SerializableLocation.decode(dest.destination.destLocation)
            if (location != null) {
                (activity as TravelPlannerInterface).tpMapFragment.setPin(index.toString(), location.getGeopoint(), "NORMAL")
            }
        }
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()
        destAdapter.items = dests
        destAdapter.selectedPosition = 0
        this.onItemSelected(0)
    }

    override fun onItemSelected(position: Int) {
        if (position >= 0 && position < dests.size) {
            selectedDest = position
            (activity as TravelPlannerInterface).tpFlyoutAdapter.attach(dests[position])
            (activity as TravelPlannerInterface).tpMapFragment.setPin(position.toString(), null, "HIGHLIGHT")
        }
    }

    override fun onItemDeselected(position: Int) {
        if (position >= 0 && position < dests.size) {
            (activity as TravelPlannerInterface).tpMapFragment.setPin(position.toString(), null, "NORMAL")
        }
    }

    override fun onItemOpened(position: Int) {
        tpViewModel.setSelectedDest(dests[selectedDest])
        (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.DEST)
    }

    override fun onItemLaunched(position: Int) {
        val location = SerializableLocation.decode(dests[selectedDest].destination.destLocation)
        if (location != null) {
            val gmmIntentURI = Uri.parse("geo:${location.lat},${location.long}?q=${location.formattedAddress}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentURI)
            mapIntent.flags = Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT + Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(mapIntent)
        } else {
            Toast.makeText(activity, activity?.getString(R.string.toast_maps_fail), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemDeleted(position: Int) {
        tpViewModel.deleteDestination(dests[selectedDest].destination)
        val toastMessage = activity?.getString(R.string.toast_delete_dest)
        Toast.makeText(
            activity,
            toastMessage?.let { String.format(it, dests[position].destination.destName) }, Toast.LENGTH_SHORT
        ).show()
    }

    override fun onItemTravelClicked(position: Int) {
        tpViewModel.setSelectedDest(dests[selectedDest])
        (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.TRAVEL)
    }

    override fun onItemLongClicked(position: Int, view: View) {
        var dest = dests[position]
        startDragAndDrop(dest, view)
    }

    private fun startDragAndDrop(destWithStops: DestinationWithStops, view: View) {
        var dd = DragDropUtils((activity as MainActivity).applicationContext)
        val clipDataItem = ClipData.Item(dd.destToText(destWithStops), dd.destToHtml(destWithStops))
        val mimeType = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML)
        val clipData = ClipData("dest_drag_drop", mimeType, clipDataItem)
        val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        view.startDragAndDrop(clipData, View.DragShadowBuilder(view), view, flags)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_add -> {
                tpViewModel.editing = false
                tpViewModel.preloadLocation = null
                (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.DEST_EDIT)
            }
        }
    }

    override fun onPinSelected(tag: String) {
        val index = tag.toInt()
        if (index >= 0 && index < dests.size) {
            selectedDest = index
            destAdapter.selectedPosition = index
            (activity as TravelPlannerInterface).tpFlyoutAdapter.attach(dests[index])
        }
    }

    override fun onLocationTapped(geopoint: Geopoint) {
        val listener = OnReverseGeocodeResultListener {
            activity?.runOnUiThread {
                if (it != null) {
                    tpViewModel.editing = false
                    tpViewModel.preloadLocation = SerializableLocation(it)
                    (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.DEST_EDIT)
                }
            }
        }

        (activity as TravelPlannerInterface).tpMapFragment.setPin("search_location", geopoint, "SEARCH")
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()

        wideReverseGeocode(listener, geopoint)

        Toast.makeText(activity, activity?.getString(R.string.toast_add_map_dest), Toast.LENGTH_SHORT).show()
    }

    override fun onCardOpenClicked() = onItemOpened(selectedDest)

    override fun onCardLaunchClicked() = onItemLaunched(selectedDest)

    override fun onCardDeleteClicked() = onItemDeleted(selectedDest)

    override fun onCardTravelClicked() = onItemTravelClicked(selectedDest)

    override fun onCardLongClicked(view: View) = onItemLongClicked(selectedDest, view)
}
