package com.roopesh.tatkshana

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class MonthDaySlot(
    val dayOfMonth: Int?,
    val dayKey: Long?
)

private fun calendarDayKey(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long =
    year.toLong() * 10_000L + (monthZeroBased + 1).toLong() * 100L + dayOfMonth.toLong()

private fun formatDayKeyReadable(dayKey: Long): String {
    val year = (dayKey / 10_000L).toInt()
    val month = ((dayKey % 10_000L) / 100L).toInt() - 1
    val day = (dayKey % 100L).toInt()
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month.coerceIn(0, 11))
        set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
    }
    return SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(cal.time)
}

private fun priorityDotColor(priority: PriorityLevel): Color = when (priority) {
    PriorityLevel.NORMAL -> Color(0xFF34D399)
    PriorityLevel.MEDIUM -> Color(0xFF38BDF8)
    PriorityLevel.HIGH -> Color(0xFFFBBF24)
    PriorityLevel.CRITICAL -> Color(0xFFF87171)
}

private fun weekdayCodeForCalendar(dayOfWeek: Int): String = when (dayOfWeek) {
    Calendar.MONDAY -> "Mon"
    Calendar.TUESDAY -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY -> "Thu"
    Calendar.FRIDAY -> "Fri"
    Calendar.SATURDAY -> "Sat"
    Calendar.SUNDAY -> "Sun"
    else -> "Mon"
}

