package com.example.sleep_garden.alarm

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarmId") ?: "default"

        // 1) 音を鳴らすサービスを起動（フォアグラウンド）
        val svc = Intent(context, AlarmRingtoneService::class.java).apply {
            action = AlarmRingtoneService.ACTION_START
            putExtra("alarmId", alarmId)
        }
        // Android 8.0+ は startForegroundService
        context.startForegroundService(svc)

        // 2) フルスクリーン通知（ロック画面でも前面に）
        val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("alarmId", alarmId)
        }
        val fullScreenPi = PendingIntent.getActivity(
            context, alarmId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPi = PendingIntent.getService(
            context, ("stop$alarmId").hashCode(),
            Intent(context, AlarmRingtoneService::class.java).apply {
                action = AlarmRingtoneService.ACTION_STOP
                putExtra("alarmId", alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val snoozePi = PendingIntent.getService(
            context, ("snooze$alarmId").hashCode(),
            Intent(context, AlarmRingtoneService::class.java).apply {
                action = AlarmRingtoneService.ACTION_SNOOZE
                putExtra("alarmId", alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, "alarm_channel")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("アラーム")
            .setContentText("アラームが鳴っています")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)   // ★ ロック画面でも前面化
            .addAction(0, "停止", stopPi)              // ★ 画面OFFのまま停止も可能
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alarmId.hashCode(), notif)
    }
}
