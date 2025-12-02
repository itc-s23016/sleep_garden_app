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
import kotlin.math.abs

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
                startForeground(alarmId.hashCode(), buildAlarmNotification())
                startSound()
            }
            ACTION_STOP -> stopSelf()
            ACTION_SNOOZE -> {
                scheduleAfterMinute(this, alarmId, 1)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun buildAlarmNotification(): Notification {
        val full = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarmId", alarmId)
        }

        val contentPi = PendingIntent.getActivity(
            this,
            ("f_$alarmId").hashCode(),
            full,
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
            .setFullScreenIntent(contentPi, true)
            .build()
    }

    private fun startSound() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus({}, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)

        val sources = listOf(
            Settings.System.DEFAULT_ALARM_ALERT_URI,
            Settings.System.DEFAULT_RINGTONE_URI,
            Settings.System.DEFAULT_NOTIFICATION_URI
        )

        for (src in sources) {
            try {
                mediaPlayer?.stop()
                mediaPlayer = MediaPlayer().apply {
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    setDataSource(this@AlarmRingtoneService, src)
                    prepare()
                    start()
                }
                return
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

@SuppressLint("ScheduleExactAlarm")
private fun scheduleAfterMinute(context: Context, alarmId: String, minutes: Int) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("alarmId", alarmId)
        action = "com.example.sleep.ALARM_$alarmId"
    }

    val firePi = PendingIntent.getBroadcast(
        context,
        abs(alarmId.hashCode()),
        fireIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val trigger = System.currentTimeMillis() + minutes * 60000L
    am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, null), firePi)
}
