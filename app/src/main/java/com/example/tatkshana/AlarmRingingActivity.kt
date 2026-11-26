package com.roopesh.tatkshana

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.roopesh.tatkshana.ui.theme.TatkshanaTheme
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class AlarmRingingActivity : ComponentActivity() {

    private var isScreenOffReceiverRegistered = false
    private var activeTaskId: String? = null

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                muteAlarmSoundKeepVibration()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentInstance = WeakReference(this)
        enableShowWhenLockedAndTurnScreenOn()

        val taskId = intent.getStringExtra("TASK_ID")
        activeTaskId = taskId
        val focusSnooze = intent.getBooleanExtra("FOCUS_SNOOZE", false)
        val task = taskId?.let { TaskStorage.getTaskById(this, it) }

        if (task == null) {
            stopAlarmService()
            closeSelfCompletely()
            return
        }

        registerScreenOffReceiver()
        AlarmLifecycleController.setRingingUiVisible(this, true, task.id)

        setContent {
            TatkshanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingScreen(
                        task = task,
                        focusSnooze = focusSnooze,
                        onSnooze = { minutes ->
                            AlarmLifecycleController.setRingingUiVisible(this, false, task.id)
                            stopAlarmService()
                            closeSelfCompletely()
                            AlarmScheduler.snoozeTask(this, task, minutes)
                        },
                        onCompleteAndStop = { updatedTodos, grade ->
                            AlarmLifecycleController.setRingingUiVisible(this, false, task.id)
                            stopAlarmService()
                            val nextTime = if (task.scheduleType == ScheduleType.ONE_TIME) {
                                null
                            } else {
                                AlarmScheduler.computeNextTriggerMillis(
                                    task,
                                    System.currentTimeMillis() + 5_000L
                                )
                            }
                            if (nextTime == null) {
                                AlarmScheduler.cancelTask(this, task.id)
                            }
                            val updatedTask = task.copy(
                                todos = updatedTodos,
                                lastGrade = grade,
                                isEnabled = nextTime != null,
                                triggerTimeMillis = nextTime ?: task.triggerTimeMillis
                            )
                            TaskStorage.updateTask(this, updatedTask)
                            if (nextTime != null) {
                                AlarmScheduler.scheduleTask(this, updatedTask, nextTime)
                            }
                            closeSelfCompletely()
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AlarmLifecycleController.setRingingUiVisible(this, true, activeTaskId)
    }

    override fun onPause() {
        AlarmLifecycleController.setRingingUiVisible(this, false, activeTaskId)
        super.onPause()
    }

    private fun closeSelfCompletely() {
        try {
            finishAndRemoveTask()
        } catch (_: Exception) {
            finish()
        }
    }

    private fun enableShowWhenLockedAndTurnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun registerScreenOffReceiver() {
        if (isScreenOffReceiverRegistered) return
        try {
            ContextCompat.registerReceiver(
                this,
                screenOffReceiver,
                IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            isScreenOffReceiverRegistered = true
        } catch (_: Exception) {
        }
    }

    private fun unregisterScreenOffReceiver() {
        if (!isScreenOffReceiverRegistered) return
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (_: Exception) {
        }
        isScreenOffReceiverRegistered = false
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                muteAlarmSoundKeepVibration()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            muteAlarmSoundKeepVibration()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun muteAlarmSoundKeepVibration() {
        val muteIntent = Intent(this, AlarmService::class.java).apply {
            action = "MUTE_SOUND_KEEP_VIBRATION"
            activeTaskId?.let { putExtra("TASK_ID", it) }
        }
        try {
            startService(muteIntent)
        } catch (_: Exception) {
        }
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        try {
            startService(stopIntent)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        if (currentInstance?.get() === this) {
            currentInstance = null
        }
        AlarmLifecycleController.setRingingUiVisible(this, false, activeTaskId)
        unregisterScreenOffReceiver()
        super.onDestroy()
    }

    companion object {
        private var currentInstance: WeakReference<AlarmRingingActivity>? = null
        private val mainHandler = Handler(Looper.getMainLooper())

        fun closeRingingScreen() {
            mainHandler.post {
                currentInstance?.get()?.let { activity ->
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        activity.closeSelfCompletely()
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmRingingScreen(
    task: ReminderTask,
    focusSnooze: Boolean = false,
    onSnooze: (Int) -> Unit,
    onCompleteAndStop: (List<TodoItem>, String?) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val todos = remember { mutableStateListOf(*task.todos.toTypedArray()) }

    LaunchedEffect(focusSnooze) {
        if (focusSnooze) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val priorityAccent = when (task.priority) {
        PriorityLevel.NORMAL -> Color(0xFF34D399)
        PriorityLevel.MEDIUM -> Color(0xFF38BDF8)
        PriorityLevel.HIGH -> Color(0xFFFBBF24)
        PriorityLevel.CRITICAL -> Color(0xFFF87171)
    }

    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val numA = remember { Random.nextInt(12, 39) }
    val numB = remember { Random.nextInt(11, 29) }
    var mathAnswer by remember { mutableStateOf("") }

    var proofBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedGrade by remember { mutableStateOf("A - Completed Fully") }
    val grades = listOf("A - Completed Fully", "B - Done with Minor Delay", "C - Partial Progress")

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            proofBitmap = bitmap
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    val allTodosDone = todos.all { it.isDone }
    val canDismiss = when (task.priority) {
        PriorityLevel.NORMAL -> true
        PriorityLevel.MEDIUM -> allTodosDone
        PriorityLevel.HIGH -> allTodosDone && (mathAnswer.trim().toIntOrNull() == numA + numB)
        PriorityLevel.CRITICAL -> allTodosDone && proofBitmap != null
    }

    val snoozePresets = listOf(
        "5 mins" to 5,
        "15 mins" to 15,
        "1 hour" to 60,
        "Next Day" to 1440
    )
    var customSnoozeInput by remember { mutableStateOf(task.snoozeMinutes.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF090D16), Color(0xFF111827), Color(0xFF0B0F17))
                )
            )
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TatkshanaBrandBadge(sizeDp = 36)
                Spacer(modifier = Modifier.width(10.dp))
                Surface(
                    color = priorityAccent.copy(alpha = 0.16f),
                    shape = CircleShape,
                    modifier = Modifier.border(1.dp, priorityAccent.copy(alpha = 0.45f), CircleShape)
                ) {
                    Text(
                        text = task.priority.label.uppercase(),
                        color = priorityAccent,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = timeFormatter.format(Date()),
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161F31)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, priorityAccent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "ACTIVE ALARM",
                    color = priorityAccent,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFF8FAFC)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stop Rule: ${task.priority.dismissalRule}",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = if (task.priority == PriorityLevel.NORMAL || task.priority == PriorityLevel.MEDIUM) {
                        "Tip: Press Volume or Power button to mute sound (vibration stops at 30s)"
                    } else {
                        "Tip: Press Volume or Power button to silence sound (vibration stays active)"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (task.notes.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF283548), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TASK NOTES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = task.notes,
                        fontSize = 15.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }

        if (todos.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF283548), RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CHECKLIST (${todos.count { it.isDone }}/${todos.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color(0xFFF8FAFC)
                    )
                    todos.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    todos[index] = item.copy(isDone = !item.isDone)
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { checked ->
                                    todos[index] = item.copy(isDone = checked)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF34D399),
                                    uncheckedColor = Color(0xFF64748B)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.text,
                                fontSize = 15.sp,
                                color = if (item.isDone) Color(0xFF64748B) else Color(0xFFF1F5F9)
                            )
                        }
                    }
                }
            }
        }

        when (task.priority) {
            PriorityLevel.HIGH -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1A11)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "HIGH PRIORITY VERIFICATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "Solve $numA + $numB to unlock dismiss",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        OutlinedTextField(
                            value = mathAnswer,
                            onValueChange = { mathAnswer = it },
                            label = { Text("Enter Answer") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            PriorityLevel.CRITICAL -> {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF211216)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFF87171).copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "CRITICAL PROOF & GRADING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFF87171)
                        )

                        Button(
                            onClick = {
                                val hasCam = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasCam) {
                                    cameraLauncher.launch(null)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF38BDF8),
                                contentColor = Color(0xFF06131C)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (proofBitmap == null) "Capture Proof Photo" else "Retake Proof Photo",
                                fontWeight = FontWeight.Bold
                            )
                        }

                        proofBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Proof Photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                        }

                        Text(
                            text = "Grade Your Task Completion",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFFF1F5F9)
                        )
                        grades.forEach { grade ->
                            FilterChip(
                                selected = selectedGrade == grade,
                                onClick = { selectedGrade = grade },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFF87171).copy(alpha = 0.22f),
                                    selectedLabelColor = Color(0xFFF8FAFC)
                                ),
                                label = { Text(grade) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            else -> Unit
        }

        Button(
            onClick = {
                val finalGrade = if (task.priority == PriorityLevel.CRITICAL) selectedGrade else null
                onCompleteAndStop(todos.toList(), finalGrade)
            },
            enabled = canDismiss,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF22C55E),
                contentColor = Color(0xFF052E16),
                disabledContainerColor = Color(0xFF1E293B),
                disabledContentColor = Color(0xFF64748B)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
        ) {
            Text(
                text = if (canDismiss) "STOP ALARM & MARK COMPLETE" else "COMPLETE REQUIREMENTS ABOVE TO STOP",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141C2B)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF283548), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SNOOZE ALARM",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(snoozePresets) { (label, mins) ->
                        OutlinedButton(
                            onClick = { onSnooze(mins) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(label, color = Color(0xFFF1F5F9))
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customSnoozeInput,
                        onValueChange = { customSnoozeInput = it },
                        label = { Text("Custom Mins") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = {
                            val mins = customSnoozeInput.toIntOrNull()?.coerceAtLeast(1) ?: 5
                            onSnooze(mins)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Snooze Custom", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}