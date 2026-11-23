package com.roopesh.tatkshana

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val tasks = TaskStorage.loadTasks(context)
            tasks.filter { it.isEnabled }.forEach { task ->
                AlarmScheduler.scheduleTask(context, task)
            }
        }
    }
}