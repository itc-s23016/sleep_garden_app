package com.example.sleep_garden.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.sleep_garden.R
import java.util.*

class AlarmRingtoneService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START_ALARM"
        const val ACTION_STOP = "ACTION_STOP_ALARM"
        const val ACTION_SNOOZE = "ACTION_SNOOZE_ALARM"
    }

    private var mediaPlayer: MediaPlayer? = null
    private var alarmId: String = "default"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                alarmId = intent.getStringExtra("alarmId") ?: "default"
                startForeground(alarmId.hashCode(), buildOngoingNotification())
                startSound()
            }
            ACTION_STOP -> {
                stopSound()
                stopSelf()
            }
            ACTION_SNOOZE -> {
                stopSound()
                // 1分後に同じアラームを再スケジュール
                scheduleAfterMinute(this, alarmId, minutes = 1)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildOngoingNotification(): Notification {
        val stopPi = PendingIntent.getService(
            this, ("stop$alarmId").hashCode(),
            Intent(this, AlarmRingtoneService::class.java).apply { action = ACTION_STOP; putExtra("alarmId", alarmId) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getService(
            this, ("snooze$alarmId").hashCode(),
            Intent(this, AlarmRingtoneService::class.java).apply { action = ACTION_SNOOZE; putExtra("alarmId", alarmId) },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("アラーム")
            .setContentText("鳴動中")
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .addAction(0, "停止", stopPi)
            .addAction(0, "スヌーズ", snoozePi)
            .build()
    }

    private fun startSound() {
        val uri: Uri = Settings.System.DEFAULT_ALARM_ALERT_URI
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(this@AlarmRingtoneService, uri)
            isLooping = true
            prepare()
            start()
        }
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
private fun scheduleAfterMinute(context: Context, alarmId: String, minutes: Int) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarmId", alarmId)
        action = "com.example.sleep_garden.ALARM_$alarmId"
    }
    val pi = PendingIntent.getBroadcast(
        context,
        kotlin.math.abs(alarmId.hashCode()),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val trigger = System.currentTimeMillis() + minutes * 60_000L
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
}
