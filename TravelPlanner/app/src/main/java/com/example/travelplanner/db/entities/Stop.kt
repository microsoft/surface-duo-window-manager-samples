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
            entity = Destination::class,
            parentColumns = ["destId"],
            childColumns = ["parentDestId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("parentDestId")
    ]
)
data class Stop(

    @Transient
    @PrimaryKey
    var stopId: Long = UUID.randomUUID().leastSignificantBits,
    @Transient
    var parentDestId: Long = 0,

    var stopName: String,
    var stopNotes: String,

    var stopLocation: String,

    var stopDay: Long,

    var stopAnyDay: Boolean = false
)
