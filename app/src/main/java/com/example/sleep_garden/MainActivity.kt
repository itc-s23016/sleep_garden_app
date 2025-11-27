package com.example.sleep_garden

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sleep_garden.data.flower.FlowerViewModel
import com.example.sleep_garden.data.flower.Zukan
import com.example.sleep_garden.data.xp.ExperienceViewModel
import com.example.sleep_garden.data.xp.XpResult
import kotlinx.coroutines.launch
import kotlin.math.max

/* ------------------- スヌーズ状態管理 ------------------- */

fun setSnoozed(ctx: Context, value: Boolean) {
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("snoozed", value)
        .apply()
}

fun wasSnoozed(ctx: Context): Boolean =
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .getBoolean("snoozed", false)


/* ------------------- MainActivity ------------------- */

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRoute = if (isSleepActive(this)) "sleep" else "home"

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }
            val scheme = if (isDark) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = scheme) {
                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = initialRoute) {

                    /* -------- Home -------- */
                    composable("home") {
                        HomeScreen(
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark },
                            onAlarmClick = { nav.navigate("alarm") },
                            onDexClick = { nav.navigate("zukan") },
                            onSleepClick = {
                                setSleepActive(applicationContext, true)
                                setSleepStartAt(applicationContext, System.currentTimeMillis())
                                nav.navigate("sleep") { launchSingleTop = true }
                            }
                        )
                    }

                    /* -------- 図鑑 -------- */
                    composable("zukan") {
                        Zukan(onBack = { nav.popBackStack() })
                    }

                    /* -------- 睡眠 -------- */
                    composable("sleep") {
                        SleepScreen(
                            onWake = {
                                nav.navigate("home") {
                                    launchSingleTop = true
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        )
                    }

                    /* -------- アラーム -------- */
                    composable("alarm") {
                        com.example.sleep_garden.alarm.AlarmScreen(
                            onBack = { nav.popBackStack() },
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark }
                        )
                    }
                }
            }
        }
    }
}


/* ------------------- SleepScreen（完全修正版） ------------------- */

@Composable
fun SleepScreen(onWake: () -> Unit) {
    val ctx = LocalContext.current
    val flowerVm: FlowerViewModel = viewModel()
    val xpVm: ExperienceViewModel = viewModel()
    val scope = rememberCoroutineScope()

    var showPopup by remember { mutableStateOf(false) }

    // XP 表示
    var gainedXp by remember { mutableStateOf(0) }
    var level by remember { mutableStateOf(1) }
    var progress by remember { mutableStateOf(0) }
    var needForNext by remember { mutableStateOf(0) }
    var leveledTo by remember { mutableStateOf<Int?>(null) }

    // 花
    var rewardName by remember { mutableStateOf<String?>(null) }
    var rewardImage by remember { mutableStateOf<Int?>(null) }

    // 多重タップ防止
    var processing by remember { mutableStateOf(false) }

    // 図鑑初期データ投入
    LaunchedEffect(Unit) { flowerVm.insertInitialFlowers() }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(R.drawable.sleep_overlay),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = (maxWidth * 0.9f).coerceIn(220.dp, 500.dp)
            val h = w * (118f / 362f)
            val pad = h * 0.02f

            Box(
                modifier = Modifier
                    .size(w, h)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {

                        if (processing) return@clickable
                        processing = true

                        scope.launch {

                            val sleepAt = getSleepStartAt(ctx) ?: System.currentTimeMillis()
                            val wakeAt = System.currentTimeMillis()
                            val durationMin = max(0, ((wakeAt - sleepAt) / 60000).toInt())
                            val snoozed = wasSnoozed(ctx)

                            // XP 半減
                            val effectiveDuration =
                                if (snoozed) durationMin / 2 else durationMin

                            // 花（スヌーズ時は無し）
                            if (!snoozed) {
                                val result = flowerVm.rewardRandomFlowerIfEligible(durationMin)
                                rewardName = result?.name
                                rewardImage = result?.imageResId
                            } else {
                                rewardName = null
                                rewardImage = null
                            }

                            // XP 保存（半減後）
                            xpVm.onWakeConfirm(
                                sleepAtMillis = sleepAt,
                                wakeAtMillis = wakeAt,
                                note = if (snoozed) "SNOOZE" else null,
                                effectiveDurationMin = effectiveDuration,
                                onResult = { r: XpResult ->
                                    gainedXp = r.session.gainedXp
                                    level = r.summary.level
                                    progress = r.levelInfo.progressInLevel
                                    needForNext = r.levelInfo.needForNext

                                    leveledTo =
                                        if (r.levelInfo.progressInLevel == 0)
                                            r.summary.level
                                        else null

                                    showPopup = true
                                },
                                onError = { processing = false }
                            )

                            // 状態リセット
                            setSleepActive(ctx, false)
                            setSleepStartAt(ctx, null)
                            setSnoozed(ctx, false)
                        }
                    }
            ) {
                Image(
                    painter = painterResource(R.drawable.btn_wake),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentScale = ContentScale.Fit
                )
            }
        }

        if (showPopup) {
            SleepResultPopup(
                gainedXp = gainedXp,
                level = level,
                progress = progress,
                needForNext = needForNext,
                leveledTo = leveledTo,
                rewardName = rewardName,
                rewardImage = rewardImage,
                onDismiss = {
                    showPopup = false
                    onWake()
                }
            )
        }
    }
}


