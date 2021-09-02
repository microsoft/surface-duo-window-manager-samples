package com.example.travelplanner.utils

import android.content.Context
import android.widget.Toast
import com.example.travelplanner.R
import com.example.travelplanner.TravelPlannerViewModel
import com.example.travelplanner.TripWithDestAndStop
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.io.IOException

class ImportExportTripUtil(context: Context, tpViewModel: TravelPlannerViewModel) {

    private var context: Context = context
    private var tpViewModel: TravelPlannerViewModel = tpViewModel

    fun importTrip(jsonPath: String) {
        var jsonString: String
        try {
            jsonString = context.assets.open(jsonPath).bufferedReader().use {
                it.readText()
            }
        } catch (ex: IOException) {
            Toast.makeText(context, context.getString(R.string.toast_import_trip_error), Toast.LENGTH_SHORT).show()
            return
        }

        var tripWithDestAndStop = Json.decodeFromString<TripWithDestAndStop>(jsonString)

        tpViewModel.importTrip(tripWithDestAndStop)

        Toast.makeText(context, String.format(context.getString(R.string.toast_import_trip), jsonPath), Toast.LENGTH_SHORT).show()
    }

    fun exportTrip(tripWithDestAndStop: TripWithDestAndStop, filename: String) {
        try {
            var jsonString = Json.encodeToString(tripWithDestAndStop)
            val root: File? = context.getExternalFilesDir(null)
            val jsonFile = File(root, filename)
            val writer = FileWriter(jsonFile)
            writer.write(jsonString)
            writer.close()
            Toast.makeText(context, String.format(context.getString(R.string.toast_export_trip), filename), Toast.LENGTH_SHORT).show()
        } catch (ex: IOException) {
            Toast.makeText(context, context.getString(R.string.toast_export_trip_error), Toast.LENGTH_SHORT).show()
        }
    }
}
