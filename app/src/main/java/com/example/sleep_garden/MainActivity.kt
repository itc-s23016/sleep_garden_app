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
import androidx.compose.ui.zIndex
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sleep_garden.data.XpRepository
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ 次回起動時のスタート画面を決める（睡眠中なら "sleep"）
        val initialRoute = if (isSleepActive(this)) "sleep" else "home"

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }
            val scheme = if (isDark) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = scheme) {
                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = initialRoute) {

                    // Home
                    composable("home") {
                        HomeScreen(
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark },
                            onAlarmClick = { nav.navigate("alarm") },

                            onDexClick = { /* TODO: 図鑑 */ },
                            onSleepClick = {
                                // ★ 睡眠フラグONにして sleep 画面へ遷移
                                setSleepActive(applicationContext, true)
                                nav.navigate("sleep") {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    // スリープ画面（アプリ内フルスクリーン表示）
                    composable("sleep") {
                        SleepScreen(
                            onWake = {
                                // ★ 起きる：フラグOFF → Homeへ
                                setSleepActive(applicationContext, false)
                                nav.navigate("home") {
                                    launchSingleTop = true
                                    popUpTo("home") { inclusive = true } // Homeを一枚だけに
                                }
                            }
                        )
                    }

                    // 既存アラーム画面
                    composable("alarm") {
                        // 既存の AlarmScreen(Compose) がある前提
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

/* ================= Home：XPバー + ボタン群 + 寝るオーバーレイ + 獲得ポップアップ ================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAlarmClick: () -> Unit,
    onDexClick: () -> Unit
) {
    val ctx = LocalContext.current
    val xpRepo = remember { XpRepository.getInstance(ctx) }

    var level by remember { mutableStateOf(xpRepo.getLevel()) }
    var currentXp by remember { mutableStateOf(xpRepo.getXp()) }
    var nextReq by remember { mutableStateOf(xpRepo.getRequiredXpFor(level)) }

    var sleepStartAt by rememberSaveable { mutableStateOf<Long?>(null) }

    var showGained by remember { mutableStateOf(false) }
    var gainedAmount by remember { mutableStateOf(0) }

    var showSleepOverlay by remember { mutableStateOf(false) }

    val progress = remember(level, currentXp, nextReq) {
        if (level >= XpRepository.MAX_LEVEL) 1f
        else if (nextReq <= 0) 0f
        else (currentXp.toFloat() / nextReq.toFloat()).coerceIn(0f, 1f)
    }

    // 起きる処理（下段ボタン／オーバーレイどちらからでも呼ぶ）
    fun performWake() {
        val end = System.currentTimeMillis()
        val minutes = max(0, ((end - (sleepStartAt ?: end)) / 60_000L).toInt())
        val (added, newLevel, newXp, newReq, _) = xpRepo.addXpAndLevelUp(minutes /* 1分=1XP */)

        level = newLevel
        currentXp = newXp
        nextReq = newReq

        gainedAmount = added
        showGained = true

        sleepStartAt = null
        showSleepOverlay = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("睡眠花育成", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = { /* アラーム音量UIはアラーム画面で */ }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "アラーム音量")
                    }
                    IconButton(onClick = onToggleTheme) {
                        val icon = if (isDark) R.drawable.ic_light_mode_24 else R.drawable.ic_dark_mode_24
                        Icon(painterResource(icon), contentDescription = "テーマ切替")
                    }
                }
            )
        }
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            // 背景
            Image(
                painter = painterResource(R.drawable.home),
                contentDescription = "background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // === 上部：レベル表示 + XPバー（見やすさ向上済み） ===
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Lv $level",
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            if (level < XpRepository.MAX_LEVEL) {
                                Text(
                                    text = "${currentXp} / $nextReq XP",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            } else {
                                Text(
                                    text = "MAX",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
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

                // === 下部：上2 / 下1 のボタン配置（元レイアウト） ===
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val buttonScale = 1.18f
                    val rowBtnHeight  = (maxWidth * 0.24f * buttonScale).coerceIn(120.dp, 208.dp)
                    val wideBtnHeight = (maxWidth * 0.22f * buttonScale).coerceIn(110.dp, 196.dp)
                    val innerPadRow   = rowBtnHeight * 0.09f
                    val innerPadWide  = wideBtnHeight * 0.09f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .offset(y = 10.dp),
                        verticalArrangement = Arrangement.Bottom,
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
                                contentDesc = "アラーム・種植え",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(rowBtnHeight),
                                corner = 20.dp,
                                contentPadding = innerPadRow,
                                onClick = onAlarmClick
                            )
                            ImageButton(
                                resId = R.drawable.btn_dex,
                                contentDesc = "花図鑑",
                                modifier = Modifier
                                    .weight(1f)
                                    .height(rowBtnHeight),
                                corner = 20.dp,
                                contentPadding = innerPadRow,
                                onClick = onDexClick
                            )
                        }

                        ImageButton(
                            resId = if (sleepStartAt == null) R.drawable.btn_sleep else R.drawable.btn_wake,
                            contentDesc = if (sleepStartAt == null) "寝る" else "起きる",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp)
                                .height(wideBtnHeight),
                            corner = 22.dp,
                            contentPadding = innerPadWide
                        ) {
                            if (sleepStartAt == null) {
                                // 寝る開始
                                sleepStartAt = System.currentTimeMillis()
                                showSleepOverlay = true
                            } else {
                                // 起きる（下段ボタン）
                                performWake()
                            }
                        }
                    }
                }
            }

            // 寝ている間のオーバーレイ（最前面 / 起きるで performWake）
            SleepOverlay(
                visible = showSleepOverlay,
                onWakeClick = {
                    if (sleepStartAt != null) performWake()
                }
            )

            // 獲得XPポップアップ（どこでもタップで閉じる）
            if (showGained) {
                GainedXpPopup(
                    gained = gainedAmount,
                    newLevel = level,
                    currentXp = currentXp,
                    nextReq = nextReq,
                    onDismiss = { showGained = false }
                )
            }
        }
    }
}

