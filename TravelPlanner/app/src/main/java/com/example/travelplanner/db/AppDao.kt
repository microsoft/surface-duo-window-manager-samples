package com.example.travelplanner

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface AppDao {

    @Transaction
    @Query("SELECT * FROM Trip ORDER BY tripStartDay ASC")
    fun getTripsWithDestsAndStops(): LiveData<List<TripWithDestAndStop>>

    @Transaction
    @Query("SELECT * FROM Trip WHERE tripId=:tripId")
    fun getTrip(tripId: Long): LiveData<TripWithDestAndStop>

    @Transaction
    @Query("SELECT * FROM Destination WHERE destId=:destId")
    fun getDestination(destId: Long): LiveData<DestinationWithStops>

    @Transaction
    @Query("SELECT * FROM Stop WHERE parentDestId=:destId")
    fun getStops(destId: Long): LiveData<List<Stop>>

    @Transaction
    @Query("SELECT * FROM Stop WHERE parentDestId=:destId AND stopDay=:day AND stopAnyDay=0")
    fun getDayStops(destId: Long, day: Long): List<Stop>

    @Transaction
    @Query("SELECT * FROM Stop WHERE parentDestId=:destId AND stopAnyDay=1")
    fun getAnyDayStops(destId: Long): List<Stop>

    @Transaction
    @Query("SELECT * FROM Stop WHERE stopId=:stopId")
    fun getStop(stopId: Long): LiveData<Stop>

    @Transaction
    @Query("SELECT * FROM Travel WHERE parentDestId=:destId")
    fun getTravel(destId: Long): LiveData<Travel>

    @Transaction
    suspend fun tripDateChanged(tripWithDestAndStop: TripWithDestAndStop) {
        insertTrip(tripWithDestAndStop.trip)
        val tripStart = tripWithDestAndStop.trip.tripStartDay
        val tripEnd = tripWithDestAndStop.trip.tripEndDay
        for (destWithStops in tripWithDestAndStop.destinations) {
            if (destWithStops.destination.destStartDay < tripStart) {
                destWithStops.destination.destStartDay = tripStart
            }

            if (destWithStops.destination.destEndDay > tripEnd) {
                destWithStops.destination.destEndDay = tripEnd
            }

            insertDestination(destWithStops.destination)

            destWithStops.travel?.let {
                if (it.travelStartDay < destWithStops.destination.destStartDay) {
                    it.travelStartDay = destWithStops.destination.destStartDay
                }

                if (it.travelEndDay > destWithStops.destination.destEndDay) {
                    it.travelEndDay = destWithStops.destination.destEndDay
                }

                insertTravel(it)
            }

            for (stop in destWithStops.stops) {

                if (stop.stopDay < destWithStops.destination.destStartDay) {
                    stop.stopAnyDay = true
                }

                if (stop.stopDay > destWithStops.destination.destEndDay) {
                    stop.stopAnyDay = true
                }

                insertStop(stop)
            }
        }
    }

    @Transaction
    suspend fun destDateChanged(destWithStops: DestinationWithStops) {
        insertDestination(destWithStops.destination)

        destWithStops.travel?.let {
            if (it.travelStartDay < destWithStops.destination.destStartDay) {
                it.travelStartDay = destWithStops.destination.destStartDay
            }

            if (it.travelEndDay > destWithStops.destination.destEndDay) {
                it.travelEndDay = destWithStops.destination.destEndDay
            }
            insertTravel(it)
        }

        for (stop in destWithStops.stops) {

            if (stop.stopDay < destWithStops.destination.destStartDay) {
                stop.stopAnyDay = true
            }

            if (stop.stopDay > destWithStops.destination.destEndDay) {
                stop.stopAnyDay = true
            }

            insertStop(stop)
        }
    }

    @Transaction
    suspend fun importTrip(tripWithDestAndStop: TripWithDestAndStop) {
        var tripId = insertTrip(tripWithDestAndStop.trip)
        for (destWithStops in tripWithDestAndStop.destinations) {
            destWithStops.destination.parentTripId = tripId
            var destId = insertDestination(destWithStops.destination)
            if (destWithStops.travel != null) {
                destWithStops.travel.parentDestId = destId
                insertTravel(destWithStops.travel)
            }
            for (stop in destWithStops.stops) {
                stop.parentDestId = destId
                insertStop(stop)
            }
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDestination(dest: Destination): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStop(stop: Stop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTravel(travel: Travel): Long

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Delete
    suspend fun deleteDestination(dest: Destination)

    @Delete
    suspend fun deleteStop(stop: Stop)

    @Delete
    suspend fun deleteTravel(travel: Travel)
}
