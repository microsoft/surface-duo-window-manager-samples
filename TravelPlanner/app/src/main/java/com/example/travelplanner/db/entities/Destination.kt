package com.example.travelplanner

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["tripId"],
            childColumns = ["parentTripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parentTripId")
    ]
)
data class Destination(

    @Transient
    @PrimaryKey
    var destId: Long = UUID.randomUUID().leastSignificantBits,
    @Transient
    var parentTripId: Long = 0,

    var destName: String,
    var destNotes: String,

    var destStartDay: Long,
    var destEndDay: Long,

    var destLocation: String,
    var accomLocation: String
)
