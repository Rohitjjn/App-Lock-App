package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.EncryptedPrefs
import com.example.data.repository.AppLockRepository
import net.sqlcipher.database.SQLiteDatabase

class AppLockApplication : Application() {

    // Central core dependencies container
    lateinit var encryptedPrefs: EncryptedPrefs
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var repository: AppLockRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize SQLCipher libraries
        SQLiteDatabase.loadLibs(this)

        // Instantiate core dependencies manually
        encryptedPrefs = EncryptedPrefs(this)
        database = AppDatabase.getInstance(this, encryptedPrefs)
        repository = AppLockRepository(database.lockedAppDao())
    }
}
