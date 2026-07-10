package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val data = intent.dataString
        Log.d("SecretCodeReceiver", "Received action: $action, data: $data")
        
        if ("android.provider.Telephony.SECRET_CODE" == action) {
            try {
                val launchIntent = Intent(context, com.example.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(launchIntent)
            } catch (e: Exception) {
                Log.e("SecretCodeReceiver", "Failed to launch main activity", e)
            }
        }
    }
}
