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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
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
import com.example.sleep_garden.alarm.AlarmRingtoneService
import com.example.sleep_garden.data.AlarmItem
import com.example.sleep_garden.data.FirestoreAlarmRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "通知がオフだと気づけない可能性があります", Toast.LENGTH_SHORT).show()
            }
        }

    private val requestExactAlarmPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    Toast.makeText(this, "正確なアラームが許可されていません", Toast.LENGTH_SHORT).show()
                }
            }
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ アプリ起動時、鳴動していたら必ず停止（STOPは通常のstartServiceでOK）
        stopAnyRingingAlarm(this)

        // 通知権限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // 正確なアラーム許可（Android 12+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                requestExactAlarmPermission.launch(intent)
            }
        }

        // 通知チャンネル（サービス/レシーバが使用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel", "アラーム通知", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val repo = remember { FirestoreAlarmRepository() }

                // Firestore: 複数購読
                val alarms: List<AlarmItem> by remember(repo) { repo.observeAlarms() }
                    .collectAsState(initial = emptyList())

                // 追加・編集用
                var pickerTargetId by remember { mutableStateOf<String?>(null) } // null=追加
                var pickerHour by remember { mutableStateOf(7) }
                var pickerMinute by remember { mutableStateOf(0) }
                var showPicker by remember { mutableStateOf(false) }

                // Firestoreの状態に合わせて端末側も冪等スケジュール
                LaunchedEffect(alarms) {
                    alarms.forEach { a ->
                        if (a.enabled) scheduleAlarm(context, a.id, a.hour, a.minute)
                        else cancelAlarm(context, a.id)
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("アラーム") },
                            actions = {
                                IconButton(onClick = {
                                    val now = Calendar.getInstance()
                                    pickerTargetId = null
                                    pickerHour = now.get(Calendar.HOUR_OF_DAY)
                                    pickerMinute = now.get(Calendar.MINUTE)
                                    showPicker = true
                                }) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add")
                                }
                            }
                        )
                    }
                ) { inner ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(inner)
                    ) {
                        if (alarms.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("アラームがありません。右上の＋で追加")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(alarms, key = { it.id }) { alarm ->
                                    AlarmRow(
                                        alarm = alarm,
                                        onToggle = { checked ->
                                            scope.launch {
                                                repo.setEnabled(alarm.id, checked)
                                                if (checked) scheduleAlarm(context, alarm.id, alarm.hour, alarm.minute)
                                                else cancelAlarm(context, alarm.id)
                                            }
                                        },
                                        onEditTime = {
                                            pickerTargetId = alarm.id
                                            pickerHour = alarm.hour
                                            pickerMinute = alarm.minute
                                            showPicker = true
                                        },
                                        onDelete = {
                                            scope.launch {
                                                cancelAlarm(context, alarm.id)
                                                repo.delete(alarm.id)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (showPicker) {
                        WheelTimePickerDialog(
                            initialHour = pickerHour,
                            initialMinute = pickerMinute,
                            is24Hour = true,
                            onDismiss = { showPicker = false },
                            onConfirm = { h, m ->
                                showPicker = false
                                scope.launch {
                                    if (pickerTargetId == null) {
                                        val id = repo.addAlarm(h, m, enabled = true)
                                        scheduleAlarm(context, id, h, m)
                                        Toast.makeText(context, "アラームを追加しました", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val id = pickerTargetId!!
                                        repo.updateTime(id, h, m)
                                        // ONなら再スケジュール
                                        alarms.find { it.id == id }?.let { item ->
                                            if (item.enabled) scheduleAlarm(context, id, h, m)
                                        }
                                        Toast.makeText(context, "時刻を更新しました", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // ★ バックグラウンドから復帰時も念のため停止
        stopAnyRingingAlarm(this)
    }

    override fun onResume() {
        super.onResume()
        // 音停止は onCreate/onStart で実施済み
    }
}

/* ---------- 端末側スケジュール（アラーム毎に一意の requestCode） ---------- */

private fun alarmRequestCode(alarmId: String): Int = abs(alarmId.hashCode())

private fun scheduleAlarm(context: Context, alarmId: String, hour: Int, minute: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, com.example.sleep_garden.alarm.AlarmReceiver::class.java).apply {
        putExtra("alarmId", alarmId)
        action = "com.example.sleep_garden.ALARM_$alarmId"
    }
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }
    val pending = PendingIntent.getBroadcast(
        context,
        alarmRequestCode(alarmId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
}

private fun cancelAlarm(context: Context, alarmId: String) {
    val intent = Intent(context, com.example.sleep_garden.alarm.AlarmReceiver::class.java).apply {
        putExtra("alarmId", alarmId)
        action = "com.example.sleep_garden.ALARM_$alarmId"
    }
    val pending = PendingIntent.getBroadcast(
        context,
        alarmRequestCode(alarmId),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.cancel(pending)
}

/* ---------- アプリ起動/復帰時に鳴動を止めるユーティリティ ---------- */
private fun stopAnyRingingAlarm(context: Context) {
    val stop = Intent(context, AlarmRingtoneService::class.java).apply {
        action = AlarmRingtoneService.ACTION_STOP
    }
    // ★ 停止系は通常の startService() で十分（前面化不要）
    try {
        context.startService(stop)
    } catch (e: IllegalStateException) {
        // 保険としてフォアグラウンド起動に切り替え（通常ここには来ない）
        ContextCompat.startForegroundService(context, stop)
    }
}

/* ----------------- 以降：Wheel ピッカー ----------------- */

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
                WheelNumberPicker(
                    value = if (is24Hour) hour else ((hour + 11) % 12) + 1,
                    range = if (is24Hour) 0..23 else 1..12,
                    formatter = { v -> if (is24Hour) "%02d".format(v) else v.toString() },
                    onValueChange = { v ->
                        hour = if (is24Hour) v else {
                            val isPm = hour >= 12
                            ((v % 12) + if (isPm) 12 else 0) % 24
                        }
                    },
                    modifier = Modifier.width(100.dp)
                )
                Text(" : ", style = MaterialTheme.typography.titleLarge)
                WheelNumberPicker(
                    value = minute,
                    range = 0..59,
                    formatter = { v -> "%02d".format(v) },
                    onValueChange = { minute = it },
                    modifier = Modifier.width(100.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(hour, minute) }) { Text("決定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
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

/* ---- 一覧行 ---- */
@Composable
private fun AlarmRow(
    alarm: AlarmItem,
    onToggle: (Boolean) -> Unit,
    onEditTime: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEditTime)
            ) {
                Text(
                    text = "%02d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (alarm.enabled) "ON" else "OFF",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
            }
        }
    }
}
