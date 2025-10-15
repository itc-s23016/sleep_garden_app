package com.example.sleep_garden.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class StopAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("StopAlarmReceiver", "停止ボタン受信 → アラーム停止")
        AlarmReceiver.stopSound(context)
    }
}