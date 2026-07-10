package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.ui.LockActivity

class SafeLockTileService : TileService() {

    companion object {
        private var activeInstance: SafeLockTileService? = null

        fun updateActiveTile() {
            activeInstance?.updateTileState()
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
    }

    override fun onDestroy() {
        activeInstance = null
        super.onDestroy()
    }

    override fun onStartListening() {
        super.onStartListening()
        activeInstance = this
        updateTileState()
    }

    override fun onStopListening() {
        super.onStopListening()
        // Keep instance if bound, though system might tear it down
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val encryptedPrefs = (application as com.example.AppLockApplication).encryptedPrefs
        val isEnabled = encryptedPrefs.isShieldEnabled()
        
        tile.label = "App Shield"
        tile.state = if (isEnabled) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isEnabled) "Shield ON" else "Shield OFF"
        }
        
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val launchIntent = Intent(this, LockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra("EXTRA_TILE_TOGGLE_ACTION", true)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    884,
                    launchIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
