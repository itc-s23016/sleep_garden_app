package com.example.sleep_garden.alarm

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import android.widget.Toast

class SnoozeReceiver : BroadcastReceiver() {

    @SuppressLint("ScheduleExactAlarm")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SnoozeReceiver", "🕒 スヌーズボタンが押されました")

        // ✅ アラーム音を停止
        AlarmActivity.stopAlarmSoundStatic()

        // ✅ 1分後（60000ミリ秒後）に再びアラームを鳴らす
        val snoozeTime = SystemClock.elapsedRealtime() + 60_000

        // 🔁 AlarmReceiver 経由で再び AlarmActivity を起動
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2001, // 固定ID（同じアラームを上書き）
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            snoozeTime,
            pendingIntent
        )

        Toast.makeText(context, "⏰ スヌーズ：1分後に再アラーム", Toast.LENGTH_SHORT).show()
        Log.d("SnoozeReceiver", "✅ 1分後に再アラーム設定完了")
    }
}
