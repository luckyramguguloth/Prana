package dev.paarudev.prana.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Encrypted Room Database Entity for Recorded Check-ins.
 * Stores optical rPPG, voice acoustic markers, fused score, and generated insight.
 */
@Entity(tableName = "wellness_entries")
data class WellnessEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val heartRateBpm: Double,
    val hrvRmssdMs: Double,
    val voiceStressScore: Double,
    val wellnessScore: Int,
    val wellnessState: String,
    val insightText: String,
    val isEncrypted: Boolean = true
)
