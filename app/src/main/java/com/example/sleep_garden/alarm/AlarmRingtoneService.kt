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
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import androidx.core.app.TaskStackBuilder
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
        // 起動先（アラーム画面）
        val fullIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarmId", alarmId)
        }

        // ✅ バックスタック付きの contentIntent を作成（タップで確実に Activity へ遷移）
        val contentPi = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(fullIntent)
            .getPendingIntent(
                ("open_$alarmId").hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        // 停止／スヌーズ アクション
        val stopIntent = Intent(this, AlarmRingtoneService::class.java).apply {
            action = ACTION_STOP
            putExtra("alarmId", alarmId)
        }
        val stopPi = PendingIntent.getService(
            this, ("stop_$alarmId").hashCode(), stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(this, AlarmRingtoneService::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("alarmId", alarmId)
        }
        val snoozePi = PendingIntent.getService(
            this, ("snooze_$alarmId").hashCode(), snoozeIntent,
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
            // ✅ タップ時に開く PendingIntent を必ず設定
            .setContentIntent(contentPi) // FIX: これが無いとタップで開かない
            // Heads-up/ロック画面での全画面表示も維持
            .setFullScreenIntent(contentPi, true)
            // 操作ボタン（任意）
            .addAction(0, "停止", stopPi)
            .addAction(0, "スヌーズ", snoozePi)
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
        // ✅ 他の箇所と action を統一
        action = "com.example.sleep_garden.ALARM_$alarmId"
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
