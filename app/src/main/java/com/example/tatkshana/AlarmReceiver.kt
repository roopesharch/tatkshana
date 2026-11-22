package com.example.tatkshana

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Tatkshana::AlarmWakeLock"
        )
        wakeLock.acquire(15_000L)

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("TASK_ID", taskId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}