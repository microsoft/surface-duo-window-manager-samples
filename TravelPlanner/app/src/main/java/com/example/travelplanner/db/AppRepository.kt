package com.example.travelplanner

import android.app.Application
import androidx.lifecycle.LiveData

class AppRepository(application: Application) {

    private val appDao: AppDao
    private val tripsWithDestAndStop: LiveData<List<TripWithDestAndStop>>

    init {
        val database = AppDatabase.getInstance(application.applicationContext)
        appDao = database!!.getAppDao()
        tripsWithDestAndStop = appDao.getTripsWithDestsAndStops()
    }

    fun getTripsWithDestAndStop(): LiveData<List<TripWithDestAndStop>> {
        return tripsWithDestAndStop
    }

    fun getTrip(tripId: Long): LiveData<TripWithDestAndStop> {
        return appDao.getTrip(tripId)
    }

    fun getDestination(destId: Long): LiveData<DestinationWithStops> {
        return appDao.getDestination(destId)
    }

    fun getStops(destId: Long): LiveData<List<Stop>> {
        return appDao.getStops(destId)
    }

    fun getDayStops(destId: Long, day: Long): List<Stop> {
        return appDao.getDayStops(destId, day)
    }

    fun getAnyDayStops(destId: Long): List<Stop> {
        return appDao.getAnyDayStops(destId)
    }

    fun getStop(stopId: Long): LiveData<Stop> {
        return appDao.getStop(stopId)
    }

    fun getTravel(destId: Long): LiveData<Travel> {
        return appDao.getTravel(destId)
    }

    suspend fun tripDateChanged(tripWithDestAndStop: TripWithDestAndStop) {
        appDao.tripDateChanged(tripWithDestAndStop)
    }

    suspend fun destDateChanged(destinationWithStops: DestinationWithStops) {
        appDao.destDateChanged(destinationWithStops)
    }

    suspend fun importTrip(tripWithDestAndStop: TripWithDestAndStop) {
        appDao.importTrip(tripWithDestAndStop)
    }

    suspend fun insertTrip(trip: Trip): Long {
        return appDao.insertTrip(trip)
    }

    suspend fun insertDestination(destination: Destination): Long {
        return appDao.insertDestination(destination)
    }

    suspend fun insertStop(stop: Stop): Long {
        return appDao.insertStop(stop)
    }

    suspend fun insertTravel(travel: Travel): Long {
        return appDao.insertTravel(travel)
    }

    suspend fun deleteTrip(trip: Trip) {
        appDao.deleteTrip(trip)
    }

    suspend fun deleteDestination(destination: Destination) {
        appDao.deleteDestination(destination)
    }

    suspend fun deleteStop(stop: Stop) {
        appDao.deleteStop(stop)
    }

    suspend fun deleteTravel(travel: Travel) {
        appDao.deleteTravel(travel)
    }
}
