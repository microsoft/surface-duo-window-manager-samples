package com.example.travelplanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.travelplanner.models.SerializableLocation
import kotlinx.coroutines.launch

class TravelPlannerViewModel(application: Application) : AndroidViewModel(application) {

    private var appRepository = AppRepository(application)

    var lastInsertedTripId = MutableLiveData<Long>()
    var lastInsertedDestId = MutableLiveData<Long>()
    var lastInsertedStopId = MutableLiveData<Long>()

    var selectedTripId: Long? = null
        private set

    var selectedDestId: Long? = null
        private set

    var selectedStopId: Long? = null
        private set

    private val mutableSelectedDayIndex = MutableLiveData<Int>()
    val selectedDayIndex: LiveData<Int> get() = mutableSelectedDayIndex
    companion object {
        // selectedIndex = -1 if all stops tab is selected
        val SELECTED_ALL_STOPS = -1
        // selectedIndex = -2 if any day tab is selected
        val SELECTED_ANY_DAY = -2
    }

    var editing: Boolean = false
    var preloadLocation: SerializableLocation? = null

    fun getTripsWithDestAndStop(): LiveData<List<TripWithDestAndStop>> {
        return appRepository.getTripsWithDestAndStop()
    }

    fun getTrip(tripId: Long): LiveData<TripWithDestAndStop> {
        return appRepository.getTrip(tripId)
    }
    fun setSelectedTrip(tripDestStop: TripWithDestAndStop) {
        // mutableSelectedTrip.value = tripDestStop
        selectedTripId = tripDestStop.trip.tripId
    }

    fun getDestination(destId: Long): LiveData<DestinationWithStops> {
        return appRepository.getDestination(destId)
    }
    fun setSelectedDest(destWithStops: DestinationWithStops) {
        selectedDestId = destWithStops.destination.destId
    }

    fun getStops(destId: Long): LiveData<List<Stop>> {
        return appRepository.getStops(destId)
    }

    fun getDayStops(destId: Long, day: Long): List<Stop> {
        return appRepository.getDayStops(destId, day)
    }
    fun setSelectedDayIndex(dayIndex: Int) {
        mutableSelectedDayIndex.value = dayIndex
    }

    fun getAnyDayStops(destId: Long): List<Stop> {
        return appRepository.getAnyDayStops(destId)
    }

    fun getStop(stopId: Long): LiveData<Stop> {
        return appRepository.getStop(stopId)
    }
    fun setSelectedStop(stop: Stop) {
        selectedStopId = stop.stopId
    }

    fun getTravel(destId: Long): LiveData<Travel> {
        return appRepository.getTravel(destId)
    }

    fun tripDateChanged(tripWithDestAndStop: TripWithDestAndStop) {
        viewModelScope.launch {
            appRepository.tripDateChanged(tripWithDestAndStop)
        }
    }

    fun destDateChanged(destinationWithStops: DestinationWithStops) {
        viewModelScope.launch {
            appRepository.destDateChanged(destinationWithStops)
        }
    }

    fun importTrip(tripWithDestAndStop: TripWithDestAndStop) {
        viewModelScope.launch {
            appRepository.importTrip(tripWithDestAndStop)
        }
    }

    fun insertTrip(trip: Trip) {
        viewModelScope.launch {
            var id = appRepository.insertTrip(trip)
            lastInsertedTripId.postValue(id)
        }
    }

    fun insertDestination(destination: Destination) {
        viewModelScope.launch {
            var id = appRepository.insertDestination(destination)
            lastInsertedDestId.postValue(id)
        }
    }

    fun insertStop(stop: Stop) {
        viewModelScope.launch {
            var id = appRepository.insertStop(stop)
            lastInsertedStopId.postValue(id)
        }
    }

    fun insertTravel(travel: Travel) {
        viewModelScope.launch {
            appRepository.insertTravel(travel)
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            appRepository.deleteTrip(trip)
        }
    }

    fun deleteDestination(destination: Destination) {
        viewModelScope.launch {
            appRepository.deleteDestination(destination)
        }
    }

    fun deleteStop(stop: Stop) {
        viewModelScope.launch {
            appRepository.deleteStop(stop)
        }
    }

    fun deleteTravel(travel: Travel) {
        viewModelScope.launch {
            appRepository.deleteTravel(travel)
        }
    }
}
