package com.example.tatkshana

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TatkshanaApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var canExactAlarm by remember { mutableStateOf(true) }
    var canOverlay by remember { mutableStateOf(true) }

    fun refreshPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            canExactAlarm = alarmMgr.canScheduleExactAlarms()
        }
        canOverlay = Settings.canDrawOverlays(context)
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        refreshPermissions()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val tasks = remember {
        mutableStateListOf<ReminderTask>().apply {
            addAll(TaskStorage.loadTasks(context))
        }
    }

    fun persistAndSync(updatedList: List<ReminderTask>) {
        tasks.clear()
        tasks.addAll(updatedList)
        TaskStorage.saveTasks(context, updatedList)
    }

    var isFormOpen by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<ReminderTask?>(null) }

    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()) }

    if (isFormOpen) {
        ReminderFormScreen(
            existingTask = taskBeingEdited,
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
                            TatkshanaBrandBadge(sizeDp = 40)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tatkshana",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 21.sp
                                )
                                Text(
                                    text = "Priority Alarms & Task Proof",
                                    fontSize = 11.sp,
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
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "+ New Reminder",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 18.dp)
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
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .border(1.dp, Color(0xFFF87171).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Enable Background Permissions for Reliable Alarms:",
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
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
                            TatkshanaBrandBadge(sizeDp = 64)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No reminders yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFF1F5F9)
                            )
                            Text(
                                text = "Tap + New Reminder to set an alarm or hourly task",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        item { Spacer(modifier = Modifier.height(84.dp)) }
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
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = value,
                color = accent,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp
            )
            Text(
                text = label,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
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
    val priorityColor = when (task.priority) {
        PriorityLevel.NORMAL -> Color(0xFF34D399)
        PriorityLevel.MEDIUM -> Color(0xFF38BDF8)
        PriorityLevel.HIGH -> Color(0xFFFBBF24)
        PriorityLevel.CRITICAL -> Color(0xFFF87171)
    }

    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2234)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF334155).copy(alpha = 0.55f), RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = priorityColor.copy(alpha = 0.16f),
                    shape = CircleShape
                ) {
                    Text(
                        text = task.priority.label.uppercase(),
                        color = priorityColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = task.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF06131C),
                            checkedTrackColor = Color(0xFF38BDF8)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = task.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )

            val scheduleDescription = when (task.scheduleType) {
                ScheduleType.ONE_TIME -> "Once • ${formatter.format(Date(task.triggerTimeMillis))}"
                ScheduleType.HOURLY_RANGE -> "Every ${task.intervalHours} hr(s) for ${task.durationDays} days • Next: ${formatter.format(Date(task.triggerTimeMillis))}"
                ScheduleType.RECURRING_DAYS -> "Every ${task.recurringDays.joinToString(", ")} • Next: ${formatter.format(Date(task.triggerTimeMillis))}"
            }
            Text(
                text = scheduleDescription,
                color = Color(0xFF38BDF8),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )

            Text(
                text = "Stop Rule: ${task.priority.dismissalRule}",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Ringtone: ${task.audioFileName}  •  Snooze: ${task.snoozeMinutes}m",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
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
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F1623), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = task.notes,
                        color = Color(0xFFE2E8F0),
                        fontSize = 13.sp
                    )
                }
            }

            if (task.todos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Checklist (${task.todos.count { it.isDone }}/${task.todos.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFF1F5F9)
                )
                task.todos.forEach { todo ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTodo(todo.id) }
                            .padding(vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = todo.isDone,
                            onCheckedChange = { onToggleTodo(todo.id) }
                        )
                        Text(
                            text = todo.text,
                            color = if (todo.isDone) Color(0xFF64748B) else Color(0xFFF1F5F9),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onTestRingNow,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ring Now (Test Sound & Stop Gate)", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFormScreen(
    existingTask: ReminderTask?,
    onBack: () -> Unit,
    onSave: (ReminderTask) -> Unit
) {
    val context = LocalContext.current
    val bundledRingtones = remember(context) { BundledRingtones.getAvailableRingtones(context) }

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var selectedPriority by remember { mutableStateOf(existingTask?.priority ?: PriorityLevel.NORMAL) }
    var selectedSchedule by remember { mutableStateOf(existingTask?.scheduleType ?: ScheduleType.ONE_TIME) }

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

    var intervalHours by remember { mutableStateOf((existingTask?.intervalHours ?: 1).toString()) }
    var durationDays by remember { mutableStateOf((existingTask?.durationDays ?: 30).toString()) }
    var activeStartHour by remember { mutableStateOf((existingTask?.activeStartHour ?: 8).toString()) }
    var activeEndHour by remember { mutableStateOf((existingTask?.activeEndHour ?: 22).toString()) }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            addAll(existingTask?.recurringDays ?: listOf("Mon", "Wed", "Fri"))
        }
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
            ringtoneSource = "STORAGE"
            audioUri = uri.toString()
            var name = "Custom Storage Audio"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
            audioFileName = name
        }
    }

    val snoozePresets = listOf("5 mins" to 5, "15 mins" to 15, "1 hour" to 60, "Next Day" to 1440)
    var selectedSnoozeMins by remember { mutableIntStateOf(existingTask?.snoozeMinutes ?: 5) }
    var customSnoozeText by remember { mutableStateOf((existingTask?.snoozeMinutes ?: 5).toString()) }

    var newTodoText by remember { mutableStateOf("") }
    val todoList = remember {
        mutableStateListOf<TodoItem>().apply {
            addAll(existingTask?.todos ?: emptyList())
        }
    }

    fun submitForm() {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            Toast.makeText(context, "Please enter a Reminder Title first", Toast.LENGTH_SHORT).show()
            return
        }

        val cal = Calendar.getInstance().apply {
            set(pickedYear, pickedMonth, pickedDay, pickedHour, pickedMinute, 0)
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

        val daysCount = durationDays.toIntOrNull()?.coerceAtLeast(1) ?: 30
        val endMillis = System.currentTimeMillis() + daysCount * 86_400_000L
        val finalSnooze = customSnoozeText.toIntOrNull()?.coerceAtLeast(1) ?: selectedSnoozeMins

        val draft = ReminderTask(
            id = existingTask?.id ?: UUID.randomUUID().toString(),
            title = cleanTitle,
            notes = notes.trim(),
            priority = selectedPriority,
            scheduleType = selectedSchedule,
            triggerTimeMillis = targetMillis,
            intervalHours = intervalHours.toIntOrNull()?.coerceAtLeast(1) ?: 1,
            durationDays = daysCount,
            endDateMillis = endMillis,
            activeStartHour = activeStartHour.toIntOrNull()?.coerceIn(0, 23) ?: 8,
            activeEndHour = activeEndHour.toIntOrNull()?.coerceIn(1, 23) ?: 22,
            recurringDays = selectedDays.toList(),
            hourOfDay = pickedHour,
            minuteOfHour = pickedMinute,
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
                        TatkshanaBrandBadge(sizeDp = 32)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (existingTask == null) "New Reminder" else "Edit Reminder",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
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
                        Text("SAVE", fontWeight = FontWeight.ExtraBold)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Reminder Title *") },
                placeholder = { Text("e.g., Drink Water / Server Check / Exam Prep") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("1. Priority & Stop Rule", fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
            PriorityLevel.entries.forEach { level ->
                FilterChip(
                    selected = selectedPriority == level,
                    onClick = { selectedPriority = level },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF38BDF8).copy(alpha = 0.22f),
                        selectedLabelColor = Color(0xFFF8FAFC)
                    ),
                    label = {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(level.label, fontWeight = FontWeight.Bold)
                            Text(level.dismissalRule, fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("2. Schedule Flexibility", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            ScheduleType.entries.forEach { type ->
                FilterChip(
                    selected = selectedSchedule == type,
                    onClick = { selectedSchedule = type },
                    label = { Text(type.label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (selectedSchedule) {
                ScheduleType.ONE_TIME -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Date: $pickedDay/${pickedMonth + 1}/$pickedYear")
                        }

                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        pickedHour = h
                                        pickedMinute = min
                                    },
                                    pickedHour,
                                    pickedMinute,
                                    false
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(String.format(Locale.getDefault(), "Time: %02d:%02d", pickedHour, pickedMinute))
                        }
                    }
                }

                ScheduleType.HOURLY_RANGE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = intervalHours,
                            onValueChange = { intervalHours = it },
                            label = { Text("Every N Hours") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it },
                            label = { Text("For N Days") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = activeStartHour,
                            onValueChange = { activeStartHour = it },
                            label = { Text("Start Hour (0-23)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = activeEndHour,
                            onValueChange = { activeEndHour = it },
                            label = { Text("End Hour (1-23)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ScheduleType.RECURRING_DAYS -> {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allDays) { day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedDays.remove(day) else selectedDays.add(day)
                                },
                                label = { Text(day) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, min ->
                                    pickedHour = h
                                    pickedMinute = min
                                },
                                pickedHour,
                                pickedMinute,
                                false
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(String.format(Locale.getDefault(), "Alarm Time: %02d:%02d", pickedHour, pickedMinute))
                    }
                }
            }

            // 3. Sound Track Dropdown ("Select from Device" first + Built-in Ringtone Tracks)
            Text(
                text = "3. Sound Track (Dropdown)",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399)
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { soundDropdownExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = audioFileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("▼", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                    }
                }

                DropdownMenu(
                    expanded = soundDropdownExpanded,
                    onDismissRequest = { soundDropdownExpanded = false },
                    modifier = Modifier.background(Color(0xFF1A2234))
                ) {
                    // First option: Select from Device
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
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        }
                    )

                    // Built-in ringtone tracks from res/raw
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
                        }
                    )
                }
            }

            // 4. Snooze Options
            Text("4. Snooze Interval", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snoozePresets) { (label, mins) ->
                    FilterChip(
                        selected = selectedSnoozeMins == mins,
                        onClick = {
                            selectedSnoozeMins = mins
                            customSnoozeText = mins.toString()
                        },
                        label = { Text(label) }
                    )
                }
            }
            OutlinedTextField(
                value = customSnoozeText,
                onValueChange = { customSnoozeText = it },
                label = { Text("Custom Snooze Minutes") },
                modifier = Modifier.fillMaxWidth()
            )

            // 5. To-Do Checklist
            Text("5. Multi-Item To-Do Checklist", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newTodoText,
                    onValueChange = { newTodoText = it },
                    label = { Text("Add subtask") },
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
                    Text("Add")
                }
            }
            todoList.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("• ${item.text}", color = Color(0xFFF1F5F9))
                    TextButton(onClick = { todoList.remove(item) }) {
                        Text("Remove", color = Color(0xFFF87171))
                    }
                }
            }

            // 6. Heavy Detailed Notes
            Text("6. Heavy Detailed Notes", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Write detailed instructions or notes shown when alarm rings...") },
                minLines = 4,
                maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )

            // Dedicated Bottom Save Button for effortless saving after filling details
            Button(
                onClick = { submitForm() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFF06131C)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("SAVE REMINDER", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}