package com.example.travelplanner

import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.DateFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.lang.Exception
import java.util.Date

class GalleryAdapter(val rootView: View, val activity: AppCompatActivity) : View.OnClickListener {
    private val linearLayout = rootView.findViewById<LinearLayout>(R.id.gallery_layout)

    private var attachedItem: Any? = null
    private var attachedIDString: String? = null

    init {
        rootView.findViewById<Button>(R.id.gallery_button_add).setOnClickListener(this)
        rootView.findViewById<Button>(R.id.gallery_button_open).setOnClickListener(this)
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
        linearLayout.removeAllViews()
    }

    fun attach(tripWithDestAndStop: TripWithDestAndStop) {
        reset()
        attachedItem = tripWithDestAndStop
        attachedIDString = "t_" + tripWithDestAndStop.trip.tripId
        getImages(attachedIDString)
    }

    fun attach(destWithStops: DestinationWithStops) {
        reset()
        attachedItem = destWithStops
        attachedIDString = "d_" + destWithStops.destination.destId
        getImages(attachedIDString)
    }

    fun attach(stop: Stop) {
        reset()
        attachedItem = stop
        attachedIDString = "s_" + stop.stopId
        getImages(attachedIDString)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.gallery_button_add -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri = newImage(attachedIDString)
                    if (uri != null) {
                        (activity as TravelPlannerInterface).tpImportImageCallback = null
                        (activity as TravelPlannerInterface).tpTakeImageCallback = TakeImageCallback()
                        (activity as TravelPlannerInterface).tpTakeImageLauncher?.launch(uri)
                    }
                }
            }
            R.id.gallery_button_open -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val uri = newImage(attachedIDString)
                    if (uri != null) {
                        (activity as TravelPlannerInterface).tpTakeImageCallback = null
                        (activity as TravelPlannerInterface).tpImportImageCallback = ImportImageCallback(uri)
                        (activity as TravelPlannerInterface).tpImportImageLauncher?.launch("image/*")
                    }
                }
            }
        }
    }

    inner class TakeImageCallback : ActivityResultCallback<Boolean> {
        override fun onActivityResult(result: Boolean?) {
            attachedItem?.let { attach(it) }
        }
    }

    inner class ImportImageCallback(private val targetUri: Uri) : ActivityResultCallback<Uri> {
        override fun onActivityResult(result: Uri?) {
            if (result != null) {
                try {
                    val inputStream = activity.contentResolver.openInputStream(result)
                    val outputStream = activity.contentResolver.openOutputStream(targetUri)

                    if (inputStream == null || outputStream == null) return

                    inputStream.copyTo(outputStream)
                } catch (e: Exception) {
                    return
                }
            }
            attachedItem?.let { attach(it) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun newImage(idString: String?): Uri? {
        if (idString == null) return null

        if (!activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) return null

        val timeStamp = DateFormat.getDateTimeInstance().format(Date())
        val dirString = StringBuffer()
        dirString.append(File.separator + "TravelPlanner")
        dirString.append(File.separator + idString)
        dirString.append(File.separator + "Travel_Planner_Images")

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "$timeStamp.jpg")
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + dirString.toString())
        return activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    }

    fun getImages(idString: String?) {
        if (idString == null) return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return

        val dirString = StringBuffer()
        dirString.append(File.separator + "TravelPlanner")
        dirString.append(File.separator + idString)
        dirString.append(File.separator + "Travel_Planner_Images")

        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.RELATIVE_PATH)
        val selection = MediaStore.Images.Media.RELATIVE_PATH + "=?"
        val selectionArgs = arrayOf(Environment.DIRECTORY_PICTURES + dirString.toString() + File.separator)
        val cursor = activity.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)

        if (cursor != null) {
            val columnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(columnIndex)
                )
                addImage(uri)
            }
        }
    }

    private fun addImage(uri: Uri) {
        val image: ImageView = LayoutInflater.from(activity).inflate(R.layout.layout_image, linearLayout, false) as ImageView

        try {
            val thumbnail = activity.contentResolver.loadThumbnail(uri, Size(128, 128), null)
            image.setImageBitmap(thumbnail)
        } catch (e: Exception) {
            return
        }

        val onClickListener = View.OnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.flags = Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT + Intent.FLAG_ACTIVITY_NEW_TASK
            activity.startActivity(intent)
        }

        image.setOnClickListener(onClickListener)
        linearLayout.addView(image)
    }
}
