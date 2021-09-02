package com.example.travelplanner.models

import com.microsoft.maps.Geopoint
import com.microsoft.maps.search.MapLocation
import com.microsoft.maps.search.MapLocationAddress
import com.microsoft.maps.search.MapLocationFinder
import com.microsoft.maps.search.MapLocationMatchCode
import com.microsoft.maps.search.MapLocationOptions
import com.microsoft.maps.search.OnMapLocationFinderResultListener
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class SerializableLocation(val lat: Double, val long: Double, val alt: Double, val formattedAddress: String) {
    constructor(mapLocation: MapLocation) : this(
        mapLocation.point.position.latitude,
        mapLocation.point.position.longitude,
        mapLocation.point.position.altitude,
        mapLocation.address.formattedAddress
    )

    fun getGeopoint() = Geopoint(lat, long, alt)

    companion object {
        fun encode(serializableLocation: SerializableLocation?): String {
            return if (serializableLocation != null) Json.encodeToString(serializableLocation) else ""
        }

        fun decode(string: String): SerializableLocation? {
            return if (string != "") Json.decodeFromString(string) else null
        }
    }
}

fun narrowReverseGeocode(listener: OnReverseGeocodeResultListener, geopoint: Geopoint) {
    val firstListener = OnMapLocationFinderResultListener {
        if (it.locations.isNotEmpty()) {
            if (!it.locations[0].matchCodes.contains(MapLocationMatchCode.UP_HIERARCHY)) {
                listener.onReverseGeocodeResult(it.locations[0])
            } else {
                listener.onReverseGeocodeResult(null)
            }
        } else {
            listener.onReverseGeocodeResult(null)
        }
    }

    val mapLocationOptions = MapLocationOptions()
    mapLocationOptions.setMaxResults(20)
    MapLocationFinder.findLocationsAt(geopoint, mapLocationOptions, firstListener)
}

fun wideReverseGeocode(listener: OnReverseGeocodeResultListener, geopoint: Geopoint) {
    val firstListener = OnMapLocationFinderResultListener {
        if (it.locations.isNotEmpty()) {
            wideReverseGeocode2(listener, it.locations[0].address, geopoint)
        }
    }

    val mapLocationOptions = MapLocationOptions()
    mapLocationOptions.setMaxResults(1)
    MapLocationFinder.findLocationsAt(geopoint, mapLocationOptions, firstListener)
}

fun wideReverseGeocode2(listener: OnReverseGeocodeResultListener, address: MapLocationAddress, geopoint: Geopoint) {
    val secondListener = OnMapLocationFinderResultListener {
        if (it.locations.isNotEmpty()) {
            listener.onReverseGeocodeResult(it.locations[0])
        }
    }

    val mapLocationOptions = MapLocationOptions()
    mapLocationOptions.setMaxResults(1)
    mapLocationOptions.setIncludeEntityTypes(false, true, true, false, true, true, true)

    val queryString = when {
        address.locality.isNotBlank() -> address.locality
        address.adminDistrict2.isNotBlank() -> address.adminDistrict2
        address.adminDistrict.isNotBlank() -> address.adminDistrict
        else -> address.countryRegion
    }

    MapLocationFinder.findLocations(queryString, geopoint, mapLocationOptions, secondListener)
}

fun interface OnReverseGeocodeResultListener {
    fun onReverseGeocodeResult(mapLocation: MapLocation?)
}
