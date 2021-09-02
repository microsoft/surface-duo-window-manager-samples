package com.example.travelplanner

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
data class Travel(

    @Transient
    @PrimaryKey(autoGenerate = true)
    var travelId: Long = 0,
    @Transient
    var parentDestId: Long = 0,

    var travelMode: String,
    var travelNotes: String,
    var travelStartDay: Long,
    var travelEndDay: Long
)
