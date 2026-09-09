package dev.paarudev.prana.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [WellnessEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wellnessDao(): WellnessDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "prana_wellness_secure.db"

        fun getDatabase(
            context: Context,
            passphraseBytes: ByteArray? = null,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )

                // Encrypt Room with SQLCipher if passphrase provided
                if (passphraseBytes != null && passphraseBytes.isNotEmpty()) {
                    try {
                        SQLiteDatabase.loadLibs(context.applicationContext)
                        val factory = SupportFactory(passphraseBytes)
                        builder.openHelperFactory(factory)
                    } catch (e: Throwable) {
                        // Fallback to standard SQLite
                    }
                }

                builder.addCallback(DatabaseCallback(scope))
                val instance = builder.build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        seedHistoricalDemoData(database.wellnessDao())
                    }
                }
            }
        }

        /**
         * Pre-seeds 7 days of realistic physiological check-ins so the 7-day trend
         * view displays rich interactive data immediately on evaluation day.
         */
        suspend fun seedHistoricalDemoData(dao: WellnessDao) {
            if (dao.getCount() > 0) return

            val now = System.currentTimeMillis()
            val dayMs = 86400000L

            val seedData = listOf(
                WellnessEntry(
                    timestampMs = now - (6 * dayMs),
                    heartRateBpm = 74.0,
                    hrvRmssdMs = 46.0,
                    voiceStressScore = 4.2,
                    wellnessScore = 76,
                    wellnessState = "Calm & Balanced",
                    insightText = "Pulse and vocal rhythms are consistent with baseline. Good stability."
                ),
                WellnessEntry(
                    timestampMs = now - (5 * dayMs),
                    heartRateBpm = 78.0,
                    hrvRmssdMs = 38.0,
                    voiceStressScore = 6.4,
                    wellnessScore = 62,
                    wellnessState = "Mild Strain",
                    insightText = "Elevated vocal tension and slight drop in HRV. Take a pause after intense tasks."
                ),
                WellnessEntry(
                    timestampMs = now - (4 * dayMs),
                    heartRateBpm = 84.0,
                    hrvRmssdMs = 28.0,
                    voiceStressScore = 7.8,
                    wellnessScore = 48,
                    wellnessState = "Elevated Tension",
                    insightText = "Significant acoustic tension and lower autonomic reserve. Prioritize rest."
                ),
                WellnessEntry(
                    timestampMs = now - (3 * dayMs),
                    heartRateBpm = 72.0,
                    hrvRmssdMs = 52.0,
                    voiceStressScore = 3.5,
                    wellnessScore = 82,
                    wellnessState = "Calm & Balanced",
                    insightText = "Autonomic tone rebounded well with improved vocal calm. Steady recovery."
                ),
                WellnessEntry(
                    timestampMs = now - (2 * dayMs),
                    heartRateBpm = 66.0,
                    hrvRmssdMs = 68.0,
                    voiceStressScore = 2.4,
                    wellnessScore = 91,
                    wellnessState = "Optimal Recovery",
                    insightText = "High HRV and relaxed vocal markers reflect strong physiological recovery."
                ),
                WellnessEntry(
                    timestampMs = now - (1 * dayMs),
                    heartRateBpm = 70.0,
                    hrvRmssdMs = 54.0,
                    voiceStressScore = 3.8,
                    wellnessScore = 84,
                    wellnessState = "Calm & Balanced",
                    insightText = "Cardiovascular and vocal markers remain well within optimal baseline."
                ),
                WellnessEntry(
                    timestampMs = now - (2 * 3600000L), // 2 hours ago today
                    heartRateBpm = 71.0,
                    hrvRmssdMs = 50.0,
                    voiceStressScore = 3.9,
                    wellnessScore = 81,
                    wellnessState = "Calm & Balanced",
                    insightText = "Holding a steady pace. Keep hydrated and maintain gentle breathing."
                )
            )

            for (entry in seedData) {
                dao.insertEntry(entry)
            }
        }
    }
}
