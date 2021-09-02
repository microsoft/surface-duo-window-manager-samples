package com.example.travelplanner

import android.graphics.Bitmap
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.microsoft.maps.Geopoint
import com.microsoft.maps.MapAnimationKind
import com.microsoft.maps.MapElementLayer
import com.microsoft.maps.MapElementTappedEventArgs
import com.microsoft.maps.MapIcon
import com.microsoft.maps.MapImage
import com.microsoft.maps.MapRenderMode
import com.microsoft.maps.MapScene
import com.microsoft.maps.MapTappedEventArgs
import com.microsoft.maps.MapView
import com.microsoft.maps.OnMapElementTappedListener
import com.microsoft.maps.OnMapTappedListener

class MapFragment(var onMapEventListener: OnMapEventListener? = null) : Fragment(), OnMapTappedListener, OnMapElementTappedListener {
    lateinit var mapView: MapView
    private lateinit var mapIconNormalBitmap: Bitmap
    private lateinit var mapIconHighlightBitmap: Bitmap
    private lateinit var mapIconSearchBitmap: Bitmap
    private val pinMap = HashMap<String, MapIcon>()
    private val mapElementLayer = MapElementLayer()
    var radius = 10.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, activity?.resources?.displayMetrics).toInt()
        mapIconNormalBitmap = activity?.getDrawable(R.drawable.ic_map_icon_normal)?.toBitmap(px, px)!!
        mapIconHighlightBitmap = activity?.getDrawable(R.drawable.ic_map_icon_highlight)?.toBitmap(px, px)!!
        mapIconSearchBitmap = activity?.getDrawable(R.drawable.ic_map_icon_search)?.toBitmap(px, px)!!

        val rootView = inflater.inflate(R.layout.fragment_map, container, false)
        mapView = MapView(activity, MapRenderMode.VECTOR)
        mapView.setCredentialsKey(BuildConfig.BUILD_KEY)
        (rootView as FrameLayout).addView(mapView)
        mapView.onCreate(savedInstanceState)
        mapView.addOnMapTappedListener(this)

        mapView.layers.add(mapElementLayer)
        mapElementLayer.addOnMapElementTappedListener(this)

        return rootView
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {

        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    fun setPin(tag: String, geopoint: Geopoint?, color: String?) {
        var mapIcon = pinMap[tag]

        if (mapIcon == null) {
            mapIcon = MapIcon()
            mapIcon.image = MapImage(mapIconNormalBitmap)
            mapIcon.tag = tag

            mapElementLayer.elements.add(mapIcon)
            pinMap.put(tag, mapIcon)
        }

        if (geopoint != null) {
            mapIcon.location = geopoint
        }

        if (color != null) {
            mapIcon.image = when (color) {
                "HIGHLIGHT" -> MapImage(mapIconHighlightBitmap)
                "SEARCH" -> MapImage(mapIconSearchBitmap)
                else -> MapImage(mapIconNormalBitmap)
            }
        }
    }

    fun removePin(tag: String) {
        var mapIcon = pinMap.get(tag)
        if (mapIcon != null) {
            mapElementLayer.elements.remove(mapIcon)
            pinMap.remove(tag)
        }
    }

    fun clearPins() {
        mapElementLayer.elements.clear()
        pinMap.clear()
    }

    fun resetScene() {
        val locations = ArrayList<Geopoint>()
        pinMap.forEach { (_, pin) -> locations.add(pin.location) }
        if (locations.size > 1) {
            mapView.setScene(MapScene.createFromLocationsAndMargin(locations, 200.0), MapAnimationKind.BOW)
        } else if (locations.isNotEmpty()) {
            mapView.setScene(MapScene.createFromLocationAndRadius(locations[0], radius), MapAnimationKind.BOW)
        }
    }

    override fun onMapTapped(mapTappedEventArgs: MapTappedEventArgs): Boolean {
        onMapEventListener?.onLocationTapped(mapTappedEventArgs.location)

        return true
    }

    override fun onMapElementTapped(mapElementTappedEventArgs: MapElementTappedEventArgs): Boolean {
        for (mapElement in mapElementTappedEventArgs.mapElements) {
            if (pinMap.contains(mapElement.tag.toString())) {
                onMapEventListener?.onPinSelected(mapElement.tag.toString())
                return true
            }
        }

        return true
    }

    interface OnMapEventListener {
        fun onPinSelected(tag: String) {}
        fun onLocationTapped(geopoint: Geopoint) {}
    }
}