/* ------------------- 結果ポップアップ ------------------- */

@Composable
fun SleepResultPopup(
    gainedXp: Int,
    level: Int,
    progress: Int,
    needForNext: Int,
    leveledTo: Int?,
    rewardName: String?,
    rewardImage: Int?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {

                Text(
                    "獲得XP",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(20.dp))

                Text(
                    "+$gainedXp XP",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))

                leveledTo?.let {
                    Text(
                        "レベルアップ！ → Lv $it",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(12.dp))
                }

                Text("現在レベル：Lv $level")
                Spacer(Modifier.height(4.dp))

                if (needForNext > 0) {
                    Text("次のレベルまで：${needForNext - progress} XP")
                } else {
                    Text("レベルは最大です")
                }

                // 花（スヌーズ時は出ない）
                rewardName?.let { name ->
                    Spacer(Modifier.height(26.dp))
                    Text(
                        "ごほうびの花",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(name)

                    rewardImage?.let { resId ->
                        Spacer(Modifier.height(16.dp))
                        Image(
                            painter = painterResource(resId),
                            contentDescription = name,
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(26.dp))
                Text("タップで閉じる", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


/* ------------------- Sleep 状態管理 ------------------- */

private fun isSleepActive(ctx: Context): Boolean =
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .getBoolean("sleep_active", false)

private fun setSleepActive(ctx: Context, value: Boolean) {
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("sleep_active", value)
        .apply()
}

private fun getSleepStartAt(ctx: Context): Long? {
    val t = ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .getLong("sleep_started_at", 0L)
    return if (t == 0L) null else t
}

private fun setSleepStartAt(ctx: Context, timeMillis: Long?) {
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .edit()
        .putLong("sleep_started_at", timeMillis ?: 0L)
        .apply()
}


/* ------------------- ImageButton 共通UI ------------------- */

@Composable
private fun ImageButton(
    @DrawableRes resId: Int,
    contentDesc: String?,
    modifier: Modifier = Modifier,
    corner: Dp = 18.dp,
    contentPadding: Dp = 10.dp,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        label = "imageButtonScale"
    )

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner))
            .indication(interaction, ripple())
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding)
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDesc,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}


/* ------------------- HomeScreen（完全修正版） ------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAlarmClick: () -> Unit,
    onDexClick: () -> Unit,
    onSleepClick: () -> Unit
) {
    val ctx = LocalContext.current
    val xpVm: ExperienceViewModel = viewModel()

    // XP summary（Roomから取得）
    val summary by xpVm.summary.collectAsState()

    val level = summary?.level ?: 1
    val totalXp = summary?.totalXp ?: 0

    val levelInfo = remember(level, totalXp) {
        com.example.sleep_garden.data.xp.Leveling.compute(totalXp)
    }

    val progress = remember(levelInfo) {
        if (levelInfo.needForNext == 0) 1f
        else levelInfo.progressInLevel.toFloat() / levelInfo.needForNext.toFloat()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("睡眠花育成", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Notifications, contentDescription = "")
                    }
                    IconButton(onClick = onToggleTheme) {
                        val icon = if (isDark) R.drawable.ic_light_mode_24 else R.drawable.ic_dark_mode_24
                        Icon(painterResource(icon), contentDescription = "")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier.fillMaxSize().padding(inner)
        ) {
            Image(
                painter = painterResource(R.drawable.home),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {

                /* ---- XP パネル ---- */
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Lv $level",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )

                            if (level < com.example.sleep_garden.data.xp.Leveling.MAX_LEVEL) {
                                Text("${levelInfo.progressInLevel} / ${levelInfo.needForNext} XP")
                            } else {
                                Text("MAX")
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = progress,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                /* ---- ボタン ---- */
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val buttonScale = 1.18f
                    val rowBtnHeight =
                        (maxWidth * 0.24f * buttonScale).coerceIn(120.dp, 208.dp)
                    val wideBtnHeight =
                        (maxWidth * 0.22f * buttonScale).coerceIn(110.dp, 196.dp)

                    val innerPadRow = rowBtnHeight * 0.09f
                    val innerPadWide = wideBtnHeight * 0.09f

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .offset(y = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .offset(y = 17.dp)
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ImageButton(
                                resId = R.drawable.btn_alarm,
                                contentDesc = "",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(rowBtnHeight),
                                corner = 20.dp,
                                contentPadding = innerPadRow,
                                onClick = onAlarmClick
                            )
                            ImageButton(
                                resId = R.drawable.btn_dex,
                                contentDesc = "",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(rowBtnHeight),
                                corner = 20.dp,
                                contentPadding = innerPadRow,
                                onClick = onDexClick
                            )
                        }

                        ImageButton(
                            resId = R.drawable.btn_sleep,
                            contentDesc = "",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(wideBtnHeight),
                            corner = 22.dp,
                            contentPadding = innerPadWide,
                            onClick = onSleepClick
                        )
                    }
                }
            }
        }
    }
}
