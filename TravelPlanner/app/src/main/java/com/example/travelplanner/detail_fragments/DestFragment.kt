package com.example.travelplanner.detail_fragments

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.format.DateUtils
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
import com.example.travelplanner.Stop
import com.example.travelplanner.TravelPlannerInterface
import com.example.travelplanner.TravelPlannerMode
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.models.OnReverseGeocodeResultListener
import com.example.travelplanner.models.SerializableLocation
import com.example.travelplanner.models.narrowReverseGeocode
import com.example.travelplanner.utils.CardAdapter
import com.example.travelplanner.utils.DragDropUtils
import com.example.travelplanner.utils.ObserverUtils.Companion.observeOnce
import com.example.travelplanner.utils.RecyclerBaseAdapter
import com.google.android.material.button.MaterialButton
import com.microsoft.maps.Geopoint

class DestFragment :
    Fragment(),
    RecyclerBaseAdapter.OnRecyclerEventListener,
    MapFragment.OnMapEventListener,
    CardAdapter.OnCardEventListener,
    View.OnClickListener {
    private lateinit var rootView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var stopAdapter: RecyclerBaseAdapter

    private val tpViewModel: TravelPlannerViewModel by activityViewModels()
    private var stops = emptyList<Stop>()
    private var allStops = emptyList<Stop>()
    private var selectedStop = 0

    private lateinit var destWithStops: DestinationWithStops
    private var destId: Long = 0

    private var currentDayIndex: Int = 0

    private lateinit var dayList: ArrayList<Long>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_dest, container, false)

        rootView.findViewById<MaterialButton>(R.id.button_add).setOnClickListener(this)

        stopAdapter = RecyclerBaseAdapter(activity as AppCompatActivity, this)
        recyclerView = rootView.findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = stopAdapter

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()
        (activity as TravelPlannerInterface).tpMapFragment.radius = 1000.0 // meters
        (activity as TravelPlannerInterface).tpMapFragment.onMapEventListener = this

        (activity as TravelPlannerInterface).tpFlyoutAdapter.reset()
        (activity as TravelPlannerInterface).tpFlyoutAdapter.onCardEventListener = this

        (activity as TravelPlannerInterface).tpHeaderTitleText.text = getText(R.string.text_header_stops)

        dayList = ArrayList<Long>()

        tpViewModel.selectedDestId?.let {
            destId = it
            tpViewModel.getDestination(it).observeOnce(
                viewLifecycleOwner,
                Observer { destWithStops_ ->
                    destWithStops = destWithStops_
                    (activity as TravelPlannerInterface).tpToolbar.title =
                        activity?.getString(R.string.activity_title_dest)?.let { str ->
                            String.format(
                                str, destWithStops.destination.destName
                            )
                        }
                    (activity as TravelPlannerInterface).tpHeaderNotesText.text = destWithStops.destination.destNotes

                    // calculate all days in destination
                    var day = destWithStops.destination.destStartDay
                    while (day <= destWithStops.destination.destEndDay) {
                        dayList.add(day)
                        day += DateUtils.DAY_IN_MILLIS
                    }
                    (activity as MainActivity).updateTabView(dayList)

                    tpViewModel.getStops(destId).observe(
                        viewLifecycleOwner,
                        Observer { stops ->
                            allStops = stops
                            updateStopList(currentDayIndex)
                        }
                    )

                    tpViewModel.selectedDayIndex.observe(
                        viewLifecycleOwner,
                        Observer { dayIndex ->
                            currentDayIndex = dayIndex
                            updateStopList(dayIndex)
                        }
                    )
                }
            )
        }

        // temp fix for the tabview to scroll back to beginning
        (activity as MainActivity).selectTab(0)

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

    private fun updateStopList(dayIndex: Int) {

        if (dayIndex == TravelPlannerViewModel.SELECTED_ALL_STOPS) {
            stops = allStops
        } else if (dayIndex == TravelPlannerViewModel.SELECTED_ANY_DAY) {
            stops = tpViewModel.getAnyDayStops(destId)
        } else {
            stops = tpViewModel.getDayStops(destId, dayList[dayIndex])
        }

        // show empty message if no stops
        val emptyText = rootView.findViewById<TextView>(R.id.empty_stops)
        if (stops.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
        }

        (activity as TravelPlannerInterface).tpMapFragment.clearPins()
        for ((index, stop) in stops.withIndex()) {
            val location = SerializableLocation.decode(stop.stopLocation)
            if (location != null) {
                (activity as TravelPlannerInterface).tpMapFragment.setPin(index.toString(), location.getGeopoint(), "NORMAL")
            }
        }
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()
        stopAdapter.items = stops
        stopAdapter.selectedPosition = 0
        this.onItemSelected(0)
    }

    override fun onItemOpened(position: Int) {
        tpViewModel.setSelectedStop(stops[selectedStop])
        tpViewModel.editing = true
        (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.STOP)
    }

    override fun onItemSelected(position: Int) {
        if (position >= 0 && position < stops.size) {
            selectedStop = position
            (activity as TravelPlannerInterface).tpFlyoutAdapter.attach(stops[position])
            (activity as TravelPlannerInterface).tpMapFragment.setPin(position.toString(), null, "HIGHLIGHT")
        }
    }

    override fun onItemDeselected(position: Int) {
        if (position >= 0 && position < stops.size) {
            (activity as TravelPlannerInterface).tpMapFragment.setPin(position.toString(), null, "NORMAL")
        }
    }

    override fun onItemClipped(position: Int) {
        val address = SerializableLocation.decode(stops[selectedStop].stopLocation)?.formattedAddress
        if (address != null) {
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(activity?.getString(R.string.text_stop_address), address))
            Toast.makeText(activity, activity?.getString(R.string.toast_address_copied), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, activity?.getString(R.string.toast_address_copy_fail), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemLaunched(position: Int) {
        val location = SerializableLocation.decode(stops[selectedStop].stopLocation)
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
        tpViewModel.deleteStop(stops[selectedStop])
        val toastMessage = activity?.getString(R.string.toast_delete_stop)
        Toast.makeText(
            activity,
            toastMessage?.let { String.format(it, stops[position].stopName) }, Toast.LENGTH_SHORT
        ).show()
    }

    override fun onItemLongClicked(position: Int, view: View) {
        var stop = stops[position]
        startDragAndDrop(stop, view)
    }

    private fun startDragAndDrop(stop: Stop, view: View) {
        var dd = DragDropUtils((activity as MainActivity).applicationContext)
        val clipDataItem = ClipData.Item(dd.stopToText(stop), dd.stopToHtml(stop))
        val mimeType = arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN, ClipDescription.MIMETYPE_TEXT_HTML)
        val clipData = ClipData("stop_drag_drop", mimeType, clipDataItem)
        val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        view.startDragAndDrop(clipData, View.DragShadowBuilder(view), view, flags)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.button_add -> {
                tpViewModel.editing = false
                tpViewModel.preloadLocation = null
                (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.STOP)
            }
        }
    }

    override fun onPinSelected(tag: String) {
        val index = tag.toInt()
        if (index >= 0 && index < stops.size) {
            selectedStop = index
            stopAdapter.selectedPosition = index
            (activity as TravelPlannerInterface).tpFlyoutAdapter.attach(stops[index])
        }
    }

    override fun onLocationTapped(geopoint: Geopoint) {
        val listener = OnReverseGeocodeResultListener {
            activity?.runOnUiThread {
                if (it != null) {
                    tpViewModel.editing = false
                    tpViewModel.preloadLocation = SerializableLocation(it)
                    (activity as TravelPlannerInterface).tpLaunchMode(TravelPlannerMode.STOP)
                }
            }
        }

        (activity as TravelPlannerInterface).tpMapFragment.setPin("search_location", geopoint, "SEARCH")
        (activity as TravelPlannerInterface).tpMapFragment.resetScene()

        narrowReverseGeocode(listener, geopoint)

        Toast.makeText(activity, activity?.getString(R.string.toast_add_map_stop), Toast.LENGTH_SHORT).show()
    }

    override fun onCardOpenClicked() = onItemOpened(selectedStop)

    override fun onCardClipClicked() = onItemLaunched(selectedStop)

    override fun onCardLaunchClicked() = onItemLaunched(selectedStop)

    override fun onCardDeleteClicked() = onItemDeleted(selectedStop)

    override fun onCardLongClicked(view: View) = onItemLongClicked(selectedStop, view)
}
