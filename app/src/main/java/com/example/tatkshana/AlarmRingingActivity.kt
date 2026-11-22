package com.example.tatkshana

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.tatkshana.ui.theme.TatkshanaTheme
import kotlin.random.Random

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getStringExtra("TASK_ID")
        val task = taskId?.let { TaskStorage.getTaskById(this, it) }

        if (task == null) {
            stopAlarmService()
            finish()
            return
        }

        setContent {
            TatkshanaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AlarmRingingScreen(
                        task = task,
                        onSnooze = { minutes ->
                            stopAlarmService()
                            AlarmScheduler.snoozeTask(this, task, minutes)
                            finish()
                        },
                        onCompleteAndStop = { updatedTodos, grade ->
                            stopAlarmService()
                            val nextTime = AlarmScheduler.computeNextTriggerMillis(
                                task,
                                System.currentTimeMillis() + 5_000L
                            )
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
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun stopAlarmService() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = "STOP_ALARM"
        }
        startService(stopIntent)
    }
}

@Composable
fun AlarmRingingScreen(
    task: ReminderTask,
    onSnooze: (Int) -> Unit,
    onCompleteAndStop: (List<TodoItem>, String?) -> Unit
) {
    val context = LocalContext.current
    val todos = remember { mutableStateListOf(*task.todos.toTypedArray()) }

    // Challenge for HIGH priority
    val numA = remember { Random.nextInt(12, 39) }
    val numB = remember { Random.nextInt(11, 29) }
    var mathAnswer by remember { mutableStateOf("") }

    // Photo proof & grading for CRITICAL priority
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = Color(0xFFC62828),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "TATKSHANA • ${task.priority.label.uppercase()}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        Text(
            text = task.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        // Heavy Detailed Notes Display
        if (task.notes.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text("TASK NOTES", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(task.notes, fontSize = 15.sp)
                }
            }
        }

        // Interactive To-Do Checklist
        if (todos.isNotEmpty()) {
            Text("Complete Checklist Items:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            todos.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            todos[index] = item.copy(isDone = !item.isDone)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = item.isDone,
                        onCheckedChange = { checked ->
                            todos[index] = item.copy(isDone = checked)
                        }
                    )
                    Text(text = item.text, fontSize = 16.sp)
                }
            }
        }

        // Priority Enforcement Gates
        when (task.priority) {
            PriorityLevel.HIGH -> {
                Text(
                    text = "High Priority Verification: Solve $numA + $numB",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF6C00)
                )
                OutlinedTextField(
                    value = mathAnswer,
                    onValueChange = { mathAnswer = it },
                    label = { Text("Enter Answer") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            PriorityLevel.CRITICAL -> {
                Text(
                    text = "Critical Task: Upload Live Photo Proof & Grade Completion",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (proofBitmap == null) "Capture Proof Photo" else "Retake Proof Photo")
                }

                proofBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Proof Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                Text("Grade Your Task Completion:", fontWeight = FontWeight.SemiBold)
                grades.forEach { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade },
                        label = { Text(grade) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            else -> Unit
        }

        // Dismiss Button (Locked until priority requirement met)
        Button(
            onClick = {
                val finalGrade = if (task.priority == PriorityLevel.CRITICAL) selectedGrade else null
                onCompleteAndStop(todos.toList(), finalGrade)
            },
            enabled = canDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text(
                text = if (canDismiss) "STOP ALARM & MARK COMPLETE" else "COMPLETE REQUIREMENTS ABOVE TO STOP",
                fontWeight = FontWeight.Bold
            )
        }

        // Flexible Snooze Controls
        Text("Snooze Alarm:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(snoozePresets) { (label, mins) ->
                OutlinedButton(onClick = { onSnooze(mins) }) {
                    Text(label)
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
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    val mins = customSnoozeInput.toIntOrNull()?.coerceAtLeast(1) ?: 5
                    onSnooze(mins)
                }
            ) {
                Text("Snooze Custom")
            }
        }
    }
}