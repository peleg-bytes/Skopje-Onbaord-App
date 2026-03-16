package com.skopje.onboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surveys")
data class Survey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val surveyorId: String,
    val stationName: String,
    val startTime: String,
    var submitTime: String,
    var latitude: Double?,
    var longitude: Double?,
    var passengerCount: Int,
    var uploadedStatus: Boolean = false,
    var isSubmitted: Boolean = false,
)
