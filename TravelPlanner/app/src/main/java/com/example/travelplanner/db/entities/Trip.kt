package com.example.travelplanner

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
@Entity
data class Trip(

    @Transient
    @PrimaryKey
    var tripId: Long = UUID.randomUUID().leastSignificantBits,

    var tripName: String,

    var tripNotes: String,

    var tripStartDay: Long,

    var tripEndDay: Long
)
