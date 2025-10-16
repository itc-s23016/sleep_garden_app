package com.example.sleep_garden

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sleep_garden.alarm.AlarmReceiver
import com.example.sleep_garden.alarm.TimePickerBottomSheet
import com.example.sleep_garden.alarm.AlarmActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ 起動時にアラーム音停止（通知やバックグラウンド復帰時対応）
        AlarmActivity.stopAlarmSoundStatic()

        // ✅ 通知権限のリクエスト（Android 13以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // ✅ 正確なアラーム許可チェック（Android 12以上）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {

                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestExactAlarmPermission.launch(intent)
            }
        }

        // ✅ 通知チャンネル作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel",
                "アラーム通知",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // Compose UI
        setContent {
            MaterialTheme {
                var selectedHour by remember { mutableStateOf(7) }
                var selectedMinute by remember { mutableStateOf(0) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "設定時刻: %02d:%02d".format(selectedHour, selectedMinute),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // BottomSheetを表示
                        Button(onClick = {
                            val sheet = TimePickerBottomSheet { hour, minute ->
                                selectedHour = hour
                                selectedMinute = minute
                            }
                            sheet.show(supportFragmentManager, "TimePickerSheet")
                        }) {
                            Text("時間を設定")
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            setAlarm(selectedHour, selectedMinute)
                        }) {
                            Text("この時間にアラーム設定")
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // ✅ アプリ再開時にも音を止める
        AlarmActivity.stopAlarmSoundStatic()
    }

    /**
     * アラーム設定処理
     */
    private fun setAlarm(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)

        // 現在時刻を基準に設定時刻を決定
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // もし過去時刻なら翌日に設定
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1000, // 固定ID
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ アラームを正確にスケジュール
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        // ✅ 確認メッセージ
        Toast.makeText(
            this,
            "⏰ アラームを %02d:%02d に設定しました".format(hour, minute),
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dialog = android.app.TimePickerDialog(
        context,
        { _, hour, minute ->
            onTimeSelected(hour, minute)
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )
    dialog.setOnDismissListener { onDismissRequest() }
    dialog.show()
}
