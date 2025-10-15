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
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.util.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.sleep_garden.alarm.AlarmReceiver

class MainActivity : ComponentActivity() {

    private val requestExactAlarmPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ アプリ起動時にアラーム音を停止（通知タップで開かれたとき対応）
        AlarmReceiver.stopSound(this)

        // 通知権限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // 正確なアラーム権限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:$packageName")
                requestExactAlarmPermission.launch(intent)
            }
        }

        // 通知チャンネル作成
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "test_channel"
            val channelName = "アラーム通知"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // ✅ Compose UI
        setContent {
            MaterialTheme {
                var showDialog by remember { mutableStateOf(false) }
                var selectedHour by remember { mutableStateOf(7) }
                var selectedMinute by remember { mutableStateOf(0) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "設定時刻: %02d:%02d".format(selectedHour, selectedMinute),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { showDialog = true }) {
                                Text("時間を選択")
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                setAlarm(selectedHour, selectedMinute)
                            }) {
                                Text("この時間にアラーム設定")
                            }
                        }

                        if (showDialog) {
                            TimePickerDialog(
                                onDismissRequest = { showDialog = false },
                                onTimeSelected = { hour, minute ->
                                    selectedHour = hour
                                    selectedMinute = minute
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ✅ アプリがバックグラウンドから戻った時もアラーム音を止める
    override fun onResume() {
        super.onResume()
        // アプリを開いたらアラーム音を止める
        AlarmReceiver.stopSound(this)
    }


    private fun setAlarm(hour: Int, minute: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 現在時刻から指定時刻に設定
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // もし過去の時刻なら翌日に設定
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(
            this,
            "アラームを %02d:%02d に設定しました".format(hour, minute),
            Toast.LENGTH_SHORT
        ).show()
    }

    @Composable
    fun TimePickerDialog(
        onDismissRequest: () -> Unit,
        onTimeSelected: (Int, Int) -> Unit
    ) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        val timePickerDialog = android.app.TimePickerDialog(
            context,
            { _, hour: Int, minute: Int ->
                onTimeSelected(hour, minute)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.setOnDismissListener { onDismissRequest() }
        timePickerDialog.show()
    }
}
