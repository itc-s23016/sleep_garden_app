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
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sleep_garden.alarm.AlarmActivity
import com.example.sleep_garden.alarm.AlarmReceiver
import java.util.Calendar

class MainActivity : ComponentActivity() {

    // Android 13+ 通知権限ランチャー
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "通知がオフだと気づけない可能性があります", Toast.LENGTH_SHORT).show()
            }
        }

    // Android 12+ 正確なアラームの「設定画面」起動ランチャー
    private val requestExactAlarmPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // 戻ってきたら現状をチェックしてメッセージだけ出す
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "正確なアラームが許可されていません", Toast.LENGTH_SHORT).show()
                }
            }
        }

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
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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
                val context = LocalContext.current
                var selectedHour by remember { mutableStateOf(7) }
                var selectedMinute by remember { mutableStateOf(0) }
                var showTimePicker by remember { mutableStateOf(false) }

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

                        // ✅ Dialogベースの時刻選択（Fragment不要）
                        Button(onClick = { showTimePicker = true }) {
                            Text("時間を設定")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = {
                            setAlarm(context, selectedHour, selectedMinute)
                        }) {
                            Text("この時間にアラーム設定")
                        }
                    }

                    if (showTimePicker) {
                        WheelTimePickerDialog(
                            initialHour = selectedHour,
                            initialMinute = selectedMinute,
                            is24Hour = true, // 12時間制にしたい場合は false
                            onDismiss = { showTimePicker = false },
                            onConfirm = { h, m ->
                                selectedHour = h
                                selectedMinute = m
                                showTimePicker = false
                            }
                        )
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
}

/**
 * アラーム設定処理（Contextを受け取る形にしてComposableからも呼べるように）
 */
private fun setAlarm(context: Context, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, AlarmReceiver::class.java)

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
        context,
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
        context,
        "⏰ アラームを %02d:%02d に設定しました".format(hour, minute),
        Toast.LENGTH_SHORT
    ).show()
}

@Composable
fun WheelTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    is24Hour: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    var hour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("時間を選択", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 時
                WheelNumberPicker(
                    value = if (is24Hour) hour else ((hour + 11) % 12) + 1,
                    range = if (is24Hour) 0..23 else 1..12,
                    formatter = { v -> if (is24Hour) "%02d".format(v) else v.toString() },
                    onValueChange = { v ->
                        hour = if (is24Hour) v else {
                            // 12時間制の値を24時間制へ一旦マッピング（午前午後はダイアログ外で切替したいなら拡張可能）
                            // ここでは「現在のhourのAM/PM」を維持して、時だけ更新します
                            val isPm = hour >= 12
                            ((v % 12) + if (isPm) 12 else 0) % 24
                        }
                    },
                    modifier = Modifier.width(100.dp)
                )

                Text(" : ", style = MaterialTheme.typography.titleLarge)

                // 分
                WheelNumberPicker(
                    value = minute,
                    range = 0..59,
                    formatter = { v -> "%02d".format(v) },
                    onValueChange = { minute = it },
                    modifier = Modifier.width(100.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text("決定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}

@Composable
private fun WheelNumberPicker(
    value: Int,
    range: IntRange,
    formatter: ((Int) -> String)? = null,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.height(160.dp),
        factory = { context ->
            NumberPicker(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                minValue = range.first
                maxValue = range.last
                this.value = value
                wrapSelectorWheel = true
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
                // 表示フォーマット（先頭0埋めなど）
                setFormatter { v -> formatter?.invoke(v) ?: v.toString() }
                setOnValueChangedListener { _, _, newVal -> onValueChange(newVal) }
            }
        },
        update = { picker ->
            if (picker.minValue != range.first) picker.minValue = range.first
            if (picker.maxValue != range.last) picker.maxValue = range.last
            if (picker.value != value) picker.value = value
            picker.setFormatter { v -> formatter?.invoke(v) ?: v.toString() }
        }
    )
}