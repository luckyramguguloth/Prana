package dev.paarudev.prana.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WellnessDao {

    @Query("SELECT * FROM wellness_entries ORDER BY timestampMs DESC")
    fun getAllEntries(): Flow<List<WellnessEntry>>

    @Query("SELECT * FROM wellness_entries ORDER BY timestampMs DESC LIMIT :limit")
    suspend fun getRecentEntries(limit: Int = 7): List<WellnessEntry>

    @Query("SELECT * FROM wellness_entries WHERE timestampMs >= :sinceMs ORDER BY timestampMs ASC")
    suspend fun getEntriesSince(sinceMs: Long): List<WellnessEntry>

    @Query("SELECT AVG(heartRateBpm) FROM (SELECT heartRateBpm FROM wellness_entries ORDER BY timestampMs DESC LIMIT 7)")
    suspend fun getAverageHeartRate(): Double?

    @Query("SELECT AVG(hrvRmssdMs) FROM (SELECT hrvRmssdMs FROM wellness_entries ORDER BY timestampMs DESC LIMIT 7)")
    suspend fun getAverageHrv(): Double?

    @Query("SELECT AVG(voiceStressScore) FROM (SELECT voiceStressScore FROM wellness_entries ORDER BY timestampMs DESC LIMIT 7)")
    suspend fun getAverageVoiceStress(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: WellnessEntry): Long

    @Query("DELETE FROM wellness_entries")
    suspend fun deleteAllEntries()

    @Query("SELECT COUNT(*) FROM wellness_entries")
    suspend fun getCount(): Int
}
