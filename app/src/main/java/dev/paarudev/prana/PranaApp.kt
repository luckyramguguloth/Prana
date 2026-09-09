package dev.paarudev.prana

import android.app.Application
import dev.paarudev.prana.data.crypto.KeystoreManager
import dev.paarudev.prana.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PranaApp : Application() {

    lateinit var keystoreManager: KeystoreManager
        private set

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        keystoreManager = KeystoreManager(this)
        val passphrase = keystoreManager.getOrCreateDatabasePassphrase()
        database = AppDatabase.getDatabase(this, passphrase)

        // Ensure demo seed data is available for jury evaluation
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.seedHistoricalDemoData(database.wellnessDao())
        }
    }
}