private fun taskMatchesDayKey(task: ReminderTask, dayKey: Long): Boolean {
    val targetYear = (dayKey / 10_000L).toInt()
    val targetMonth = ((dayKey % 10_000L) / 100L).toInt() - 1
    val targetDay = (dayKey % 100L).toInt()

    val dayStart = Calendar.getInstance().apply {
        set(Calendar.YEAR, targetYear)
        set(Calendar.MONTH, targetMonth.coerceIn(0, 11))
        set(Calendar.DAY_OF_MONTH, targetDay.coerceIn(1, 31))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val dayEnd = (dayStart.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    val triggerCal = Calendar.getInstance().apply {
        timeInMillis = task.triggerTimeMillis
    }
    val triggerDayKey = calendarDayKey(
        triggerCal.get(Calendar.YEAR),
        triggerCal.get(Calendar.MONTH),
        triggerCal.get(Calendar.DAY_OF_MONTH)
    )

    return when (task.scheduleType) {
        ScheduleType.ONE_TIME -> triggerDayKey == dayKey

        ScheduleType.RECURRING_DAYS -> {
            if (dayEnd.timeInMillis < dayStart.timeInMillis) return false
            if (task.endDateMillis > 0L && dayStart.timeInMillis > task.endDateMillis) return false
            val code = weekdayCodeForCalendar(dayStart.get(Calendar.DAY_OF_WEEK))
            val allowedDays = if (task.recurringDays.isEmpty()) {
                setOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            } else {
                task.recurringDays.toSet()
            }
            code in allowedDays
        }

        ScheduleType.HOURLY_RANGE -> {
            val effectiveEnd = if (task.endDateMillis > 0L) task.endDateMillis else Long.MAX_VALUE
            val overlapsSpan = dayEnd.timeInMillis >= task.triggerTimeMillis && dayStart.timeInMillis <= effectiveEnd
            if (!overlapsSpan) return false
            if (task.recurringDays.isEmpty()) return true
            val code = weekdayCodeForCalendar(dayStart.get(Calendar.DAY_OF_WEEK))
            code in task.recurringDays.toSet()
        }
    }
}

private fun taskDisplayTimeForDay(task: ReminderTask, dayKey: Long): String {
    val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return when (task.scheduleType) {
        ScheduleType.HOURLY_RANGE -> {
            val startFmt = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, task.activeStartHour.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
            }
            val endFmt = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, task.activeEndHour.coerceIn(0, 23))
                set(Calendar.MINUTE, 0)
            }
            "Every ${task.intervalHours.coerceAtLeast(1)}h (${timeFmt.format(startFmt.time)} – ${timeFmt.format(endFmt.time)})"
        }
        else -> {
            val triggerCal = Calendar.getInstance().apply {
                timeInMillis = task.triggerTimeMillis
            }
            val triggerDayKey = calendarDayKey(
                triggerCal.get(Calendar.YEAR),
                triggerCal.get(Calendar.MONTH),
                triggerCal.get(Calendar.DAY_OF_MONTH)
            )
            if (task.scheduleType == ScheduleType.RECURRING_DAYS && triggerDayKey != dayKey) {
                val displayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, task.hourOfDay.coerceIn(0, 23))
                    set(Calendar.MINUTE, task.minuteOfHour.coerceIn(0, 59))
                }
                timeFmt.format(displayCal.time)
            } else {
                timeFmt.format(Date(task.triggerTimeMillis))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmCalendarScreen(
    tasks: List<ReminderTask>,
    onBack: () -> Unit,
    onEditTask: (ReminderTask) -> Unit,
    onCreateForDate: (Long) -> Unit
) {
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH)
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
    val todayKey = remember { calendarDayKey(todayYear, todayMonth, todayDay) }

    var displayYear by remember { mutableIntStateOf(todayYear) }
    var displayMonth by remember { mutableIntStateOf(todayMonth) }
    var onlyActiveAlarms by remember { mutableStateOf(false) }
    var selectedDayKeyForPopup by remember { mutableStateOf<Long?>(null) }

    val filteredTasks = remember(tasks, onlyActiveAlarms) {
        if (onlyActiveAlarms) tasks.filter { it.isEnabled } else tasks
    }

    val monthTitle = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val daySlots = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayYear)
            set(Calendar.MONTH, displayMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)
        val leadingBlanks = (firstDow + 5) % 7
        val totalDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        buildList {
            repeat(leadingBlanks) {
                add(MonthDaySlot(dayOfMonth = null, dayKey = null))
            }
            for (d in 1..totalDays) {
                add(MonthDaySlot(dayOfMonth = d, dayKey = calendarDayKey(displayYear, displayMonth, d)))
            }
        }
    }

    val tasksByDayKey = remember(filteredTasks, daySlots) {
        val map = mutableMapOf<Long, List<ReminderTask>>()
        daySlots.forEach { slot ->
            val key = slot.dayKey
            if (key != null) {
                val matching = filteredTasks.filter { taskMatchesDayKey(it, key) }
                if (matching.isNotEmpty()) {
                    map[key] = matching
                }
            }
        }
        map
    }

    val monthAlarmCount = remember(tasksByDayKey) {
        tasksByDayKey.values.sumOf { it.size }
    }

    val weekLabels = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111827),
                    titleContentColor = Color(0xFFF8FAFC)
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TatkshanaBrandBadge(sizeDp = 32)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Alarm Calendar",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Tap any day with dots to view or edit",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text(
                            text = "←",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            displayYear = todayYear
                            displayMonth = todayMonth
                        }
                    ) {
                        Text(
                            text = "Today",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B0F17), Color(0xFF101827))
                    )
                )
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161F31)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF28354D), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (displayMonth == 0) {
                                    displayMonth = 11
                                    displayYear -= 1
                                } else {
                                    displayMonth -= 1
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("◀", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = monthTitle,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = "$monthAlarmCount alarm event(s) in month",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                if (displayMonth == 11) {
                                    displayMonth = 0
                                    displayYear += 1
                                } else {
                                    displayMonth += 1
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("▶", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = !onlyActiveAlarms,
                            onClick = { onlyActiveAlarms = false },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.22f),
                                selectedLabelColor = Color(0xFF38BDF8)
                            ),
                            label = { Text("All Reminders", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = onlyActiveAlarms,
                            onClick = { onlyActiveAlarms = true },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF34D399).copy(alpha = 0.22f),
                                selectedLabelColor = Color(0xFF34D399)
                            ),
                            label = { Text("Active Only", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                weekLabels.forEach { label ->
                    val isWeekend = label == "Sat" || label == "Sun"
                    Text(
                        text = label,
                        color = if (isWeekend) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, Color(0xFF28354D), RoundedCornerShape(18.dp))
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(daySlots) { slot ->
                        val dayNum = slot.dayOfMonth
                        val dayKey = slot.dayKey
                        if (dayNum == null || dayKey == null) {
                            Box(modifier = Modifier.aspectRatio(0.88f))
                        } else {
                            val dayTasks = tasksByDayKey[dayKey].orEmpty()
                            val isToday = dayKey == todayKey
                            val isSelected = dayKey == selectedDayKeyForPopup
                            val hasAlarms = dayTasks.isNotEmpty()

                            val borderColor = when {
                                isSelected -> Color(0xFFFBBF24)
                                isToday -> Color(0xFF38BDF8)
                                hasAlarms -> Color(0xFF38BDF8).copy(alpha = 0.45f)
                                else -> Color(0xFF233046)
                            }

                            val bgColor = when {
                                isToday -> Color(0xFF17293E)
                                hasAlarms -> Color(0xFF192336)
                                else -> Color(0xFF0F1623)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .aspectRatio(0.88f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgColor)
                                    .border(
                                        width = if (isToday || isSelected) 1.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        selectedDayKeyForPopup = dayKey
                                    }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isToday || hasAlarms) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = when {
                                        isToday -> Color(0xFF38BDF8)
                                        hasAlarms -> Color(0xFFF8FAFC)
                                        else -> Color(0xFF94A3B8)
                                    }
                                )

                                if (hasAlarms) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            dayTasks.take(3).forEach { task ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(priorityDotColor(task.priority))
                                                )
                                            }
                                        }
                                        if (dayTasks.size > 3) {
                                            Text(
                                                text = "+${dayTasks.size - 3}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFBBF24)
                                            )
                                        } else {
                                            Text(
                                                text = "${dayTasks.size}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141C2B))
                    .border(1.dp, Color(0xFF233046), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(color = Color(0xFF34D399), label = "Normal")
                    LegendDot(color = Color(0xFF38BDF8), label = "Medium")
                    LegendDot(color = Color(0xFFFBBF24), label = "High")
                    LegendDot(color = Color(0xFFF87171), label = "Critical")
                }
            }
        }
    }

    val popupKey = selectedDayKeyForPopup
    if (popupKey != null) {
        val dayReminders = tasksByDayKey[popupKey].orEmpty()
        Dialog(onDismissRequest = { selectedDayKeyForPopup = null }) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = formatDayKeyReadable(popupKey),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = if (dayReminders.isEmpty()) {
                                    "No reminders or alarms on this date"
                                } else {
                                    "${dayReminders.size} reminder(s)/alarm(s) scheduled"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Surface(
                            color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                            shape = CircleShape
                        ) {
                            Text(
                                text = dayReminders.size.toString(),
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    if (dayReminders.isEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1623)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Free Day",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFE2E8F0)
                                )
                                Text(
                                    text = "You can schedule a new reminder or alarm for this date.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                        ) {
                            items(dayReminders, key = { it.id }) { item ->
                                val accent = priorityDotColor(item.priority)
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2234)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(accent)
                                                )
                                                Text(
                                                    text = item.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF8FAFC),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Surface(
                                                color = if (item.isEnabled) {
                                                    Color(0xFF34D399).copy(alpha = 0.16f)
                                                } else {
                                                    Color(0xFF64748B).copy(alpha = 0.2f)
                                                },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = if (item.isEnabled) "Active" else "Off",
                                                    color = if (item.isEnabled) Color(0xFF34D399) else Color(0xFF94A3B8),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${taskDisplayTimeForDay(item, popupKey)}  •  ${item.priority.label}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accent
                                        )

                                        if (item.notes.isNotBlank()) {
                                            Text(
                                                text = item.notes,
                                                fontSize = 12.sp,
                                                color = Color(0xFFCBD5E1),
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (item.todos.isNotEmpty()) {
                                            val doneCount = item.todos.count { it.isDone }
                                            Text(
                                                text = "Checklist: $doneCount/${item.todos.size} done",
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Button(
                                                onClick = {
                                                    selectedDayKeyForPopup = null
                                                    onEditTask(item)
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF38BDF8),
                                                    contentColor = Color(0xFF06131C)
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Edit Reminder",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { selectedDayKeyForPopup = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = {
                                selectedDayKeyForPopup = null
                                onCreateForDate(popupKey)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF34D399),
                                contentColor = Color(0xFF052E16)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+ Add for Date", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFCBD5E1),
            fontWeight = FontWeight.Medium
        )
    }
}