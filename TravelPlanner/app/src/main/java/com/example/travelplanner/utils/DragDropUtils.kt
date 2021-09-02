package com.example.travelplanner.utils

import android.content.Context
import com.example.travelplanner.DateUtil
import com.example.travelplanner.DestinationWithStops
import com.example.travelplanner.R
import com.example.travelplanner.Stop
import com.example.travelplanner.TripWithDestAndStop
import com.example.travelplanner.models.SerializableLocation

class DragDropUtils(context: Context) {

    val context = context

    fun tripToHtml(tripWithDestAndStop: TripWithDestAndStop): String {

        var sb: StringBuilder = StringBuilder()
        sb.append("<h1>").append(tripWithDestAndStop.trip.tripName).append("</h3>")
        sb.append("<p>").append(DateUtil.longToDate(tripWithDestAndStop.trip.tripStartDay))
            .append(" - ").append(DateUtil.longToDate(tripWithDestAndStop.trip.tripEndDay))
            .append("<p>")
        sb.append("<p>").append(context.getString(R.string.dd_notes)).append(tripWithDestAndStop.trip.tripNotes).append("</p>")

        for (destWithStops in tripWithDestAndStop.destinations) {

            sb.append(destToHtml(destWithStops))
        }

        return sb.toString()
    }

    fun destToHtml(destWithStops: DestinationWithStops): String {

        val sb = StringBuilder()

        sb.append("<h2>")
            .append(destWithStops.destination.destName)
            .append("</h2>")

        sb.append("<p>")
            .append(DateUtil.longToDateShortened(destWithStops.destination.destStartDay))
            .append(" - ")
            .append(DateUtil.longToDateShortened(destWithStops.destination.destEndDay))
            .append("</p>")

        var location = SerializableLocation.decode(destWithStops.destination.destLocation)
        var accom = SerializableLocation.decode(destWithStops.destination.accomLocation)
        sb.append("<p>")
            .append(context.getString(R.string.dd_location))
            .append(location?.formattedAddress)
            .append("</p>")

        sb.append("<p>")
            .append(context.getString(R.string.dd_accommodation))
            .append(accom?.formattedAddress)
            .append("</p>")

        sb.append("<p>")
            .append(context.getString(R.string.dd_notes))
            .append(destWithStops.destination.destNotes)
            .append("</p>")

        if (destWithStops.travel != null) {
            // todo: add travel info
        }

        for (stop in destWithStops.stops) {
            sb.append("<blockquote>")
            sb.append(stopToHtml(stop))
            sb.append("</blockquote>")
        }

        return sb.toString()
    }

    fun stopToHtml(stop: Stop): String {
        val sb = StringBuilder()
        sb.append("<h3>").append(stop.stopName).append("</h3>")
        if (stop.stopAnyDay) {
            sb.append("<p>")
                .append(context.getString(R.string.dd_day))
                .append(context.getString(R.string.text_any_day))
                .append("</p>")
        } else {
            sb.append("<p>")
                .append(context.getString(R.string.dd_day))
                .append(DateUtil.longToDateShortened(stop.stopDay))
                .append("</p>")
        }
        sb.append("<p>")
            .append(context.getString(R.string.dd_location))
            .append(SerializableLocation.decode(stop.stopLocation)?.formattedAddress)
            .append("</p>")

        sb.append("<p>")
            .append(context.getString(R.string.dd_notes))
            .append(stop.stopNotes)
            .append("</p>")

        return sb.toString()
    }

    fun tripToText(tripWithDestAndStop: TripWithDestAndStop): String {
        var sb: StringBuilder = StringBuilder()
        sb.append(tripWithDestAndStop.trip.tripName).append("\r\n")
        sb.append(DateUtil.longToDate(tripWithDestAndStop.trip.tripStartDay))
            .append(" - ").append(DateUtil.longToDate(tripWithDestAndStop.trip.tripEndDay))
            .append("\r\n")
        sb.append(context.getString(R.string.dd_notes)).append(tripWithDestAndStop.trip.tripNotes).append("\r\n").append("\r\n")

        for (destWithStops in tripWithDestAndStop.destinations) {

            sb.append(destToText(destWithStops))
        }

        return sb.toString()
    }

    fun destToText(destWithStops: DestinationWithStops): String {
        val sb = StringBuilder()

        sb.append(destWithStops.destination.destName)
            .append("\r\n")

        sb.append(DateUtil.longToDateShortened(destWithStops.destination.destStartDay))
            .append(" - ")
            .append(DateUtil.longToDateShortened(destWithStops.destination.destEndDay))
            .append("\r\n")

        var location = SerializableLocation.decode(destWithStops.destination.destLocation)
        var accom = SerializableLocation.decode(destWithStops.destination.accomLocation)
        sb.append(context.getString(R.string.dd_location))
            .append(location?.formattedAddress)
            .append("\r\n")

        sb.append(context.getString(R.string.dd_accommodation))
            .append(accom?.formattedAddress)
            .append("\r\n")

        sb.append(context.getString(R.string.dd_notes))
            .append(destWithStops.destination.destNotes)
            .append("\r\n")

        if (destWithStops.travel != null) {
            // todo: add travel info
        }

        for (stop in destWithStops.stops) {
            sb.append("\t")
            sb.append(stopToText(stop))
        }

        return sb.toString()
    }

    fun stopToText(stop: Stop): String {
        val sb = StringBuilder()
        sb.append(stop.stopName).append("\r\n")
        if (stop.stopAnyDay) {
            sb.append(context.getString(R.string.dd_day))
                .append(context.getString(R.string.text_any_day))
                .append("\r\n")
        } else {
            sb.append(context.getString(R.string.dd_day))
                .append(DateUtil.longToDateShortened(stop.stopDay))
                .append("\r\n")
        }
        sb.append(context.getString(R.string.dd_location))
            .append(SerializableLocation.decode(stop.stopLocation)?.formattedAddress)
            .append("\r\n")

        sb.append(context.getString(R.string.dd_notes))
            .append(stop.stopNotes)
            .append("\r\n")

        return sb.toString()
    }
}
