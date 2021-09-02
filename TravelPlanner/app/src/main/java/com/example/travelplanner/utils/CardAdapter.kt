package com.example.travelplanner.utils

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.travelplanner.DateUtil
import com.example.travelplanner.DestinationWithStops
import com.example.travelplanner.GalleryAdapter
import com.example.travelplanner.R
import com.example.travelplanner.Stop
import com.example.travelplanner.TripWithDestAndStop
import com.example.travelplanner.models.SerializableLocation
import com.google.android.material.chip.Chip
import com.google.android.material.divider.MaterialDivider

class CardAdapter(val rootView: View, val activity: AppCompatActivity, var onCardEventListener: OnCardEventListener? = null) : View.OnClickListener, View.OnLongClickListener {
    private val context = rootView.context
    private val titleText = rootView.findViewById<TextView>(R.id.card_trick_title)
    private val subtitleText = rootView.findViewById<TextView>(R.id.card_trick_subtitle)
    private val contentText = rootView.findViewById<TextView>(R.id.card_trick_content)
    private val divider = rootView.findViewById<MaterialDivider>(R.id.card_trick_divider)
    private val openButton = rootView.findViewById<Button>(R.id.card_trick_button_open)
    private val clipChip = rootView.findViewById<Chip>(R.id.card_trick_button_clip)
    private val launchChip = rootView.findViewById<Chip>(R.id.card_trick_button_launch)
    private val deleteChip = rootView.findViewById<Chip>(R.id.card_trick_button_delete)
    private val travelChip = rootView.findViewById<Chip>(R.id.card_trick_button_travel)
    private val galleryLayout = rootView.findViewById<View>(R.id.card_trick_gallery)
    private val galleryAdapter = GalleryAdapter(galleryLayout, activity)

    private var attachedItem: Any? = null

    var expanded = false
        set(value) {
            if (value != field) {
                field = value
                contentText.visibility = if (field) View.VISIBLE else View.GONE
                divider.visibility = if (field) View.VISIBLE else View.GONE
                openButton.visibility = if (field) View.VISIBLE else View.GONE
                galleryLayout.visibility = if (field) View.VISIBLE else View.GONE

                deleteChip.visibility = if (field) View.VISIBLE else View.GONE
                clipChip.visibility = if (field && attachedItem is Stop) View.VISIBLE else View.GONE
                launchChip.visibility = if (field && (attachedItem is Stop || attachedItem is DestinationWithStops)) View.VISIBLE else View.GONE
                travelChip.visibility = if (field && attachedItem is DestinationWithStops) View.VISIBLE else View.GONE
            }
        }

    init {
        rootView.setOnClickListener(this)
        rootView.setOnLongClickListener(this)
        openButton.setOnClickListener(this)
        clipChip.setOnClickListener(this)
        launchChip.setOnClickListener(this)
        deleteChip.setOnClickListener(this)
        travelChip.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        when (view) {
            rootView -> {
                expanded = !expanded
                if (expanded) {
                    onCardEventListener?.onCardExpanded()
                } else {
                    onCardEventListener?.onCardShrunk()
                }
            }
            openButton -> onCardEventListener?.onCardOpenClicked()
            clipChip -> onCardEventListener?.onCardClipClicked()
            launchChip -> onCardEventListener?.onCardLaunchClicked()
            deleteChip -> onCardEventListener?.onCardDeleteClicked()
            travelChip -> onCardEventListener?.onCardTravelClicked()
        }
    }

    override fun onLongClick(view: View): Boolean {
        when (view) {
            rootView -> {
                onCardEventListener?.onCardLongClicked(view)
            }
        }
        return true
    }

    fun attach(item: Any) {
        if (item is Stop) {
            attach(item as Stop)
        }
        if (item is DestinationWithStops) {
            attach(item as DestinationWithStops)
        }
        if (item is TripWithDestAndStop) {
            attach(item as TripWithDestAndStop)
        }
    }

    fun reset() {
        titleText.text = context.getString(R.string.card_placeholder_title)
        subtitleText.text = context.getString(R.string.card_placeholder_subtitle)
        contentText.text = context.getString(R.string.card_placeholder_content)

        expanded = false
        attachedItem = null
    }

    fun attach(tripWithDestAndStop: TripWithDestAndStop) {
        attachedItem = tripWithDestAndStop
        titleText.text = tripWithDestAndStop.trip.tripName
        subtitleText.text = String.format(context.getString(R.string.text_date_range), DateUtil.longToDate(tripWithDestAndStop.trip.tripStartDay), DateUtil.longToDate(tripWithDestAndStop.trip.tripEndDay))
        contentText.text = tripWithDestAndStop.trip.tripNotes
        openButton.text = context.getString(R.string.button_trip_open)

        galleryAdapter.attach(tripWithDestAndStop)
    }

    fun attach(destWithStops: DestinationWithStops) {
        attachedItem = destWithStops
        titleText.text = destWithStops.destination.destName
        subtitleText.text = String.format(context.getString(R.string.text_date_range), DateUtil.longToDate(destWithStops.destination.destEndDay), DateUtil.longToDate(destWithStops.destination.destEndDay))
        contentText.text = destWithStops.destination.destNotes
        openButton.text = context.getString(R.string.button_dest_open)

        galleryAdapter.attach(destWithStops)
    }

    fun attach(stop: Stop) {
        attachedItem = stop
        titleText.text = stop.stopName
        subtitleText.text = SerializableLocation.decode(stop.stopLocation)?.formattedAddress ?: "No Address"
        contentText.text = stop.stopNotes
        openButton.text = context.getString(R.string.button_stop_open)

        galleryAdapter.attach(stop)
    }

    interface OnCardEventListener {
        fun onCardExpanded() {}

        fun onCardShrunk() {}

        fun onCardOpenClicked() {}

        fun onCardClipClicked() {}

        fun onCardLaunchClicked() {}

        fun onCardDeleteClicked() {}

        fun onCardTravelClicked() {}

        fun onCardLongClicked(view: View) {}
    }
}
