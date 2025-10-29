package com.example.sleep_garden

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
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.sleep_garden.data.AlarmItem
import com.example.sleep_garden.data.LocalAlarmRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.round

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
                    data = android.net.Uri.parse("package:$packageName")
                }
                requestExactAlarmPermission.launch(intent)
            }
        }

        // 通知チャンネル
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_channel", "アラーム通知", NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }
            val scheme = if (isDark) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = scheme) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                val repo = remember { LocalAlarmRepository.getInstance(context) }

                // SQLiteのアラーム一覧を購読
                val alarms: List<AlarmItem> by remember(repo) { repo.observeAlarms() }
                    .collectAsState(initial = emptyList())

                // 追加・編集用（親が唯一のソース・オブ・トゥルース）
                var pickerTargetId by remember { mutableStateOf<String?>(null) } // null=追加
                var showPicker by remember { mutableStateOf(false) }
                val nowInit = remember { Calendar.getInstance() }
                var tempHour by remember { mutableStateOf(nowInit.get(Calendar.HOUR_OF_DAY)) }
                var tempMinute by remember { mutableStateOf(nowInit.get(Calendar.MINUTE)) }

                // 端末側のスケジュール同期
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
                                IconButton(onClick = { isDark = !isDark }) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isDark) R.drawable.ic_light_mode_24
                                            else R.drawable.ic_dark_mode_24
                                        ),
                                        contentDescription = if (isDark) "ライトモードに切替" else "ダークモードに切替"
                                    )
                                }
                                IconButton(onClick = { showAlarmVolumePanel(context) }) {
                                    Icon(Icons.Filled.Notifications, contentDescription = "アラーム音量")
                                }
                                IconButton(onClick = {
                                    val now = Calendar.getInstance()
                                    pickerTargetId = null
                                    tempHour = now.get(Calendar.HOUR_OF_DAY)
                                    tempMinute = now.get(Calendar.MINUTE)
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
                                items(
                                    items = alarms,
                                    key = { it.id }
                                ) { alarm ->
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

    val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, showPi)
    am.setAlarmClock(info, firePi)
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

/* ---------- ホイール（Compose） ---------- */

@Composable
fun WheelTimePickerDialog(
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Wheel(
                    range = 0..23,
                    value = hour,
                    onValueChange = onHourChange,
                    formatter = { "%02d".format(it) },
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                )
                Wheel(
                    range = 0..59,
                    value = minute,
                    onValueChange = onMinuteChange,
                    formatter = { "%02d".format(it) },
                    isDark = isDark,
                    modifier = Modifier
                        .weight(1f)
                        .height(200.dp)
                )
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

    // 初期位置（値の行が中央に来るように：先頭をその値に）
    val initialIndex = (value - range.first).coerceIn(0, values.lastIndex)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = 0
    )

    // 外部の value が変わったら中央に再合わせ
    LaunchedEffect(value, range) {
        val i = (value - range.first).coerceIn(0, values.lastIndex)
        listState.scrollToItem(i, 0)
    }

    // ビューポート中央に最も近い“実アイテム”を選ぶ（上下ダミー行あり）
    val selectedIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visibles = info.visibleItemsInfo
            if (visibles.isEmpty()) return@derivedStateOf initialIndex

            val viewportCenter = info.viewportStartOffset +
                    (info.viewportEndOffset - info.viewportStartOffset) / 2f

            val nearest = visibles.minByOrNull { item ->
                val center = item.offset + item.size / 2f
                kotlin.math.abs(center - viewportCenter)
            } ?: return@derivedStateOf initialIndex

            // ダミー行の分（centerOffset）を引いて実データのindexに戻す
            (nearest.index - centerOffset).coerceIn(0, values.lastIndex)
        }
    }

    // 値通知（選択が変わったら親へ）
    LaunchedEffect(selectedIndex) {
        val newValue = values[selectedIndex]
        if (newValue != value) onValueChange(newValue)
    }

    // スクロール停止時は必ず行境界に吸着（微妙な位置で止まらない）
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val off = listState.firstVisibleItemScrollOffset.toFloat()
            val down = (off >= itemHpx / 2f)
            val target = listState.firstVisibleItemIndex + if (down) 1 else 0
            listState.animateScrollToItem(target, 0)
        }
    }

    val lineColor =
        if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
    val textColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 上ダミー行（見かけのパディング）
            items(count = centerOffset) {
                Box(Modifier.fillMaxWidth().height(itemHeight))
            }

            // 実データ
            items(values.size, key = { idx -> values[idx] }) { i ->
                val selected = (i == selectedIndex)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = formatter(values[i]),
                        color = if (selected) textColor else textColor.copy(alpha = 0.6f),
                        style = if (selected)
                            MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        else
                            MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 下ダミー行
            items(count = centerOffset) {
                Box(Modifier.fillMaxWidth().height(itemHeight))
            }
        }

        // 中央の2本線（正確に中央へ）
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .height(itemHeight)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Divider(color = lineColor, thickness = 2.dp)
            Divider(color = lineColor, thickness = 2.dp)
        }
    }
}

/** システムの音量UIを開く（アラーム音量） */
private fun showAlarmVolumePanel(context: Context) {
    val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    am.adjustStreamVolume(
        AudioManager.STREAM_ALARM,
        AudioManager.ADJUST_SAME,
        AudioManager.FLAG_SHOW_UI
    )
}

/* ---- アラーム一覧の行 ---- */
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
