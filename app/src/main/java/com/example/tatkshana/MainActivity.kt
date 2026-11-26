package com.roopesh.tatkshana

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val TatkshanaDarkScheme = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF06131C),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF1F1602),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1A2234),
    onSurfaceVariant = Color(0xFF94A3B8)
)

private data class PermissionGateItem(
    val title: String,
    val description: String,
    val isGranted: Boolean,
    val onRequest: () -> Unit
)

private enum class HourlyPeriodOption(val label: String, val subtitle: String) {
    SPECIFIC_DAY(
        label = "Specific Day",
        subtitle = "Runs on one date you pick from the calendar"
    ),
    SPECIFIED_DAYS(
        label = "Specified Days",
        subtitle = "Runs on selected weekdays (optionally from a chosen date)"
    ),
    FULL_MONTH(
        label = "Full Month",
        subtitle = "Runs every day across a chosen month & year"
    ),
    FULL_YEAR(
        label = "Full Year",
        subtitle = "Runs through December 31 of a chosen year"
    )
}

private enum class RecurringPatternMode(val label: String, val hint: String) {
    WEEKDAY_PRESETS(
        label = "Weekday Pattern",
        hint = "Quick presets or pick individual Mon–Sun days"
    ),
    RANDOM_CALENDAR(
        label = "Random / Custom Calendar",
        hint = "Open calendar to tap custom or random dates"
    ),
    YEAR_MONTHLY_DAYS(
        label = "Year & Month Day Pattern",
        hint = "Pick specific day numbers for each month across a full year"
    )
}

object AlarmLifecycleController {
    const val ACTION_TASKS_UPDATED = "com.roopesh.tatkshana.ACTION_TASKS_UPDATED"
    const val ACTION_MUTE_SOUND_KEEP_VIBRATION = "com.roopesh.tatkshana.ACTION_MUTE_SOUND_KEEP_VIBRATION"
    const val EXTRA_TASK_ID = "TASK_ID"

    fun onAlarmDismissed(context: Context, taskId: String): List<ReminderTask> {
        val current = TaskStorage.loadTasks(context)
        val updated = current.map { item ->
            if (item.id != taskId) {
                item
            } else {
                val nextTrigger = AlarmScheduler.computeNextTriggerMillis(item)
                val pastEnd = item.endDateMillis > 0L && (nextTrigger == null || nextTrigger > item.endDateMillis)
                if (item.scheduleType == ScheduleType.ONE_TIME || nextTrigger == null || pastEnd) {
                    item.copy(isEnabled = false)
                } else {
                    item.copy(triggerTimeMillis = nextTrigger, isEnabled = true)
                }
            }
        }
        TaskStorage.saveTasks(context, updated)
        AlarmScheduler.cancelTask(context, taskId)
        updated.firstOrNull { it.id == taskId && it.isEnabled }?.let { activeRecurring ->
            AlarmScheduler.scheduleTask(context, activeRecurring)
        }
        context.sendBroadcast(Intent(ACTION_TASKS_UPDATED).setPackage(context.packageName))
        return updated
    }

    fun completeFromPopupSilence(context: Context, taskId: String): List<ReminderTask> {
        AlarmScheduler.cancelTask(context, taskId)
        val current = TaskStorage.loadTasks(context)
        val updated = current.map { item ->
            if (item.id != taskId) {
                item
            } else {
                val nextTime = if (item.scheduleType == ScheduleType.ONE_TIME) {
                    null
                } else {
                    val candidate = AlarmScheduler.computeNextTriggerMillis(
                        item,
                        System.currentTimeMillis() + 5_000L
                    )
                    if (candidate != null && item.endDateMillis > 0L && candidate > item.endDateMillis) {
                        null
                    } else {
                        candidate
                    }
                }
                item.copy(
                    isEnabled = nextTime != null,
                    triggerTimeMillis = nextTime ?: item.triggerTimeMillis
                )
            }
        }
        TaskStorage.saveTasks(context, updated)
        updated.firstOrNull { it.id == taskId && it.isEnabled }?.let { activeNext ->
            AlarmScheduler.scheduleTask(context, activeNext, activeNext.triggerTimeMillis)
        }
        context.sendBroadcast(Intent(ACTION_TASKS_UPDATED).setPackage(context.packageName))
        return updated
    }

    fun muteSoundKeepVibration(context: Context, taskId: String? = null) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = "MUTE_SOUND_KEEP_VIBRATION"
            if (taskId != null) putExtra(EXTRA_TASK_ID, taskId)
        }
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {
        }
    }

    fun setRingingUiVisible(context: Context, visible: Boolean, taskId: String? = null) {
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            action = if (visible) "HIDE_POPUP_FOR_RINGING_UI" else "SHOW_POPUP_IF_NEEDED"
            if (taskId != null) putExtra(EXTRA_TASK_ID, taskId)
        }
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = TatkshanaDarkScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TatkshanaApp()
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || event.keyCode == KeyEvent.KEYCODE_VOLUME_UP)
        ) {
            AlarmLifecycleController.muteSoundKeepVibration(this)
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun TatkshanaBrandBadge(sizeDp: Int = 42) {
    val context = LocalContext.current
    val logoResId = remember {
        context.resources.getIdentifier("tatkshana_logo", "drawable", context.packageName)
    }
    if (logoResId != 0) {
        Image(
            painter = painterResource(id = logoResId),
            contentDescription = "Tatkshana Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFFF59E0B)))
                )
        ) {
            Text(
                text = "T",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (sizeDp * 0.45f).sp
            )
        }
    }
}

