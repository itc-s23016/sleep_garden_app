package com.example.sleep_garden.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlin.math.abs

class SnoozeReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarmId") ?: "default"
        Log.d("SnoozeReceiver", "🕒 スヌーズを受信 alarmId=$alarmId")

        // 1) いま鳴っている音をサービス経由で停止
        val stopIntent = Intent(context, AlarmRingtoneService::class.java).apply {
            action = AlarmRingtoneService.ACTION_STOP
            putExtra("alarmId", alarmId)
        }
        // Android 8.0+ は startForegroundService が必要
        context.startForegroundService(stopIntent)

        // 2) 1分後に同じアラームを再スケジュール（AlarmReceiver へ戻す）
        val triggerAt = System.currentTimeMillis() + 60_000L
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alarmId)
            action = "com.example.sleep_garden.ALARM_$alarmId"
        }
        val pending = PendingIntent.getBroadcast(
            context,
            abs(alarmId.hashCode()), // アラームごとに一意の requestCode
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)

        Toast.makeText(context, "⏰ スヌーズ：1分後に再アラーム", Toast.LENGTH_SHORT).show()
        Log.d("SnoozeReceiver", "✅ 再アラーム設定 triggerAt=$triggerAt")
    }
}
