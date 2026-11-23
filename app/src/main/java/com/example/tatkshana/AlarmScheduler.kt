package com.roopesh.tatkshana

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    fun scheduleTask(context: Context, task: ReminderTask, overrideTriggerMillis: Long? = null) {
        if (!task.isEnabled) {
            cancelTask(context, task.id)
            return
        }

        val triggerAt = overrideTriggerMillis ?: computeNextTriggerMillis(task) ?: run {
            cancelTask(context, task.id)
            return
        }

        val updated = task.copy(triggerTimeMillis = triggerAt)
        TaskStorage.updateTask(context, updated)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAt, pendingIntent),
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAt, pendingIntent),
                pendingIntent
            )
        }
    }

    fun snoozeTask(context: Context, task: ReminderTask, snoozeMinutes: Int) {
        val nextMillis = System.currentTimeMillis() + snoozeMinutes * 60_000L
        scheduleTask(context, task, overrideTriggerMillis = nextMillis)
    }

    fun cancelTask(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun computeNextTriggerMillis(task: ReminderTask, fromMillis: Long = System.currentTimeMillis()): Long? {
        return when (task.scheduleType) {
            ScheduleType.ONE_TIME -> {
                if (task.triggerTimeMillis > fromMillis) task.triggerTimeMillis else null
            }

            ScheduleType.HOURLY_RANGE -> {
                if (fromMillis > task.endDateMillis) return null
                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.HOUR_OF_DAY, task.intervalHours.coerceAtLeast(1))
                }
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                if (hour >= task.activeEndHour) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, task.activeStartHour)
                    cal.set(Calendar.MINUTE, 0)
                } else if (hour < task.activeStartHour) {
                    cal.set(Calendar.HOUR_OF_DAY, task.activeStartHour)
                    cal.set(Calendar.MINUTE, 0)
                }
                if (cal.timeInMillis <= task.endDateMillis) cal.timeInMillis else null
            }

            ScheduleType.RECURRING_DAYS -> {
                val dayMap = mapOf(
                    "Sun" to Calendar.SUNDAY,
                    "Mon" to Calendar.MONDAY,
                    "Tue" to Calendar.TUESDAY,
                    "Wed" to Calendar.WEDNESDAY,
                    "Thu" to Calendar.THURSDAY,
                    "Fri" to Calendar.FRIDAY,
                    "Sat" to Calendar.SATURDAY
                )
                val targetDays = task.recurringDays.mapNotNull { dayMap[it] }.toSet()
                if (targetDays.isEmpty()) return null

                val cal = Calendar.getInstance().apply {
                    timeInMillis = fromMillis
                    set(Calendar.HOUR_OF_DAY, task.hourOfDay)
                    set(Calendar.MINUTE, task.minuteOfHour)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= fromMillis) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                for (i in 0..7) {
                    if (targetDays.contains(cal.get(Calendar.DAY_OF_WEEK))) {
                        return cal.timeInMillis
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                null
            }
        }
    }
}