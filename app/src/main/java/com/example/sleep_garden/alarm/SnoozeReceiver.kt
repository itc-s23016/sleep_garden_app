package com.example.sleep_garden.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class SnoozeReceiver : BroadcastReceiver() {
    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SnoozeReceiver", "スヌーズ受信 → 一旦停止＆再セット")

        // ✅ 一旦アラーム停止
        AlarmReceiver.stopSound(context)

        // ✅ 5分後に再びアラームをセット
        val snoozeTime = System.currentTimeMillis() + 1 * 60 * 1000 // 1分後

        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent)

        Toast.makeText(context, "1分後に再通知します", Toast.LENGTH_SHORT).show()
    }
}
