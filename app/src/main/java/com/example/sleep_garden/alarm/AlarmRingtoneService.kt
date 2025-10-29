package com.example.sleep_garden.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import com.example.sleep_garden.MainActivity
import kotlin.math.abs

class AlarmRingtoneService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_ALARM"
        const val ACTION_STOP = "ACTION_STOP_ALARM"      // ← 他画面から停止したい場合用に残す
        const val ACTION_SNOOZE = "ACTION_SNOOZE_ALARM"  // ← 他画面からスヌーズしたい場合用に残す
    }

    private var mediaPlayer: MediaPlayer? = null
    private var alarmId: String = "default"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                alarmId = intent.getStringExtra("alarmId") ?: "default"
                startForeground(alarmId.hashCode(), buildAlarmNotification(alarmId))
                startSound()
                return START_STICKY
            }
            ACTION_STOP -> {
                stopSound()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                stopSound()
                scheduleAfterMinute(this, alarmId, minutes = 1)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> return START_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    /** 通常通知：タップで AlarmActivity、ロック中は全画面。停止/スヌーズのボタンは置かない */
    private fun buildAlarmNotification(alarmId: String): Notification {
        val alarmActivityIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarmId", alarmId)
        }
        val contentPi = PendingIntent.getActivity(
            this,
            ("content_$alarmId").hashCode(),
            alarmActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenPi = PendingIntent.getActivity(
            this,
            ("fullscreen_$alarmId").hashCode(),
            Intent(alarmActivityIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("アラーム")
            .setContentText("アラームが鳴っています")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(contentPi)             // 通知タップで AlarmActivity
            .setFullScreenIntent(fullScreenPi, true) // ロック画面では全画面
            // .addAction(...) は置かない（停止/スヌーズボタン削除）
            .build()
    }

    private fun startSound() {
        val TAG = "AlarmRingtoneService"
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(
            AudioManager.OnAudioFocusChangeListener { },
            AudioManager.STREAM_ALARM,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )

        val candidates: List<Uri?> = listOf(
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            Settings.System.DEFAULT_RINGTONE_URI,
            Settings.System.DEFAULT_NOTIFICATION_URI
        )

        var lastError: Exception? = null
        for (src in candidates) {
            try {
                if (src == null) continue
                stopSound()
                mediaPlayer = MediaPlayer().apply {
                    setWakeMode(this@AlarmRingtoneService, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    setDataSource(this@AlarmRingtoneService, src)
                    prepare()
                    start()
                }
                Log.i(TAG, "Alarm started with URI: $src")
                return
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Failed with $src, trying next...", e)
            }
        }
        Log.e(TAG, "All alarm sources failed.", lastError)
    }

    private fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/* 1分後スヌーズ */
@SuppressLint("ScheduleExactAlarm")
private fun scheduleAfterMinute(context: Context, alarmId: String, minutes: Int) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarmId", alarmId)
        action = "com.example.sleep_garden.ALARM_$alarmId"
    }
    val firePi = PendingIntent.getBroadcast(
        context,
        abs(alarmId.hashCode()),
        fireIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val showIntent = Intent(context, com.example.sleep_garden.alarm.AlarmActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("alarmId", alarmId)
    }
    val showPi = PendingIntent.getActivity(
        context,
        ("show_$alarmId").hashCode(),
        showIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val trigger = System.currentTimeMillis() + minutes * 60_000L
    val info = AlarmManager.AlarmClockInfo(trigger, showPi)
    am.setAlarmClock(info, firePi)
}
