package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

object RealmeOemHelper {

    /**
     * Checks if the app is already ignoring battery optimization.
     */
    fun isBatteryIgnoringOptimization(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return if (pm != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pm.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        } else {
            true
        }
    }

    /**
     * Gets the Intent to request ignoring battery optimization.
     */
    fun getBatteryOptimizationIntent(context: Context): Intent {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    /**
     * Returns a text guide explaining standard ColorOS/Realme configurations.
     */
    fun getRealmeSettingsInstructions(): List<String> = listOf(
        "Auto-start: Settings -> Apps -> Auto-launch apps -> Enable App Lock.",
        "Battery Optimization: Settings -> Battery -> More settings -> App battery management -> App Lock -> Allow foreground, background and auto-launch activity.",
        "Pin App in Recents: Open recent apps panel -> Tap 3-dots on App Lock -> Tap 'Lock' to prevent system clearance."
    )

    /**
     * Attempt to launch ColorOS / Realme startup manager settings.
     */
    fun launchAutoStartSettings(context: Context): Boolean {
        val intents = listOf(
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startupapp.StartupAppListActivity")),
            Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.FakeActivity"))
        )

        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Continue trying fallback options
            }
        }
        return false
    }
}
