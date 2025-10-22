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
                // 通知はサービス側で1枚に統一（contentIntent + fullScreenIntent 付き）
                startForeground(alarmId.hashCode(), buildAlarmNotification(alarmId))
                startSound()
                // ★ 起動の安定性を優先（Activity表示などのタイミングでも止まらない）
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
            else -> {
                // 万一 action が null の再起動でも継続
                return START_STICKY
            }
        }
    }

    /** ユーザーが最近タスクからアプリをスワイプしても勝手に止めない */
    override fun onTaskRemoved(rootIntent: Intent?) {
        // 何もしない：前景通知 + START_STICKY で鳴動維持
        super.onTaskRemoved(rootIntent)
    }

    /** 通知（タップで AlarmActivity、画面OFFでも前面化） */
    private fun buildAlarmNotification(alarmId: String): Notification {
        val activityIntent = Intent(this, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarmId", alarmId)
        }
        val contentPi = PendingIntent.getActivity(
            this,
            ("content_$alarmId").hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreenPi = PendingIntent.getActivity(
            this,
            ("fullscreen_$alarmId").hashCode(),
            Intent(activityIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPi = PendingIntent.getService(
            this, ("stop_$alarmId").hashCode(),
            Intent(this, AlarmRingtoneService::class.java).apply {
                action = ACTION_STOP
                putExtra("alarmId", alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozePi = PendingIntent.getService(
            this, ("snooze_$alarmId").hashCode(),
            Intent(this, AlarmRingtoneService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra("alarmId", alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "alarm_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("アラーム")
            .setContentText("アラームが鳴っています")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(VISIBILITY_PUBLIC)
            .setOngoing(true)              // ← ユーザーが消せない
            .setAutoCancel(false)          // ← タップしても自動で消えない
            .setContentIntent(contentPi)   // ← 通知タップで AlarmActivity
            .setFullScreenIntent(fullScreenPi, true) // ← ロック画面でも前面化
            .addAction(0, "停止", snoozeOr(stopPi = stopPi))
            .addAction(0, "スヌーズ", snoozePi)
            .build()
    }

    // 一部機種で同一requestCodeの競合を避けるだけの小技（なくてもOK）
    private fun snoozeOr(stopPi: PendingIntent) = stopPi

    private fun startSound() {
        val TAG = "AlarmRingtoneService"

        // 1) AudioFocus を取る
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val focusResult = am.requestAudioFocus(
            AudioManager.OnAudioFocusChangeListener { /* アラームなので無視でOK */ },
            AudioManager.STREAM_ALARM,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "AudioFocus not granted. Trying to play anyway.")
        }

        // 2) 再生候補（上から順に試す）
        val candidates: List<Uri?> = listOf(
            Settings.System.DEFAULT_ALARM_ALERT_URI,                                 // 通常のアラーム音
            Settings.System.DEFAULT_RINGTONE_URI,                                    // 着信音で代用
            Settings.System.DEFAULT_NOTIFICATION_URI                                 // 通知音で代用
            // ※ raw リソースがあればここに Uri.parse("android.resource://$packageName/${R.raw.xxx}") を追加
        )

        var lastError: Exception? = null
        for (source in candidates) {
            try {
                if (source == null) continue

                // 既存プレイヤーを念のため破棄
                stopSound()

                mediaPlayer = MediaPlayer().apply {
                    // CPU維持（Manifest に WAKE_LOCK あり）
                    setWakeMode(this@AlarmRingtoneService, android.os.PowerManager.PARTIAL_WAKE_LOCK)

                    setAudioStreamType(AudioManager.STREAM_ALARM) // ALARM ストリームに固定
                    isLooping = true
                    setDataSource(this@AlarmRingtoneService, source)
                    prepare()
                    start()
                }
                Log.i(TAG, "Alarm started with URI: $source")
                return // ← 成功したら終了
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Failed to play with $source, trying next...", e)
            }
        }

        // 3) すべて失敗した場合でもサービスを落とさない（無音で継続）
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

    val showIntent = Intent(context, AlarmActivity::class.java).apply {
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
