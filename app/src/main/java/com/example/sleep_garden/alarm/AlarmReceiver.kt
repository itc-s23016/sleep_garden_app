package com.example.sleep_garden.alarm

import android.Manifest
import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.sleep_garden.MainActivity

class AlarmReceiver : BroadcastReceiver() {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "アラーム受信！")

        val channelId = "alarm_channel"
        val channelName = "アラーム通知"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ アラーム音を再生
        try {
            if (mediaPlayer == null) {
                val alarmUri: Uri = Settings.System.DEFAULT_ALARM_ALERT_URI
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, alarmUri)
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                    isLooping = true
                    prepare()
                    setVolume(0.03f, 0.03f)
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "MediaPlayerエラー: ${e.message}")
        }

        // ✅ 停止ボタン
        val stopIntent = Intent(context, StopAlarmReceiver::class.java)
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ スヌーズボタン
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java)
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            102,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ 通知タップでアプリを開く
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            103,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ 通知作成
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ アラーム")
            .setContentText("時間になりました！")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                R.drawable.ic_menu_close_clear_cancel, // ← 停止アイコン
                "停止",
                stopPendingIntent
            )
            .addAction(
                R.drawable.ic_media_pause, // ← スヌーズアイコン
                "スヌーズ (1分)",
                snoozePendingIntent
            )

        NotificationManagerCompat.from(context).notify(2001, builder.build())
    }

    companion object {
        private var mediaPlayer: MediaPlayer? = null

        fun stopSound(context: Context) {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                NotificationManagerCompat.from(context).cancel(2001)
                Log.d("AlarmReceiver", "アラーム停止")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "停止エラー: ${e.message}")
            }
        }
    }
}
