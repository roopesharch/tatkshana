package com.example.tatkshana

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object TaskStorage {
    private const val PREFS_NAME = "tatkshana_prefs"
    private const val KEY_TASKS = "saved_tasks_json"

    fun loadTasks(context: Context): List<ReminderTask> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<ReminderTask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val todosArray = obj.optJSONArray("todos") ?: JSONArray()
                val todos = mutableListOf<TodoItem>()
                for (j in 0 until todosArray.length()) {
                    val tObj = todosArray.getJSONObject(j)
                    todos.add(
                        TodoItem(
                            id = tObj.getString("id"),
                            text = tObj.getString("text"),
                            isDone = tObj.optBoolean("isDone", false)
                        )
                    )
                }

                val daysArray = obj.optJSONArray("recurringDays") ?: JSONArray()
                val days = mutableListOf<String>()
                for (k in 0 until daysArray.length()) {
                    days.add(daysArray.getString(k))
                }

                list.add(
                    ReminderTask(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        notes = obj.optString("notes", ""),
                        priority = PriorityLevel.valueOf(obj.optString("priority", "NORMAL")),
                        scheduleType = ScheduleType.valueOf(obj.optString("scheduleType", "ONE_TIME")),
                        triggerTimeMillis = obj.optLong("triggerTimeMillis", System.currentTimeMillis() + 60_000L),
                        dateText = obj.optString("dateText", ""),
                        timeText = obj.optString("timeText", ""),
                        intervalHours = obj.optInt("intervalHours", 1),
                        durationDays = obj.optInt("durationDays", 30),
                        endDateMillis = obj.optLong("endDateMillis", System.currentTimeMillis() + 30L * 86_400_000L),
                        activeStartHour = obj.optInt("activeStartHour", 8),
                        activeEndHour = obj.optInt("activeEndHour", 22),
                        recurringDays = days,
                        hourOfDay = obj.optInt("hourOfDay", 9),
                        minuteOfHour = obj.optInt("minuteOfHour", 0),
                        ringtoneSource = obj.optString("ringtoneSource", "BUNDLED"),
                        bundledResName = obj.optString("bundledResName", "tatkshana_default"),
                        audioUri = if (obj.isNull("audioUri")) null else obj.optString("audioUri", null),
                        audioFileName = obj.optString("audioFileName", "Tatkshana Default Ringtone"),
                        snoozeMinutes = obj.optInt("snoozeMinutes", 5),
                        todos = todos,
                        isEnabled = obj.optBoolean("isEnabled", true),
                        lastGrade = if (obj.isNull("lastGrade")) null else obj.optString("lastGrade", null)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveTasks(context: Context, tasks: List<ReminderTask>) {
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("notes", task.notes)
                put("priority", task.priority.name)
                put("scheduleType", task.scheduleType.name)
                put("triggerTimeMillis", task.triggerTimeMillis)
                put("dateText", task.dateText)
                put("timeText", task.timeText)
                put("intervalHours", task.intervalHours)
                put("durationDays", task.durationDays)
                put("endDateMillis", task.endDateMillis)
                put("activeStartHour", task.activeStartHour)
                put("activeEndHour", task.activeEndHour)
                put("hourOfDay", task.hourOfDay)
                put("minuteOfHour", task.minuteOfHour)
                put("ringtoneSource", task.ringtoneSource)
                put("bundledResName", task.bundledResName)
                put("audioUri", task.audioUri ?: JSONObject.NULL)
                put("audioFileName", task.audioFileName)
                put("snoozeMinutes", task.snoozeMinutes)
                put("isEnabled", task.isEnabled)
                put("lastGrade", task.lastGrade ?: JSONObject.NULL)

                val daysArray = JSONArray()
                task.recurringDays.forEach { daysArray.put(it) }
                put("recurringDays", daysArray)

                val todosArray = JSONArray()
                task.todos.forEach { todo ->
                    todosArray.put(
                        JSONObject().apply {
                            put("id", todo.id)
                            put("text", todo.text)
                            put("isDone", todo.isDone)
                        }
                    )
                }
                put("todos", todosArray)
            }
            array.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    fun getTaskById(context: Context, id: String): ReminderTask? {
        return loadTasks(context).firstOrNull { it.id == id }
    }

    fun updateTask(context: Context, updated: ReminderTask) {
        val current = loadTasks(context).toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx != -1) {
            current[idx] = updated
            saveTasks(context, current)
        }
    }
}