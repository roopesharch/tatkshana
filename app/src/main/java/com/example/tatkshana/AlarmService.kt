package com.roopesh.tatkshana

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var activePriority: PriorityLevel = PriorityLevel.NORMAL
    private var activeTaskId: String? = null
    private var soundSilencedManually: Boolean = false

    private val timeoutHandler = Handler(Looper.getMainLooper())

    private val stopSoundAfter30sRunnable = Runnable {
        if (isNormalOrMedium(activePriority) && !soundSilencedManually) {
            silenceAlarmSound()
        }
    }

    private val autoExpireNormalMediumAfter1mRunnable = Runnable {
        if (isNormalOrMedium(activePriority)) {
            val taskId = activeTaskId
            stopAlarmPlayback()
            if (!taskId.isNullOrBlank()) {
                AlarmLifecycleController.completeFromPopupSilence(this, taskId)
            }
            AlarmRingingActivity.closeRingingScreen()
            stopSelf()
        }
    }

    private val stopHighCriticalVibrationAfter7mRunnable = Runnable {
        if (!isNormalOrMedium(activePriority)) {
            stopVibration()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {

            "STOP_ALARM" -> {
                stopAlarmPlayback()
                AlarmRingingActivity.closeRingingScreen()
                stopSelf()
                return START_NOT_STICKY
            }

            "SILENCE_FROM_POPUP" -> {
                val taskId = intent.getStringExtra("TASK_ID") ?: activeTaskId
                val task = taskId?.let { TaskStorage.getTaskById(this, it) }
                val priority = task?.priority ?: activePriority

                if (isNormalOrMedium(priority)) {
                    stopAlarmPlayback()
                    if (!taskId.isNullOrBlank()) {
                        AlarmLifecycleController.completeFromPopupSilence(this, taskId)
                    }
                    AlarmRingingActivity.closeRingingScreen()
                    try {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                            )
                        }
                        startActivity(mainIntent)
                    } catch (_: Exception) {
                    }
                    stopSelf()
                    return START_NOT_STICKY
                } else {
                    soundSilencedManually = true
                    silenceAlarmSound()
                    ensureVibrationRunning()
                    scheduleHighCritical7mVibrationCap()
                    if (!taskId.isNullOrBlank()) {
                        val ringIntent = Intent(this, AlarmRingingActivity::class.java).apply {
                            putExtra("TASK_ID", taskId)
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            )
                        }
                        try {
                            startActivity(ringIntent)
                        } catch (_: Exception) {
                        }
                    }
                    return START_STICKY
                }
            }

            "MUTE_SOUND_KEEP_VIBRATION" -> {
                soundSilencedManually = true
                silenceAlarmSound()

                if (isNormalOrMedium(activePriority)) {
                    ensureVibrationRunning()
                } else {
                    ensureVibrationRunning()
                    scheduleHighCritical7mVibrationCap()
                }

                return START_STICKY
            }
        }

        val taskId = intent?.getStringExtra("TASK_ID")
            ?: return START_NOT_STICKY

        val task = TaskStorage.getTaskById(this, taskId)
            ?: return START_NOT_STICKY

        createNotificationChannel()

        val fullScreenIntent = Intent(
            this,
            AlarmRingingActivity::class.java
        ).apply {
            putExtra("TASK_ID", taskId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePageIntent = Intent(
            this,
            AlarmRingingActivity::class.java
        ).apply {
            putExtra("TASK_ID", taskId)
            putExtra("FOCUS_SNOOZE", true)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }

        val snoozePagePendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode() + 2,
            snoozePageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silenceIntent = Intent(
            this,
            AlarmService::class.java
        ).apply {
            action = "SILENCE_FROM_POPUP"
            putExtra("TASK_ID", taskId)
        }

        val silencePendingIntent = PendingIntent.getService(
            this,
            taskId.hashCode() + 1,
            silenceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val silenceActionLabel = if (isNormalOrMedium(task.priority)) {
            "Silence (Turn Off)"
        } else {
            "Silence & Open"
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Tatkshana • ${task.priority.label}")
            .setContentText("${task.title} — Tap to open alarm")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_lock_idle_alarm,
                "Snooze",
                snoozePagePendingIntent
            )
            .addAction(
                android.R.drawable.ic_lock_silent_mode,
                silenceActionLabel,
                silencePendingIntent
            )
            .build()

        startForeground(1001, notification)

        startAlarmPlayback(task)

        if (Settings.canDrawOverlays(this)) {
            try {
                startActivity(fullScreenIntent)
            } catch (_: Exception) {
            }
        }

        return START_STICKY
    }

    private fun isNormalOrMedium(priority: PriorityLevel): Boolean {
        return priority == PriorityLevel.NORMAL ||
                priority == PriorityLevel.MEDIUM
    }

    private fun clearTimers() {
        timeoutHandler.removeCallbacks(stopSoundAfter30sRunnable)
        timeoutHandler.removeCallbacks(autoExpireNormalMediumAfter1mRunnable)
        timeoutHandler.removeCallbacks(stopHighCriticalVibrationAfter7mRunnable)
    }

    private fun scheduleHighCritical7mVibrationCap() {
        timeoutHandler.removeCallbacks(stopHighCriticalVibrationAfter7mRunnable)
        timeoutHandler.postDelayed(
            stopHighCriticalVibrationAfter7mRunnable,
            SEVEN_MINUTES_MILLIS
        )
    }

    private fun startAlarmPlayback(task: ReminderTask) {
        stopAlarmPlayback()

        activePriority = task.priority
        activeTaskId = task.id
        soundSilencedManually = false

        val defaultUri = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_ALARM
        ) ?: RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_RINGTONE
        ) ?: RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION
        )

        val bundledUri = BundledRingtones.resolveUri(
            this,
            task.bundledResName
        )

        val targetUri: Uri = when (task.ringtoneSource) {
            "STORAGE" -> {
                if (!task.audioUri.isNullOrBlank()) {
                    Uri.parse(task.audioUri)
                } else {
                    bundledUri ?: defaultUri
                }
            }

            "BUNDLED" -> bundledUri ?: defaultUri

            else -> defaultUri
        }

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                setDataSource(this@AlarmService, targetUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )

                    setDataSource(this@AlarmService, defaultUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (_: Exception) {
            }
        }

        ensureVibrationRunning()

        if (isNormalOrMedium(activePriority)) {
            timeoutHandler.postDelayed(
                stopSoundAfter30sRunnable,
                THIRTY_SECONDS_MILLIS
            )
            timeoutHandler.postDelayed(
                autoExpireNormalMediumAfter1mRunnable,
                ONE_MINUTE_MILLIS
            )
        }
    }

    private fun ensureVibrationRunning() {
        if (vibrator == null) {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(
                    Context.VIBRATOR_MANAGER_SERVICE
                ) as VibratorManager

                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        }

        val pattern = longArrayOf(0, 700, 500)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (_: Exception) {
        }
    }

    private fun silenceAlarmSound() {
        timeoutHandler.removeCallbacks(stopSoundAfter30sRunnable)

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {
        }

        mediaPlayer = null
    }

    private fun stopVibration() {
        timeoutHandler.removeCallbacks(stopHighCriticalVibrationAfter7mRunnable)

        try {
            vibrator?.cancel()
        } catch (_: Exception) {
        }

        vibrator = null
    }

    private fun stopAlarmPlayback() {
        clearTimers()
        silenceAlarmSound()
        stopVibration()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Tatkshana Priority Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen priority alarms"
                setSound(null, null)
                enableVibration(true)
                lockscreenVisibility =
                    android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAlarmPlayback()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "tatkshana_alarm_channel_v2"
        private const val THIRTY_SECONDS_MILLIS = 30_000L
        private const val ONE_MINUTE_MILLIS = 60_000L
        private const val SEVEN_MINUTES_MILLIS = 7 * 60_000L
    }
}