/* ---------------- 画像ボタン（拡大縮小の押下演出つき） ---------------- */

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
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, label = "pressScale")

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner))
            .indication(interaction, ripple())
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(contentPadding)
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDesc,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            alignment = Alignment.Center
        )
    }
}

/* ---------------- フルスクリーン “おやすみ” オーバーレイ（起きるボタンあり） ---------------- */




/* ---------------- スリープ画面（アプリ内フルスクリーン + 起きるボタン） ---------------- */

@Composable
private fun SleepScreen(
    onWake: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Image(
            painter = painterResource(R.drawable.sleep_overlay),
            contentDescription = "sleep overlay",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val w = (maxWidth * 0.90f).coerceIn(220.dp, 500.dp)
            val h = w * (118f / 362f)

            val innerPad = h * 0.02f

            Box(
                modifier = Modifier
                    .size(width = w, height = h)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onWakeClick)
            ) {
                Image(
                    painter = painterResource(R.drawable.btn_wake),
                    contentDescription = "起きる",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPad),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/* ---------------- 獲得XPポップアップ ---------------- */


@Composable
private fun GainedXpPopup(
    gained: Int,
    newLevel: Int,
    currentXp: Int,
    nextReq: Int,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(24.dp)
                .clickable(enabled = false) { } // 中もタップで閉じたいなら削除
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "獲得XP",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "+$gained XP",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "現在レベル：Lv $newLevel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                if (newLevel < XpRepository.MAX_LEVEL) {
                    Text(
                        text = "次のレベルまで：${(nextReq - currentXp).coerceAtLeast(0)} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "レベルは最大です",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "どこでもタップで閉じる",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ---------------- シンプルな永続フラグ（睡眠中かどうか） ---------------- */

private fun isSleepActive(ctx: Context): Boolean =
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .getBoolean("sleep_active", false)

private fun setSleepActive(ctx: Context, value: Boolean) {
    ctx.getSharedPreferences("sleep_prefs", Context.MODE_PRIVATE)
        .edit()
        .putBoolean("sleep_active", value)
        .apply()
}