@Composable
private fun PermissionGateScreen(
    items: List<PermissionGateItem>,
    allGranted: Boolean,
    onRequestRuntimeBatch: () -> Unit,
    onProceed: () -> Unit
) {
    val grantedCount = items.count { it.isGranted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B0F17), Color(0xFF101827))
                )
            )
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TatkshanaBrandBadge(sizeDp = 46)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Permissions Required",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                )
                Text(
                    text = "Allow these permissions to use Tatkshana ($grantedCount/${items.size} granted)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onRequestRuntimeBatch,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                contentColor = Color(0xFF06131C)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Allow App Permissions (Notifications, Sound, Camera)",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                val statusColor = if (item.isGranted) Color(0xFF34D399) else Color(0xFFF87171)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2234)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = item.description,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = if (item.isGranted) "Allowed" else "Required",
                                color = statusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        if (!item.isGranted) {
                            OutlinedButton(
                                onClick = item.onRequest,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Allow", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onProceed,
            enabled = allGranted,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF34D399),
                contentColor = Color(0xFF052E16),
                disabledContainerColor = Color(0xFF1E293B),
                disabledContentColor = Color(0xFF64748B)
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(
                text = if (allGranted) "PROCEED TO TATKSHANA" else "ALLOW ALL REQUIRED PERMISSIONS TO PROCEED",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun FreeSpinWheel(
    label: String,
    items: List<String>,
    selectedIndex: Int,
    accent: Color,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val count = items.size
    if (count == 0) return

    val safeIndex = ((selectedIndex % count) + count) % count
    val rowHeight = 36.dp
    val rowPx = with(LocalDensity.current) { rowHeight.toPx() }
    val totalHeight = rowHeight * 5
    val scope = rememberCoroutineScope()

    val wheelPos = remember { Animatable(0f) }

    LaunchedEffect(safeIndex) {
        if (!wheelPos.isRunning) {
            wheelPos.snapTo(0f)
        }
    }

    fun modIdx(raw: Int): Int = ((raw % count) + count) % count

    suspend fun settleToNearest(currentDeltaItems: Float) {
        val roundedSteps = currentDeltaItems.roundToInt()
        val targetDelta = roundedSteps.toFloat()
        wheelPos.animateTo(
            targetValue = targetDelta,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
        val finalIndex = modIdx(safeIndex + roundedSteps)
        wheelPos.snapTo(0f)
        if (finalIndex != safeIndex) {
            onSelectedIndexChange(finalIndex)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF475569)
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFEEF2F6))
                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                .pointerInput(count, safeIndex) {
                    val velocityTracker = VelocityTracker()
                    detectDragGestures(
                        onDragStart = {
                            velocityTracker.resetTracking()
                            scope.launch { wheelPos.stop() }
                        },
                        onDragEnd = {
                            val velItemsPerSec = velocityTracker.calculateVelocity().y / rowPx
                            val initialItemVel = -velItemsPerSec * 0.45f
                            scope.launch {
                                if (abs(initialItemVel) > 0.6f) {
                                    val decayResult = wheelPos.animateDecay(
                                        initialVelocity = initialItemVel,
                                        animationSpec = exponentialDecay(
                                            frictionMultiplier = 1.15f,
                                            absVelocityThreshold = 0.1f
                                        )
                                    )
                                    settleToNearest(decayResult.endState.value)
                                } else {
                                    settleToNearest(wheelPos.value)
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { settleToNearest(wheelPos.value) }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val deltaItems = -dragAmount.y / rowPx
                            scope.launch {
                                wheelPos.snapTo(wheelPos.value + deltaItems)
                            }
                        }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .padding(horizontal = 6.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.White)
                    .border(1.5.dp, accent, RoundedCornerShape(11.dp))
            )

            val centerFloat = safeIndex.toFloat() + wheelPos.value
            val centerNearest = centerFloat.roundToInt()

            for (offset in -2..2) {
                val itemIdx = modIdx(centerNearest + offset)
                val exactPos = (centerNearest + offset).toFloat() - centerFloat
                val yPx = exactPos * rowPx
                val distFromCenter = abs(exactPos)

                val textColor = when {
                    distFromCenter < 0.55f -> Color(0xFF0F172A)
                    distFromCenter < 1.5f -> Color(0xFF475569)
                    else -> Color(0xFF94A3B8)
                }
                val textSize = when {
                    distFromCenter < 0.55f -> 20.sp
                    distFromCenter < 1.5f -> 15.sp
                    else -> 12.sp
                }
                val fontWeight = if (distFromCenter < 0.55f) FontWeight.ExtraBold else FontWeight.Medium

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset { IntOffset(0, yPx.roundToInt()) }
                        .fillMaxWidth()
                        .height(rowHeight)
                        .clickable {
                            val stepDiff = (centerNearest + offset) - safeIndex
                            scope.launch {
                                val target = stepDiff.toFloat()
                                wheelPos.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                                val nextIdx = modIdx(safeIndex + stepDiff)
                                wheelPos.snapTo(0f)
                                onSelectedIndexChange(nextIdx)
                            }
                        }
                ) {
                    Text(
                        text = items[itemIdx],
                        fontSize = textSize,
                        fontWeight = fontWeight,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WallClockDisplay(
    hour24: Int,
    minute: Int,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(2.dp, Color(0xFFCBD5E1), CircleShape)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 10f

                drawCircle(
                    color = Color(0xFFF1F5F9),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFCBD5E1),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )

                for (h in 0 until 12) {
                    val rad = (h * 30.0 - 90.0) * PI / 180.0
                    val isCardinal = h % 3 == 0
                    val inner = if (isCardinal) radius - 12f else radius - 7f
                    drawLine(
                        color = if (isCardinal) Color(0xFF0F172A) else Color(0xFF94A3B8),
                        start = Offset(
                            center.x + (inner * cos(rad)).toFloat(),
                            center.y + (inner * sin(rad)).toFloat()
                        ),
                        end = Offset(
                            center.x + (radius * cos(rad)).toFloat(),
                            center.y + (radius * sin(rad)).toFloat()
                        ),
                        strokeWidth = if (isCardinal) 3f else 1.8f,
                        cap = StrokeCap.Round
                    )
                }

                val hourVal = (hour24 % 12) + (minute / 60f)
                val hourRad = (hourVal * 30.0 - 90.0) * PI / 180.0
                val hourEnd = Offset(
                    center.x + (radius * 0.50f * cos(hourRad)).toFloat(),
                    center.y + (radius * 0.50f * sin(hourRad)).toFloat()
                )
                drawLine(
                    color = Color(0xFFD97706),
                    start = center,
                    end = hourEnd,
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )

                val minRad = (minute * 6.0 - 90.0) * PI / 180.0
                val minEnd = Offset(
                    center.x + (radius * 0.78f * cos(minRad)).toFloat(),
                    center.y + (radius * 0.78f * sin(minRad)).toFloat()
                )
                drawLine(
                    color = Color(0xFF0284C7),
                    start = center,
                    end = minEnd,
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )

                drawCircle(color = Color(0xFF0F172A), radius = 5.5f, center = center)
            }
        }

        Text(
            text = "Wall Clock Preview",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
private fun FreshTimePickerSheet(
    titleText: String = "Pick Alarm Time",
    initialHour24: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour24 by remember { mutableIntStateOf(initialHour24.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    val isPm = hour24 >= 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }

    val hourItems = remember { (1..12).map { String.format(Locale.getDefault(), "%02d", it) } }
    val minuteItems = remember { (0..59).map { String.format(Locale.getDefault(), "%02d", it) } }

    fun apply12Hour(h12: Int, pm: Boolean) {
        hour24 = if (pm) {
            if (h12 == 12) 12 else h12 + 12
        } else {
            if (h12 == 12) 0 else h12
        }
    }

    val formattedTime = remember(hour24, minute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
        }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFFCBD5E1), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = titleText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Slide wheels freely to set time",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FreeSpinWheel(
                        label = "HOUR",
                        items = hourItems,
                        selectedIndex = (hour12 - 1).coerceIn(0, 11),
                        accent = Color(0xFFD97706),
                        onSelectedIndexChange = { idx -> apply12Hour(idx + 1, isPm) },
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = ":",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    FreeSpinWheel(
                        label = "MINUTE",
                        items = minuteItems,
                        selectedIndex = minute.coerceIn(0, 59),
                        accent = Color(0xFF0284C7),
                        onSelectedIndexChange = { idx -> minute = idx },
                        modifier = Modifier.weight(1f)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        FilterChip(
                            selected = !isPm,
                            onClick = { apply12Hour(hour12, false) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFFEEF2F6),
                                labelColor = Color(0xFF334155),
                                selectedContainerColor = Color(0xFF0F172A),
                                selectedLabelColor = Color.White
                            ),
                            label = { Text("AM", fontWeight = FontWeight.Bold) }
                        )
                        FilterChip(
                            selected = isPm,
                            onClick = { apply12Hour(hour12, true) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFFEEF2F6),
                                labelColor = Color(0xFF334155),
                                selectedContainerColor = Color(0xFF0F172A),
                                selectedLabelColor = Color.White
                            ),
                            label = { Text("PM", fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                WallClockDisplay(
                    hour24 = hour24,
                    minute = minute,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF475569), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { onConfirm(hour24, minute) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F172A),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

private data class CalendarDayCell(
    val dayOfMonth: Int?,
    val epochDayKey: Long?
)

@Composable
private fun RandomCalendarPatternDialog(
    initialYear: Int,
    initialMonth: Int,
    initialPickedKeys: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var viewYear by remember { mutableIntStateOf(initialYear) }
    var viewMonth by remember { mutableIntStateOf(initialMonth.coerceIn(0, 11)) }
    val pickedKeys = remember {
        mutableStateListOf<Long>().apply { addAll(initialPickedKeys.sorted()) }
    }

    val monthsShort = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val weekHeaders = remember { listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") }

    fun toDayKey(y: Int, m: Int, d: Int): Long =
        y.toLong() * 10_000L + (m + 1).toLong() * 100L + d.toLong()

    fun formatKey(key: Long): String {
        val y = (key / 10_000L).toInt()
        val m = ((key % 10_000L) / 100L).toInt() - 1
        val d = (key % 100L).toInt()
        val cal = Calendar.getInstance().apply { set(y, m.coerceIn(0, 11), d.coerceIn(1, 31)) }
        return SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
    }

    val calendarCells = remember(viewYear, viewMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingBlanks = (firstDayOfWeek + 5) % 7
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        buildList {
            repeat(leadingBlanks) {
                add(CalendarDayCell(dayOfMonth = null, epochDayKey = null))
            }
            for (d in 1..daysInMonth) {
                add(CalendarDayCell(dayOfMonth = d, epochDayKey = toDayKey(viewYear, viewMonth, d)))
            }
        }
    }

    fun pickRandomDaysInCurrentMonth(count: Int = 7) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, viewYear)
            set(Calendar.MONTH, viewMonth)
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val candidates = (1..maxDay).shuffled(Random(System.nanoTime())).take(count.coerceAtMost(maxDay))
        val newKeys = candidates.map { toDayKey(viewYear, viewMonth, it) }
        newKeys.forEach { key ->
            if (!pickedKeys.contains(key)) pickedKeys.add(key)
        }
        pickedKeys.sort()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(22.dp))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Random & Custom Calendar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFF8FAFC)
                        )
                        Text(
                            text = "Tap any dates or generate a random pattern",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Surface(
                        color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${pickedKeys.size} picked",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            if (viewMonth == 0) {
                                viewMonth = 11
                                viewYear -= 1
                            } else {
                                viewMonth -= 1
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("◀", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${monthsShort[viewMonth]} $viewYear",
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )

                    OutlinedButton(
                        onClick = {
                            if (viewMonth == 11) {
                                viewMonth = 0
                                viewYear += 1
                            } else {
                                viewMonth += 1
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("▶", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { pickRandomDaysInCurrentMonth(7) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24),
                            contentColor = Color(0xFF1F1602)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎲 Random 7 Days", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { pickedKeys.clear() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(0.7f)
                    ) {
                        Text("Clear", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    weekHeaders.forEach { header ->
                        Text(
                            text = header,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    items(calendarCells) { cell ->
                        val key = cell.epochDayKey
                        val day = cell.dayOfMonth
                        if (day == null || key == null) {
                            Box(modifier = Modifier.aspectRatio(1f))
                        } else {
                            val isSelected = pickedKeys.contains(key)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF0F1623)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF233046),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        if (isSelected) {
                                            pickedKeys.remove(key)
                                        } else {
                                            pickedKeys.add(key)
                                            pickedKeys.sort()
                                        }
                                    }
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF06131C) else Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }

                if (pickedKeys.isNotEmpty()) {
                    val previewText = pickedKeys.sorted().take(8).joinToString(", ") { formatKey(it) }
                    val extra = if (pickedKeys.size > 8) " +${pickedKeys.size - 8} more" else ""
                    Text(
                        text = "Selected: $previewText$extra",
                        color = Color(0xFF34D399),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = { onConfirm(pickedKeys.toSet()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF06131C)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Pattern", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TatkshanaApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var canPostNotifications by remember { mutableStateOf(true) }
    var canExactAlarm by remember { mutableStateOf(true) }
    var canOverlay by remember { mutableStateOf(true) }
    var canAudioStorage by remember { mutableStateOf(true) }
    var canCamera by remember { mutableStateOf(true) }
    var hasPromptedOnLaunch by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        canPostNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        canExactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmMgr.canScheduleExactAlarms()
        } else {
            true
        }

        canOverlay = Settings.canDrawOverlays(context)

        canAudioStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                audioPermission
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

        canCamera = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshPermissions()
    }

    fun requestRuntimePermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !canPostNotifications) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (!canAudioStorage) {
                add(audioPermission)
            }
            if (!canCamera) {
                add(Manifest.permission.CAMERA)
            }
        }
        if (needed.isNotEmpty()) {
            multiPermissionLauncher.launch(needed.toTypedArray())
        } else {
            refreshPermissions()
        }
    }

    val tasks = remember {
        mutableStateListOf<ReminderTask>().apply {
            addAll(TaskStorage.loadTasks(context))
        }
    }

    fun reloadTasksFromStorage() {
        val fresh = TaskStorage.loadTasks(context)
        tasks.clear()
        tasks.addAll(fresh)
    }

    LaunchedEffect(Unit) {
        refreshPermissions()
        reloadTasksFromStorage()
        if (!hasPromptedOnLaunch) {
            hasPromptedOnLaunch = true
            requestRuntimePermissions()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
                reloadTasksFromStorage()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val allRequiredPermissionsGranted =
        canPostNotifications && canExactAlarm && canOverlay && canAudioStorage && canCamera

    var permissionGatePassed by remember { mutableStateOf(false) }

    val permissionItems = listOf(
        PermissionGateItem(
            title = "Notifications",
            description = "Show alarm notifications and background alerts",
            isGranted = canPostNotifications,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    multiPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                }
            }
        ),
        PermissionGateItem(
            title = "Exact Alarms",
            description = "Trigger reminders and recurring alarms at exact times",
            isGranted = canExactAlarm,
            onRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
        ),
        PermissionGateItem(
            title = "Display Over Other Apps",
            description = "Display ringing alarms above other apps",
            isGranted = canOverlay,
            onRequest = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
        ),
        PermissionGateItem(
            title = "Storage Sound / Audio",
            description = "Pick and play custom MP3 / audio ringtones from storage",
            isGranted = canAudioStorage,
            onRequest = {
                multiPermissionLauncher.launch(arrayOf(audioPermission))
            }
        ),
        PermissionGateItem(
            title = "Camera",
            description = "Capture photo proof for Critical priority tasks",
            isGranted = canCamera,
            onRequest = {
                multiPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        )
    )

    if (!permissionGatePassed && !allRequiredPermissionsGranted) {
        PermissionGateScreen(
            items = permissionItems,
            allGranted = allRequiredPermissionsGranted,
            onRequestRuntimeBatch = { requestRuntimePermissions() },
            onProceed = { permissionGatePassed = true }
        )
        return
    }

    fun persistAndSync(updatedList: List<ReminderTask>) {
        tasks.clear()
        tasks.addAll(updatedList)
        TaskStorage.saveTasks(context, updatedList)
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == AlarmLifecycleController.ACTION_TASKS_UPDATED) {
                    reloadTasksFromStorage()
                }
            }
        }
        val filter = IntentFilter(AlarmLifecycleController.ACTION_TASKS_UPDATED)
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {
            }
        }
    }

    var isFormOpen by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<ReminderTask?>(null) }

    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()) }

    if (isFormOpen) {
        ReminderFormScreen(
            existingTask = taskBeingEdited,
            allTasks = tasks.toList(),
            onBack = {
                isFormOpen = false
                taskBeingEdited = null
            },
            onSave = { savedTask ->
                val exists = tasks.any { it.id == savedTask.id }
                val baseList = if (exists) {
                    tasks.map { if (it.id == savedTask.id) savedTask else it }
                } else {
                    listOf(savedTask) + tasks
                }
                TaskStorage.saveTasks(context, baseList)
                AlarmScheduler.scheduleTask(context, savedTask)

                val syncedList = TaskStorage.loadTasks(context)
                tasks.clear()
                tasks.addAll(syncedList)

                val activeTask = syncedList.firstOrNull { it.id == savedTask.id } ?: savedTask
                Toast.makeText(
                    context,
                    "Saved! Next alarm: ${formatter.format(Date(activeTask.triggerTimeMillis))}",
                    Toast.LENGTH_LONG
                ).show()

                isFormOpen = false
                taskBeingEdited = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF111827),
                        titleContentColor = Color(0xFFF8FAFC)
                    ),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TatkshanaBrandBadge(sizeDp = 36)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tatkshana",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp
                                )
                                Text(
                                    text = "Priority Alarms & Task Proof",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        taskBeingEdited = null
                        isFormOpen = true
                    },
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF06131C),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "+ New",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
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
            ) {
                if (!canExactAlarm || !canOverlay) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1218)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .border(1.dp, Color(0xFFF87171).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Enable Background Permissions for Reliable Alarms:",
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (!canExactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                TextButton(
                                    onClick = {
                                        context.startActivity(
                                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        )
                                    }
                                ) {
                                    Text("Allow Exact Alarms", color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (!canOverlay) {
                                TextButton(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                ) {
                                    Text("Allow Display Over Other Apps", color = Color(0xFFF87171), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                val activeCount = tasks.count { it.isEnabled }
                val criticalCount = tasks.count { it.priority == PriorityLevel.CRITICAL }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    SummaryPill(
                        label = "Total",
                        value = tasks.size.toString(),
                        accent = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryPill(
                        label = "Active",
                        value = activeCount.toString(),
                        accent = Color(0xFF34D399),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryPill(
                        label = "Critical",
                        value = criticalCount.toString(),
                        accent = Color(0xFFF87171),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TatkshanaBrandBadge(sizeDp = 56)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No reminders yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color(0xFFF1F5F9)
                            )
                            Text(
                                text = "Tap + New to set an alarm or hourly task",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tasks, key = { it.id }) { task ->
                            ReminderCard(
                                task = task,
                                onToggleEnabled = { enabled ->
                                    val updatedTask = task.copy(isEnabled = enabled)
                                    val updatedList = tasks.map { if (it.id == task.id) updatedTask else it }
                                    persistAndSync(updatedList)
                                    if (enabled) {
                                        AlarmScheduler.scheduleTask(context, updatedTask)
                                    } else {
                                        AlarmScheduler.cancelTask(context, task.id)
                                    }
                                },
                                onToggleTodo = { todoId ->
                                    val updatedTodos = task.todos.map {
                                        if (it.id == todoId) it.copy(isDone = !it.isDone) else it
                                    }
                                    val updatedTask = task.copy(todos = updatedTodos)
                                    val updatedList = tasks.map { if (it.id == task.id) updatedTask else it }
                                    persistAndSync(updatedList)
                                },
                                onEdit = {
                                    taskBeingEdited = task
                                    isFormOpen = true
                                },
                                onTestRingNow = {
                                    val serviceIntent = Intent(context, AlarmService::class.java).apply {
                                        putExtra("TASK_ID", task.id)
                                    }
                                    ContextCompat.startForegroundService(context, serviceIntent)
                                },
                                onDelete = {
                                    AlarmScheduler.cancelTask(context, task.id)
                                    persistAndSync(tasks.filterNot { it.id == task.id })
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF1A2234),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
            Text(
                text = value,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun ReminderCard(
    task: ReminderTask,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleTodo: (String) -> Unit,
    onEdit: () -> Unit,
    onTestRingNow: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = priorityAccentColor(task.priority)

    val timeOnlyFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    var expandedWhenInactive by remember(task.id, task.isEnabled) { mutableStateOf(false) }
    val showFullDetails = task.isEnabled || expandedWhenInactive

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isEnabled) Color(0xFF1A2234) else Color(0xFF141B29)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .border(
                1.dp,
                if (task.isEnabled) Color(0xFF334155).copy(alpha = 0.55f) else Color(0xFF263248),
                RoundedCornerShape(16.dp)
            )
            .clickable(enabled = !task.isEnabled) {
                expandedWhenInactive = !expandedWhenInactive
            }
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = if (task.isEnabled) 14.dp else 10.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = priorityColor.copy(alpha = 0.16f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = task.priority.label.uppercase(),
                            color = priorityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            fontSize = if (task.isEnabled) 17.sp else 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.isEnabled) Color(0xFFF8FAFC) else Color(0xFFCBD5E1),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = timeOnlyFormatter.format(Date(task.triggerTimeMillis)),
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Switch(
                    checked = task.isEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) expandedWhenInactive = false
                        onToggleEnabled(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF06131C),
                        checkedTrackColor = Color(0xFF38BDF8)
                    )
                )
            }

            if (!task.isEnabled && !expandedWhenInactive) {
                Text(
                    text = "Tap to expand details",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            if (showFullDetails) {
                val scheduleDescription = when (task.scheduleType) {
                    ScheduleType.ONE_TIME -> "Once • ${formatter.format(Date(task.triggerTimeMillis))}"
                    ScheduleType.HOURLY_RANGE -> "Every ${task.intervalHours} hr(s) for ${task.durationDays} days • Next: ${formatter.format(Date(task.triggerTimeMillis))}"
                    ScheduleType.RECURRING_DAYS -> "Every ${task.recurringDays.joinToString(", ")} • Next: ${formatter.format(Date(task.triggerTimeMillis))}"
                }
                Text(
                    text = scheduleDescription,
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Text(
                    text = "Stop Rule: ${task.priority.dismissalRule}",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Text(
                    text = "Ringtone: ${task.audioFileName}  •  Snooze: ${task.snoozeMinutes}m",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )

                if (!task.lastGrade.isNullOrBlank()) {
                    Text(
                        text = "Last Grade: ${task.lastGrade}",
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F1623), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = task.notes,
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp
                        )
                    }
                }

                if (task.todos.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Checklist (${task.todos.count { it.isDone }}/${task.todos.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFF1F5F9)
                    )
                    task.todos.forEach { todo ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTodo(todo.id) }
                                .padding(vertical = 1.dp)
                        ) {
                            Checkbox(
                                checked = todo.isDone,
                                onCheckedChange = { onToggleTodo(todo.id) }
                            )
                            Text(
                                text = todo.text,
                                color = if (todo.isDone) Color(0xFF64748B) else Color(0xFFF1F5F9),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    TextButton(onClick = onEdit) {
                        Text(
                            text = "Edit",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    TextButton(onClick = onDelete) {
                        Text(
                            text = "Delete",
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = onTestRingNow,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ring Now (Test Sound & Stop Gate)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun priorityAccentColor(priority: PriorityLevel): Color = when (priority) {
    PriorityLevel.NORMAL -> Color(0xFF34D399)
    PriorityLevel.MEDIUM -> Color(0xFF38BDF8)
    PriorityLevel.HIGH -> Color(0xFFFBBF24)
    PriorityLevel.CRITICAL -> Color(0xFFF87171)
}

@Composable
private fun FormSectionCard(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF28354D), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = accent
                )
            }
            content()
        }
    }
}

private fun scheduleOptionTitle(type: ScheduleType): String = when (type) {
    ScheduleType.ONE_TIME -> "Specified Date & Time"
    ScheduleType.HOURLY_RANGE -> "Every N Hours"
    ScheduleType.RECURRING_DAYS -> "Weekly / Recurring Days"
}

private fun scheduleOptionSubtitle(type: ScheduleType): String = when (type) {
    ScheduleType.ONE_TIME -> "Rings once on a specific date and time"
    ScheduleType.HOURLY_RANGE -> "Repeats every N hours between a From and To time"
    ScheduleType.RECURRING_DAYS -> "Repeats in custom day patterns, calendar dates, or monthly year patterns"
}

private fun formatHourLabel(hour24: Int): String {
    val safeHour = hour24.coerceIn(0, 23)
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, safeHour)
        set(Calendar.MINUTE, 0)
    }
    return SimpleDateFormat("hh:00 a", Locale.getDefault()).format(cal.time)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormScreen(
    existingTask: ReminderTask?,
    allTasks: List<ReminderTask> = emptyList(),
    onBack: () -> Unit,
    onSave: (ReminderTask) -> Unit
) {
    val context = LocalContext.current
    val bundledRingtones = remember(context) { BundledRingtones.getAvailableRingtones(context) }
    val nowCal = remember { Calendar.getInstance() }
    val defaultYear = nowCal.get(Calendar.YEAR)
    val defaultMonth = nowCal.get(Calendar.MONTH)

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var selectedPriority by remember { mutableStateOf(existingTask?.priority ?: PriorityLevel.NORMAL) }
    var priorityDropdownExpanded by remember { mutableStateOf(false) }

    var selectedSchedule by remember { mutableStateOf(existingTask?.scheduleType ?: ScheduleType.ONE_TIME) }
    var scheduleDropdownExpanded by remember { mutableStateOf(false) }

    var recurringPatternMode by remember { mutableStateOf(RecurringPatternMode.WEEKDAY_PRESETS) }
    var patternDropdownExpanded by remember { mutableStateOf(false) }

    val randomCalendarKeys = remember { mutableStateListOf<Long>() }
    var showRandomCalendarDialog by remember { mutableStateOf(false) }

    val monthDayPatternMap = remember {
        mutableStateMapOf<Int, Set<Int>>().apply {
            for (m in 0..11) {
                put(m, emptySet())
            }
        }
    }
    var activePatternMonthIdx by remember { mutableIntStateOf(defaultMonth.coerceIn(0, 11)) }

    var hourlyPeriod by remember { mutableStateOf(HourlyPeriodOption.SPECIFIC_DAY) }
    var hourlyPeriodDropdownExpanded by remember { mutableStateOf(false) }
    var hourlyUseStartDate by remember { mutableStateOf(false) }

    val initialCal = remember {
        Calendar.getInstance().apply {
            if (existingTask != null && existingTask.triggerTimeMillis > System.currentTimeMillis()) {
                timeInMillis = existingTask.triggerTimeMillis
            } else {
                add(Calendar.MINUTE, 1)
            }
        }
    }
    var pickedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var pickedMonth by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }
    var pickedDay by remember { mutableIntStateOf(initialCal.get(Calendar.DAY_OF_MONTH)) }
    var pickedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var pickedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    var selectedMonthIdx by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH).coerceIn(0, 11)) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    var selectedYearValue by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR).coerceAtLeast(defaultYear)) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }

    val monthsShort = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }
    val monthsOfYear = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
    val selectableYears = remember(defaultYear) {
        (defaultYear..(defaultYear + 10)).toList()
    }

    val formattedPickedTime = remember(pickedHour, pickedMinute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, pickedHour)
            set(Calendar.MINUTE, pickedMinute)
        }
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(cal.time)
    }

    val formattedPickedDate = remember(pickedYear, pickedMonth, pickedDay) {
        val cal = Calendar.getInstance().apply {
            set(pickedYear, pickedMonth, pickedDay)
        }
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    if (showTimePickerDialog) {
        FreshTimePickerSheet(
            initialHour24 = pickedHour,
            initialMinute = pickedMinute,
            onDismiss = { showTimePickerDialog = false },
            onConfirm = { h, min ->
                pickedHour = h
                pickedMinute = min
                showTimePickerDialog = false
            }
        )
    }

    if (showRandomCalendarDialog) {
        RandomCalendarPatternDialog(
            initialYear = selectedYearValue,
            initialMonth = selectedMonthIdx,
            initialPickedKeys = randomCalendarKeys.toSet(),
            onDismiss = { showRandomCalendarDialog = false },
            onConfirm = { updatedKeys ->
                randomCalendarKeys.clear()
                randomCalendarKeys.addAll(updatedKeys.sorted())
                showRandomCalendarDialog = false
            }
        )
    }

    var intervalHours by remember { mutableStateOf((existingTask?.intervalHours ?: 1).toString()) }
    var fromHour by remember { mutableIntStateOf((existingTask?.activeStartHour ?: 8).coerceIn(0, 23)) }
    var toHour by remember { mutableIntStateOf((existingTask?.activeEndHour ?: 22).coerceIn(0, 23)) }
    var pickingRollingFromTime by remember { mutableStateOf<Boolean?>(null) }

    if (pickingRollingFromTime != null) {
        val isFrom = pickingRollingFromTime == true
        FreshTimePickerSheet(
            titleText = if (isFrom) "Rolling Window: From Time" else "Rolling Window: To Time",
            initialHour24 = if (isFrom) fromHour else toHour,
            initialMinute = 0,
            onDismiss = { pickingRollingFromTime = null },
            onConfirm = { h, _ ->
                if (isFrom) fromHour = h else toHour = h
                pickingRollingFromTime = null
            }
        )
    }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            addAll(existingTask?.recurringDays ?: listOf("Mon", "Wed", "Fri"))
        }
    }

    fun applyDayPreset(presetDays: List<String>) {
        selectedDays.clear()
        selectedDays.addAll(presetDays)
    }

    val defaultBundled = bundledRingtones.firstOrNull()?.rawResName ?: "tatkshana_default"
    val defaultBundledLabel = bundledRingtones.firstOrNull()?.displayName ?: "Tatkshana Default Ringtone"

    var ringtoneSource by remember { mutableStateOf(existingTask?.ringtoneSource ?: "BUNDLED") }
    var selectedBundledRes by remember { mutableStateOf(existingTask?.bundledResName ?: defaultBundled) }
    var audioUri by remember { mutableStateOf(existingTask?.audioUri) }
    var audioFileName by remember {
        mutableStateOf(existingTask?.audioFileName ?: defaultBundledLabel)
    }
    var soundDropdownExpanded by remember { mutableStateOf(false) }

    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    fun stopPreviewSound() {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {
        }
        previewPlayer = null
        isPreviewPlaying = false
    }

    fun playPreviewSound(source: String, bundledName: String, customUriString: String?) {
        stopPreviewSound()

        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val bundledUri = BundledRingtones.resolveUri(context, bundledName)
        val uriToPlay: Uri = when (source) {
            "STORAGE" -> if (!customUriString.isNullOrBlank()) Uri.parse(customUriString) else (bundledUri ?: defaultUri)
            "BUNDLED" -> bundledUri ?: defaultUri
            else -> defaultUri
        } ?: return

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uriToPlay)
                isLooping = false
                setOnCompletionListener {
                    stopPreviewSound()
                }
                prepare()
                start()
            }
            previewPlayer = player
            isPreviewPlaying = true
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Unable to preview this sound track (missing raw file or audio format)",
                Toast.LENGTH_SHORT
            ).show()
            stopPreviewSound()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreviewSound()
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val newSource = "STORAGE"
            val newUriString = uri.toString()
            var name = "Custom Storage Audio"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
            ringtoneSource = newSource
            audioUri = newUriString
            audioFileName = name
            playPreviewSound(newSource, selectedBundledRes, newUriString)
        }
    }

    val snoozePresets = listOf("5m" to 5, "15m" to 15, "1h" to 60, "1d" to 1440)
    var selectedSnoozeMins by remember { mutableIntStateOf(existingTask?.snoozeMinutes ?: 5) }
    var customSnoozeText by remember { mutableStateOf((existingTask?.snoozeMinutes ?: 5).toString()) }

    var newTodoText by remember { mutableStateOf("") }
    val todoList = remember {
        mutableStateListOf<TodoItem>().apply {
            addAll(existingTask?.todos ?: emptyList())
        }
    }

    val inactiveOtherTasks = remember(allTasks, existingTask) {
        allTasks.filter { !it.isEnabled && it.id != existingTask?.id }
    }
    var expandedInactiveInEditIds by remember { mutableStateOf(setOf<String>()) }
    val inactiveTimeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    fun submitForm() {
        stopPreviewSound()
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            Toast.makeText(context, "Please enter a Reminder Title first", Toast.LENGTH_SHORT).show()
            return
        }

        val stepHours = intervalHours.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val safeFromHour = fromHour.coerceIn(0, 23)
        val safeToHour = toHour.coerceIn(0, 23)

        val cal = Calendar.getInstance().apply {
            when (selectedSchedule) {
                ScheduleType.ONE_TIME -> {
                    set(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute, 0)
                }
                ScheduleType.HOURLY_RANGE -> {
                    when (hourlyPeriod) {
                        HourlyPeriodOption.SPECIFIC_DAY -> {
                            set(pickedYear, pickedMonth, pickedDay, safeFromHour, 0, 0)
                        }
                        HourlyPeriodOption.SPECIFIED_DAYS -> {
                            if (hourlyUseStartDate) {
                                set(pickedYear, pickedMonth, pickedDay, safeFromHour, 0, 0)
                            } else {
                                set(Calendar.HOUR_OF_DAY, safeFromHour)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                        }
                        HourlyPeriodOption.FULL_MONTH -> {
                            set(Calendar.YEAR, selectedYearValue)
                            set(Calendar.MONTH, selectedMonthIdx)
                            val startDay = if (selectedYearValue == defaultYear && selectedMonthIdx == defaultMonth) {
                                nowCal.get(Calendar.DAY_OF_MONTH)
                            } else {
                                1
                            }
                            set(Calendar.DAY_OF_MONTH, startDay)
                            set(Calendar.HOUR_OF_DAY, safeFromHour)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        HourlyPeriodOption.FULL_YEAR -> {
                            set(Calendar.YEAR, selectedYearValue)
                            val startMonth = if (selectedYearValue == defaultYear) defaultMonth else Calendar.JANUARY
                            val startDay = if (selectedYearValue == defaultYear) nowCal.get(Calendar.DAY_OF_MONTH) else 1
                            set(Calendar.MONTH, startMonth)
                            set(Calendar.DAY_OF_MONTH, startDay)
                            set(Calendar.HOUR_OF_DAY, safeFromHour)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                    }
                }
                ScheduleType.RECURRING_DAYS -> {
                    when (recurringPatternMode) {
                        RecurringPatternMode.WEEKDAY_PRESETS -> {
                            set(Calendar.HOUR_OF_DAY, pickedHour)
                            set(Calendar.MINUTE, pickedMinute)
                            set(Calendar.SECOND, 0)
                        }
                        RecurringPatternMode.RANDOM_CALENDAR -> {
                            val firstKey = randomCalendarKeys.minOrNull()
                            if (firstKey != null) {
                                val y = (firstKey / 10_000L).toInt()
                                val m = ((firstKey % 10_000L) / 100L).toInt() - 1
                                val d = (firstKey % 100L).toInt()
                                set(y, m.coerceIn(0, 11), d.coerceIn(1, 31), pickedHour, pickedMinute, 0)
                            } else {
                                set(Calendar.HOUR_OF_DAY, pickedHour)
                                set(Calendar.MINUTE, pickedMinute)
                                set(Calendar.SECOND, 0)
                            }
                        }
                        RecurringPatternMode.YEAR_MONTHLY_DAYS -> {
                            set(Calendar.YEAR, selectedYearValue)
                            val firstMonthWithDays = (0..11).firstOrNull { !monthDayPatternMap[it].isNullOrEmpty() }
                                ?: activePatternMonthIdx
                            val firstDay = monthDayPatternMap[firstMonthWithDays]?.minOrNull() ?: 1
                            set(Calendar.MONTH, firstMonthWithDays)
                            set(Calendar.DAY_OF_MONTH, firstDay)
                            set(Calendar.HOUR_OF_DAY, pickedHour)
                            set(Calendar.MINUTE, pickedMinute)
                            set(Calendar.SECOND, 0)
                        }
                    }
                }
            }
            set(Calendar.MILLISECOND, 0)
        }

        val targetMillis = if (selectedSchedule == ScheduleType.ONE_TIME) {
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                System.currentTimeMillis() + 15_000L
            } else {
                cal.timeInMillis
            }
        } else {
            cal.timeInMillis
        }

        val endMillis = when (selectedSchedule) {
            ScheduleType.ONE_TIME -> targetMillis
            ScheduleType.HOURLY_RANGE -> when (hourlyPeriod) {
                HourlyPeriodOption.SPECIFIC_DAY -> {
                    Calendar.getInstance().apply {
                        timeInMillis = targetMillis
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }
                HourlyPeriodOption.SPECIFIED_DAYS -> {
                    System.currentTimeMillis() + 365L * 86_400_000L
                }
                HourlyPeriodOption.FULL_MONTH -> {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYearValue)
                        set(Calendar.MONTH, selectedMonthIdx)
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }
                HourlyPeriodOption.FULL_YEAR -> {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYearValue)
                        set(Calendar.MONTH, Calendar.DECEMBER)
                        set(Calendar.DAY_OF_MONTH, 31)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }
            }
            ScheduleType.RECURRING_DAYS -> when (recurringPatternMode) {
                RecurringPatternMode.WEEKDAY_PRESETS -> {
                    System.currentTimeMillis() + 365L * 86_400_000L
                }
                RecurringPatternMode.RANDOM_CALENDAR -> {
                    val lastKey = randomCalendarKeys.maxOrNull()
                    if (lastKey != null) {
                        val y = (lastKey / 10_000L).toInt()
                        val m = ((lastKey % 10_000L) / 100L).toInt() - 1
                        val d = (lastKey % 100L).toInt()
                        Calendar.getInstance().apply {
                            set(y, m.coerceIn(0, 11), d.coerceIn(1, 31), 23, 59, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                    } else {
                        System.currentTimeMillis() + 365L * 86_400_000L
                    }
                }
                RecurringPatternMode.YEAR_MONTHLY_DAYS -> {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYearValue)
                        set(Calendar.MONTH, Calendar.DECEMBER)
                        set(Calendar.DAY_OF_MONTH, 31)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                }
            }
        }

        val daysCount = ((endMillis - System.currentTimeMillis()).coerceAtLeast(86_400_000L) / 86_400_000L)
            .toInt()
            .coerceAtLeast(1)
        val finalSnooze = customSnoozeText.toIntOrNull()?.coerceAtLeast(1) ?: selectedSnoozeMins

        val effectiveDaysForHourly = when (hourlyPeriod) {
            HourlyPeriodOption.SPECIFIED_DAYS -> if (selectedDays.isEmpty()) allDays else selectedDays.toList()
            else -> allDays
        }

        val effectiveRecurringDays = if (selectedDays.isEmpty()) allDays else selectedDays.toList()

        val draft = ReminderTask(
            id = existingTask?.id ?: UUID.randomUUID().toString(),
            title = cleanTitle,
            notes = notes.trim(),
            priority = selectedPriority,
            scheduleType = selectedSchedule,
            triggerTimeMillis = targetMillis,
            intervalHours = stepHours,
            durationDays = daysCount,
            endDateMillis = endMillis,
            activeStartHour = safeFromHour,
            activeEndHour = safeToHour,
            recurringDays = if (selectedSchedule == ScheduleType.HOURLY_RANGE) {
                effectiveDaysForHourly
            } else {
                effectiveRecurringDays
            },
            hourOfDay = if (selectedSchedule == ScheduleType.HOURLY_RANGE) safeFromHour else pickedHour,
            minuteOfHour = if (selectedSchedule == ScheduleType.HOURLY_RANGE) 0 else pickedMinute,
            ringtoneSource = ringtoneSource,
            bundledResName = selectedBundledRes,
            audioUri = audioUri,
            audioFileName = audioFileName,
            snoozeMinutes = finalSnooze,
            todos = todoList.toList(),
            isEnabled = true,
            lastGrade = existingTask?.lastGrade
        )

        val computedNext = AlarmScheduler.computeNextTriggerMillis(draft)
        val finalTask = draft.copy(
            triggerTimeMillis = when (selectedSchedule) {
                ScheduleType.ONE_TIME -> targetMillis
                else -> computedNext ?: targetMillis
            }
        )
        onSave(finalTask)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111827)),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TatkshanaBrandBadge(sizeDp = 28)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (existingTask == null) "New Reminder" else "Edit Reminder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            stopPreviewSound()
                            onBack()
                        }
                    ) {
                        Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                },
                actions = {
                    Button(
                        onClick = { submitForm() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color(0xFF06131C)
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("SAVE", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0F17))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FormSectionCard(title = "REMINDER & PRIORITY", accent = Color(0xFFFBBF24)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *", fontSize = 11.sp) },
                        placeholder = { Text("Drink water / Check", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1.35f)
                    )

                    Box(modifier = Modifier.weight(0.95f)) {
                        OutlinedButton(
                            onClick = { priorityDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = selectedPriority.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = priorityAccentColor(selectedPriority),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "▼",
                                    color = priorityAccentColor(selectedPriority),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = priorityDropdownExpanded,
                            onDismissRequest = { priorityDropdownExpanded = false },
                            modifier = Modifier.background(Color(0xFF1A2234))
                        ) {
                            PriorityLevel.entries.forEach { level ->
                                val levelColor = priorityAccentColor(level)
                                val isSelected = selectedPriority == level
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = level.label,
                                                color = if (isSelected) levelColor else Color(0xFFF8FAFC),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = level.dismissalRule,
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPriority = level
                                        priorityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Stop: ${selectedPriority.dismissalRule}",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FormSectionCard(title = "SCHEDULE & TIME", accent = Color(0xFF38BDF8)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { scheduleDropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scheduleOptionTitle(selectedSchedule),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF8FAFC)
                                )
                                Text(
                                    text = scheduleOptionSubtitle(selectedSchedule),
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("▼", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    DropdownMenu(
                        expanded = scheduleDropdownExpanded,
                        onDismissRequest = { scheduleDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1A2234))
                    ) {
                        ScheduleType.entries.forEach { type ->
                            val isSelected = selectedSchedule == type
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = scheduleOptionTitle(type),
                                            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = scheduleOptionSubtitle(type),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedSchedule = type
                                    scheduleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (selectedSchedule == ScheduleType.ONE_TIME) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showTimePickerDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Time: $formattedPickedTime", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        pickedYear = y
                                        pickedMonth = m
                                        pickedDay = d
                                    },
                                    pickedYear,
                                    pickedMonth,
                                    pickedDay
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Date: $formattedPickedDate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (selectedSchedule == ScheduleType.RECURRING_DAYS) {
                    OutlinedButton(
                        onClick = { showTimePickerDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Alarm Time: $formattedPickedTime",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = "Tap to change",
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                when (selectedSchedule) {
                    ScheduleType.ONE_TIME -> Unit

                    ScheduleType.HOURLY_RANGE -> {
                        val stepVal = intervalHours.toIntOrNull()?.coerceAtLeast(1) ?: 1

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = intervalHours,
                                onValueChange = { intervalHours = it },
                                label = { Text("Every N Hrs", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(0.9f)
                            )

                            OutlinedButton(
                                onClick = { pickingRollingFromTime = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "From: ${formatHourLabel(fromHour)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = { pickingRollingFromTime = false },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "To: ${formatHourLabel(toHour)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { hourlyPeriodDropdownExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Schedule Span: ${hourlyPeriod.label}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Text(
                                            text = hourlyPeriod.subtitle,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("▼", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            DropdownMenu(
                                expanded = hourlyPeriodDropdownExpanded,
                                onDismissRequest = { hourlyPeriodDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1A2234))
                            ) {
                                HourlyPeriodOption.entries.forEach { period ->
                                    val isChosen = hourlyPeriod == period
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = period.label,
                                                    color = if (isChosen) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = period.subtitle,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            hourlyPeriod = period
                                            hourlyPeriodDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        when (hourlyPeriod) {
                            HourlyPeriodOption.SPECIFIC_DAY -> {
                                OutlinedButton(
                                    onClick = {
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                pickedYear = y
                                                pickedMonth = m
                                                pickedDay = d
                                            },
                                            pickedYear,
                                            pickedMonth,
                                            pickedDay
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Pick Date: $formattedPickedDate",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            HourlyPeriodOption.SPECIFIED_DAYS -> {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(allDays) { day ->
                                        val isSelected = selectedDays.contains(day)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                                            },
                                            label = { Text(day, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { hourlyUseStartDate = !hourlyUseStartDate }
                                    ) {
                                        Checkbox(
                                            checked = hourlyUseStartDate,
                                            onCheckedChange = { hourlyUseStartDate = it }
                                        )
                                        Text(
                                            text = "Start from specific date",
                                            color = Color(0xFFCBD5E1),
                                            fontSize = 12.sp
                                        )
                                    }

                                    if (hourlyUseStartDate) {
                                        OutlinedButton(
                                            onClick = {
                                                DatePickerDialog(
                                                    context,
                                                    { _, y, m, d ->
                                                        pickedYear = y
                                                        pickedMonth = m
                                                        pickedDay = d
                                                    },
                                                    pickedYear,
                                                    pickedMonth,
                                                    pickedDay
                                                ).show()
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(formattedPickedDate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            HourlyPeriodOption.FULL_MONTH -> {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.weight(1.3f)) {
                                        OutlinedButton(
                                            onClick = { monthDropdownExpanded = true },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = monthsOfYear[selectedMonthIdx],
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text("▼", color = Color(0xFF38BDF8), fontSize = 11.sp)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = monthDropdownExpanded,
                                            onDismissRequest = { monthDropdownExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1A2234))
                                        ) {
                                            monthsOfYear.forEachIndexed { idx, monthName ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = monthName,
                                                            color = if (idx == selectedMonthIdx) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
                                                            fontWeight = if (idx == selectedMonthIdx) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedMonthIdx = idx
                                                        monthDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { yearDropdownExpanded = true },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "Year: $selectedYearValue",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFBBF24)
                                                )
                                                Text("▼", color = Color(0xFFFBBF24), fontSize = 11.sp)
                                            }
                                        }

                                        DropdownMenu(
                                            expanded = yearDropdownExpanded,
                                            onDismissRequest = { yearDropdownExpanded = false },
                                            modifier = Modifier.background(Color(0xFF1A2234))
                                        ) {
                                            selectableYears.forEach { yr ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = yr.toString(),
                                                            color = if (yr == selectedYearValue) Color(0xFFFBBF24) else Color(0xFFF8FAFC),
                                                            fontWeight = if (yr == selectedYearValue) FontWeight.Bold else FontWeight.Normal,
                                                            fontSize = 12.sp
                                                        )
                                                    },
                                                    onClick = {
                                                        selectedYearValue = yr
                                                        yearDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            HourlyPeriodOption.FULL_YEAR -> {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(
                                        onClick = { yearDropdownExpanded = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Full Year: $selectedYearValue",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFFBBF24)
                                            )
                                            Text("▼", color = Color(0xFFFBBF24), fontSize = 11.sp)
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = yearDropdownExpanded,
                                        onDismissRequest = { yearDropdownExpanded = false },
                                        modifier = Modifier.background(Color(0xFF1A2234))
                                    ) {
                                        selectableYears.forEach { yr ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        text = yr.toString(),
                                                        color = if (yr == selectedYearValue) Color(0xFFFBBF24) else Color(0xFFF8FAFC),
                                                        fontWeight = if (yr == selectedYearValue) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                onClick = {
                                                    selectedYearValue = yr
                                                    yearDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val spanSummary = when (hourlyPeriod) {
                            HourlyPeriodOption.SPECIFIC_DAY -> "on $formattedPickedDate"
                            HourlyPeriodOption.SPECIFIED_DAYS -> {
                                val daysLabel = if (selectedDays.isEmpty()) "every day" else selectedDays.joinToString(", ")
                                if (hourlyUseStartDate) "on $daysLabel from $formattedPickedDate" else "on $daysLabel"
                            }
                            HourlyPeriodOption.FULL_MONTH -> "throughout ${monthsOfYear[selectedMonthIdx]} $selectedYearValue"
                            HourlyPeriodOption.FULL_YEAR -> "throughout $selectedYearValue"
                        }
                        Surface(
                            color = Color(0xFF0F1623),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF233046), RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = "Rings every $stepVal hr(s) from ${formatHourLabel(fromHour)} to ${formatHourLabel(toHour)} $spanSummary",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    ScheduleType.RECURRING_DAYS -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri")
                            val weekends = listOf("Sat", "Sun")
                            val isWeekdays = selectedDays.toSet() == weekdays.toSet()
                            val isWeekends = selectedDays.toSet() == weekends.toSet()
                            val isAllDays = selectedDays.toSet() == allDays.toSet()

                            FilterChip(
                                selected = isWeekdays,
                                onClick = {
                                    recurringPatternMode = RecurringPatternMode.WEEKDAY_PRESETS
                                    applyDayPreset(weekdays)
                                },
                                label = { Text("Weekdays", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = isWeekends,
                                onClick = {
                                    recurringPatternMode = RecurringPatternMode.WEEKDAY_PRESETS
                                    applyDayPreset(weekends)
                                },
                                label = { Text("Weekends", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = isAllDays,
                                onClick = {
                                    recurringPatternMode = RecurringPatternMode.WEEKDAY_PRESETS
                                    applyDayPreset(allDays)
                                },
                                label = { Text("Every Day", fontSize = 11.sp) }
                            )
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allDays) { day ->
                                val isSelected = selectedDays.contains(day)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                                    },
                                    label = { Text(day, fontSize = 11.sp) }
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { patternDropdownExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Pattern Mode: ${recurringPatternMode.label}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF8FAFC)
                                        )
                                        Text(
                                            text = recurringPatternMode.hint,
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("▼", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            DropdownMenu(
                                expanded = patternDropdownExpanded,
                                onDismissRequest = { patternDropdownExpanded = false },
                                modifier = Modifier.background(Color(0xFF1A2234))
                            ) {
                                RecurringPatternMode.entries.forEach { mode ->
                                    val isChosen = recurringPatternMode == mode
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    text = mode.label,
                                                    color = if (isChosen) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = mode.hint,
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 11.sp
                                                )
                                            }
                                        },
                                        onClick = {
                                            recurringPatternMode = mode
                                            patternDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        when (recurringPatternMode) {
                            RecurringPatternMode.WEEKDAY_PRESETS -> {
                                val summaryDays = if (selectedDays.isEmpty()) "Every Day" else selectedDays.joinToString(", ")
                                Surface(
                                    color = Color(0xFF0F1623),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF233046), RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = "Rings at $formattedPickedTime on $summaryDays",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            RecurringPatternMode.RANDOM_CALENDAR -> {
                                Button(
                                    onClick = { showRandomCalendarDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF1E293B),
                                        contentColor = Color(0xFF38BDF8)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                ) {
                                    Text(
                                        text = if (randomCalendarKeys.isEmpty()) {
                                            "📅  Select Random / Custom Pattern on Calendar"
                                        } else {
                                            "📅  Edit Random / Custom Pattern (${randomCalendarKeys.size} dates)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(
                                    color = Color(0xFF0F1623),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF233046), RoundedCornerShape(10.dp))
                                ) {
                                    val infoText = if (randomCalendarKeys.isEmpty()) {
                                        "Tap the calendar button above to pick random or custom dates at $formattedPickedTime"
                                    } else {
                                        "Custom pattern (${randomCalendarKeys.size} dates) at $formattedPickedTime"
                                    }
                                    Text(
                                        text = infoText,
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }

                            RecurringPatternMode.YEAR_MONTHLY_DAYS -> {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1623)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF233046), RoundedCornerShape(12.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Select Days per Month",
                                                color = Color(0xFFF8FAFC),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )

                                            Box {
                                                OutlinedButton(
                                                    onClick = { yearDropdownExpanded = true },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Year: $selectedYearValue ▼",
                                                        color = Color(0xFFFBBF24),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                DropdownMenu(
                                                    expanded = yearDropdownExpanded,
                                                    onDismissRequest = { yearDropdownExpanded = false },
                                                    modifier = Modifier.background(Color(0xFF1A2234))
                                                ) {
                                                    selectableYears.forEach { yr ->
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    text = yr.toString(),
                                                                    color = if (yr == selectedYearValue) Color(0xFFFBBF24) else Color(0xFFF8FAFC),
                                                                    fontSize = 12.sp
                                                                )
                                                            },
                                                            onClick = {
                                                                selectedYearValue = yr
                                                                yearDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            items(12) { mIdx ->
                                                val countForMonth = monthDayPatternMap[mIdx]?.size ?: 0
                                                val isActiveTab = activePatternMonthIdx == mIdx
                                                FilterChip(
                                                    selected = isActiveTab,
                                                    onClick = { activePatternMonthIdx = mIdx },
                                                    label = {
                                                        Text(
                                                            text = if (countForMonth > 0) "${monthsShort[mIdx]} ($countForMonth)" else monthsShort[mIdx],
                                                            fontSize = 11.sp
                                                        )
                                                    }
                                                )
                                            }
                                        }

                                        val daysInActiveMonth = remember(selectedYearValue, activePatternMonthIdx) {
                                            Calendar.getInstance().apply {
                                                set(Calendar.YEAR, selectedYearValue)
                                                set(Calendar.MONTH, activePatternMonthIdx)
                                            }.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        }
                                        val currentMonthDays = monthDayPatternMap[activePatternMonthIdx].orEmpty()

                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(7),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(156.dp)
                                        ) {
                                            items((1..daysInActiveMonth).toList()) { dayNum ->
                                                val checked = currentMonthDays.contains(dayNum)
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .aspectRatio(1.25f)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(
                                                            if (checked) Color(0xFF38BDF8) else Color(0xFF162032)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (checked) Color(0xFF38BDF8) else Color(0xFF28354D),
                                                            RoundedCornerShape(7.dp)
                                                        )
                                                        .clickable {
                                                            val nextSet = if (checked) {
                                                                currentMonthDays - dayNum
                                                            } else {
                                                                currentMonthDays + dayNum
                                                            }
                                                            monthDayPatternMap[activePatternMonthIdx] = nextSet
                                                        }
                                                ) {
                                                    Text(
                                                        text = dayNum.toString(),
                                                        fontSize = 11.sp,
                                                        fontWeight = if (checked) FontWeight.ExtraBold else FontWeight.Medium,
                                                        color = if (checked) Color(0xFF06131C) else Color(0xFFE2E8F0)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val template = monthDayPatternMap[activePatternMonthIdx].orEmpty()
                                                    for (m in 0..11) {
                                                        monthDayPatternMap[m] = template
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = "Copy ${monthsShort[activePatternMonthIdx]} to All Months",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF38BDF8),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    for (m in 0..11) {
                                                        monthDayPatternMap[m] = emptySet()
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "Clear Year",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFFF87171),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        val totalDaysAcrossYear = (0..11).sumOf { monthDayPatternMap[it]?.size ?: 0 }
                                        val activeMonthsCount = (0..11).count { !monthDayPatternMap[it].isNullOrEmpty() }
                                        Text(
                                            text = "$selectedYearValue Pattern: $totalDaysAcrossYear day(s) across $activeMonthsCount month(s) at $formattedPickedTime",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FormSectionCard(title = "SOUND & SNOOZE", accent = Color(0xFF34D399)) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { soundDropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isPreviewPlaying) "♪ $audioFileName (Playing)" else audioFileName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                color = if (isPreviewPlaying) Color(0xFF34D399) else Color(0xFFF8FAFC),
                                fontWeight = if (isPreviewPlaying) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("▼", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    DropdownMenu(
                        expanded = soundDropdownExpanded,
                        onDismissRequest = { soundDropdownExpanded = false },
                        modifier = Modifier.background(Color(0xFF1A2234))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Select from Device (MP3 / Audio)",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                soundDropdownExpanded = false
                                stopPreviewSound()
                                audioPickerLauncher.launch(arrayOf("audio/*"))
                            }
                        )

                        bundledRingtones.forEach { track ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = track.displayName,
                                        color = Color(0xFFF1F5F9)
                                    )
                                },
                                onClick = {
                                    soundDropdownExpanded = false
                                    ringtoneSource = "BUNDLED"
                                    selectedBundledRes = track.rawResName
                                    audioFileName = track.displayName
                                    playPreviewSound("BUNDLED", track.rawResName, audioUri)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "System Default Alarm",
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            onClick = {
                                soundDropdownExpanded = false
                                ringtoneSource = "SYSTEM"
                                audioFileName = "System Default Alarm"
                                playPreviewSound("SYSTEM", selectedBundledRes, audioUri)
                            }
                        )
                    }
                }

                if (isPreviewPlaying) {
                    Button(
                        onClick = { stopPreviewSound() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Text(
                            text = "■  STOP PREVIEW SOUND",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    snoozePresets.forEach { (label, mins) ->
                        FilterChip(
                            selected = selectedSnoozeMins == mins,
                            onClick = {
                                selectedSnoozeMins = mins
                                customSnoozeText = mins.toString()
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                    OutlinedTextField(
                        value = customSnoozeText,
                        onValueChange = { customSnoozeText = it },
                        label = { Text("Mins", fontSize = 10.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            FormSectionCard(title = "CHECKLIST & NOTES", accent = Color(0xFF38BDF8)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTodoText,
                        onValueChange = { newTodoText = it },
                        label = { Text("Add subtask", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newTodoText.isNotBlank()) {
                                todoList.add(TodoItem(text = newTodoText.trim()))
                                newTodoText = ""
                            }
                        }
                    ) {
                        Text("Add", fontSize = 12.sp)
                    }
                }

                todoList.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("• ${item.text}", color = Color(0xFFF1F5F9), fontSize = 12.sp)
                        TextButton(onClick = { todoList.remove(item) }) {
                            Text("Remove", color = Color(0xFFF87171), fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Optional notes...", fontSize = 11.sp) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (inactiveOtherTasks.isNotEmpty()) {
                Text(
                    text = "Inactive Alarms (Tap to expand)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp)
                )
                inactiveOtherTasks.forEach { inactive ->
                    val isExpanded = expandedInactiveInEditIds.contains(inactive.id)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF141B29)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .border(1.dp, Color(0xFF263248), RoundedCornerShape(12.dp))
                            .clickable {
                                expandedInactiveInEditIds = if (isExpanded) {
                                    expandedInactiveInEditIds - inactive.id
                                } else {
                                    expandedInactiveInEditIds + inactive.id
                                }
                            }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "${inactive.priority.label} • ${inactive.title}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = inactiveTimeFormatter.format(Date(inactive.triggerTimeMillis)),
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isExpanded) {
                                Text(
                                    text = "Snooze: ${inactive.snoozeMinutes}m • Ringtone: ${inactive.audioFileName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (inactive.notes.isNotBlank()) {
                                    Text(
                                        text = inactive.notes,
                                        fontSize = 11.sp,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { submitForm() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF06131C)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text("SAVE REMINDER", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}