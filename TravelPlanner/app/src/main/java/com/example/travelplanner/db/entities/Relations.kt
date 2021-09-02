package com.example.travelplanner

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class DestinationWithStops(
    @Embedded val destination: Destination,

    @Relation(
        parentColumn = "destId",
        entityColumn = "parentDestId",
    )
    val stops: List<Stop>,

    // travel time to this destination
    @Relation(
        parentColumn = "destId",
        entityColumn = "parentDestId",
    )
    val travel: Travel?

)

@Serializable
data class TripWithDestAndStop(
    @Embedded val trip: Trip,

    @Relation(
        entity = Destination::class,
        parentColumn = "tripId",
        entityColumn = "parentTripId"
    )
    val destinations: List<DestinationWithStops>
)
