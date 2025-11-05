package com.example.sleep_garden.alarm

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sleep_garden.R
import com.example.sleep_garden.data.AlarmItem
import com.example.sleep_garden.data.LocalAlarmRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmScreen(
    onBack: () -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { LocalAlarmRepository.getInstance(context) }

    // 権限系（通知/TIRAMISU、正確アラーム/S+）
    val notifPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(context, "通知がオフだと気づけない可能性があります", Toast.LENGTH_SHORT).show()
            }
        }
    val exactAlarmLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                }
                exactAlarmLauncher.launch(intent)
            }
        }
        // 通知チャネル（O+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel("alarm_channel", "アラーム通知", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    // DB購読
    val alarms by remember(repo) { repo.observeAlarms() }.collectAsState(initial = emptyList())

    // ダイアログ制御（親が唯一の状態源）
    var pickerTargetId by remember { mutableStateOf<String?>(null) } // null=追加
    var showPicker by remember { mutableStateOf(false) }
    val nowInit = remember { Calendar.getInstance() }
    var tempHour by remember { mutableStateOf(nowInit.get(Calendar.HOUR_OF_DAY)) }
    var tempMinute by remember { mutableStateOf(nowInit.get(Calendar.MINUTE)) }

    // 端末スケジュールと同期
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
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { showAlarmVolumePanel(context) }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "アラーム音量")
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            painter = painterResource(
                                id = if (isDark) R.drawable.ic_light_mode_24 else R.drawable.ic_dark_mode_24
                            ),
                            contentDescription = "テーマ切替"
                        )
                    }
                    IconButton(onClick = {
                        val now = Calendar.getInstance()
                        pickerTargetId = null
                        tempHour = now.get(Calendar.HOUR_OF_DAY)
                        tempMinute = now.get(Calendar.MINUTE)
                        showPicker = true
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = "追加")
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
                    items(
                        count = alarms.size,
                        key = { i -> alarms[i].id }
                    ) { idx ->
                        val alarm = alarms[idx]
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
                                tempHour = alarm.hour
                                tempMinute = alarm.minute
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
            val dialogKey = pickerTargetId ?: "add"
            androidx.compose.runtime.key(dialogKey) {
                WheelTimePickerDialog(
                    hour = tempHour,
                    minute = tempMinute,
                    isDark = isDark,
                    onHourChange = { tempHour = it },
                    onMinuteChange = { tempMinute = it },
                    onDismiss = { showPicker = false },
                    onConfirm = {
                        showPicker = false
                        scope.launch {
                            if (pickerTargetId == null) {
                                val id = repo.addAlarm(tempHour, tempMinute, enabled = true)
                                scheduleAlarm(context, id, tempHour, tempMinute)
                                Toast.makeText(context, "アラームを追加しました", Toast.LENGTH_SHORT).show()
                            } else {
                                val id = pickerTargetId!!
                                repo.updateTime(id, tempHour, tempMinute)
                                alarms.find { it.id == id }?.let { item ->
                                    if (item.enabled) scheduleAlarm(context, id, tempHour, tempMinute)
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

/* ---------- 端末スケジュール ---------- */

private fun alarmRequestCode(alarmId: String): Int = abs(alarmId.hashCode())

private fun scheduleAlarm(context: Context, alarmId: String, hour: Int, minute: Int) {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val fireIntent = Intent(context, com.example.sleep_garden.alarm.AlarmReceiver::class.java).apply {
        putExtra("alarmId", alarmId)
        action = "com.example.sleep_garden.ALARM_$alarmId"
    }
    val firePi = PendingIntent.getBroadcast(
        context,
        alarmRequestCode(alarmId),
        fireIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val showIntent = Intent(context, com.example.sleep_garden.alarm.AlarmActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        putExtra("alarmId", alarmId)
    }
    val showPi = PendingIntent.getActivity(
        context,
        ("show_$alarmId").hashCode(),
        showIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, showPi)
        am.setAlarmClock(info, firePi)
    } else {
        am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, firePi)
    }
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
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.cancel(pending)
}

/** システムの音量UIを開いて、アラーム音量を調整させる */
private fun showAlarmVolumePanel(context: Context) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    am.adjustStreamVolume(
        AudioManager.STREAM_ALARM,
        AudioManager.ADJUST_SAME,
        AudioManager.FLAG_SHOW_UI
    )
}

/* --------- タイムピッカー（ダミー行＋停止スナップ） --------- */

@Composable
private fun WheelTimePickerDialog(
    hour: Int,
    minute: Int,
    isDark: Boolean,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("時間を選択") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Wheel(0..23, hour, onHourChange, { "%02d".format(it) }, isDark, Modifier.weight(1f).height(200.dp))
                Wheel(0..59, minute, onMinuteChange, { "%02d".format(it) }, isDark, Modifier.weight(1f).height(200.dp))
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("決定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("キャンセル") } }
    )
}

@Composable
private fun Wheel(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    formatter: (Int) -> String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 40.dp
) {
    val values = remember(range) { range.toList() }
    val centerOffset = visibleCount / 2

    val density = LocalDensity.current
    val itemHpx = with(density) { itemHeight.toPx() }

    // 値の行が先頭に来るよう初期化（上下はダミー行で見かけの余白にする）
    val initialIndex = (value - range.first).coerceIn(0, values.lastIndex)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = 0
    )

    // 外部 value が変化したら合わせ直し
    LaunchedEffect(value, range) {
        val i = (value - range.first).coerceIn(0, values.lastIndex)
        listState.scrollToItem(i, 0)
    }

    // 中央の行を求めて親に通知
    val selectedIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visibles = info.visibleItemsInfo
            if (visibles.isEmpty()) return@derivedStateOf initialIndex
            val viewportCenter = info.viewportStartOffset + (info.viewportEndOffset - info.viewportStartOffset) / 2f
            val nearest = visibles.minByOrNull { item ->
                val center = item.offset + item.size / 2f
                kotlin.math.abs(center - viewportCenter)
            } ?: return@derivedStateOf initialIndex
            (nearest.index - centerOffset).coerceIn(0, values.lastIndex)
        }
    }
    LaunchedEffect(selectedIndex) {
        val newValue = values[selectedIndex]
        if (newValue != value) onValueChange(newValue)
    }

    // 停止時は必ず行境界に吸着
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val off = listState.firstVisibleItemScrollOffset.toFloat()
            val down = (off >= itemHpx / 2f)
            val target = listState.firstVisibleItemIndex + if (down) 1 else 0
            listState.animateScrollToItem(target, 0)
        }
    }

    val lineColor = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 上ダミー
            items(count = centerOffset) {
                Box(Modifier.fillMaxWidth().height(itemHeight))
            }
            // 実データ
            items(values.size, key = { idx -> values[idx] }) { i ->
                val selected = (i == selectedIndex)
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatter(values[i]),
                        color = if (selected) textColor else textColor.copy(alpha = 0.6f),
                        style = if (selected)
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        else MaterialTheme.typography.titleMedium
                    )
                }
            }
            // 下ダミー
            items(count = centerOffset) {
                Box(Modifier.fillMaxWidth().height(itemHeight))
            }
        }

        // 中央の2本線
        Column(
            modifier = Modifier.align(Alignment.Center).height(itemHeight).fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Divider(color = lineColor, thickness = 2.dp)
            Divider(color = lineColor, thickness = 2.dp)
        }
    }
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
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onEditTime)
            ) {
                Text(
                    text = "%02d:%02d".format(alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(text = if (alarm.enabled) "ON" else "OFF", style = MaterialTheme.typography.labelMedium)
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete")
            }
        }
    }
}
