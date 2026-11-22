package com.example.tatkshana

import java.util.UUID

enum class PriorityLevel(val label: String, val dismissalRule: String) {
    NORMAL("Normal", "Tap Dismiss"),
    MEDIUM("Medium", "Complete Checklist + Tap Dismiss"),
    HIGH("High", "Solve Challenge + Complete Checklist"),
    CRITICAL("Critical (ASAP)", "Capture Photo Proof + Self-Grade Task")
}

enum class ScheduleType(val label: String) {
    ONE_TIME("Specific Date & Time"),
    HOURLY_RANGE("Every N Hours (Date Range)"),
    RECURRING_DAYS("Weekly / Recurring Days")
}

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)

data class ReminderTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val priority: PriorityLevel = PriorityLevel.NORMAL,
    val scheduleType: ScheduleType = ScheduleType.ONE_TIME,
    val triggerTimeMillis: Long = System.currentTimeMillis() + 60_000L,
    val dateText: String = "2026-11-22",
    val timeText: String = "10:48 PM",
    val intervalHours: Int = 1,
    val durationDays: Int = 30,
    val endDateMillis: Long = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000,
    val activeStartHour: Int = 8,
    val activeEndHour: Int = 22,
    val recurringDays: List<String> = listOf("Mon", "Wed", "Fri"),
    val hourOfDay: Int = 9,
    val minuteOfHour: Int = 0,
    val ringtoneSource: String = "BUNDLED",
    val bundledResName: String = "tatkshana_default",
    val audioUri: String? = null,
    val audioFileName: String = "Tatkshana Default Ringtone",
    val snoozeMinutes: Int = 5,
    val todos: List<TodoItem> = emptyList(),
    val isEnabled: Boolean = true,
    val lastGrade: String? = null
)