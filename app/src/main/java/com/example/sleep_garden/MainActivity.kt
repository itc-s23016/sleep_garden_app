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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.sleep_garden.data.XpRepository
import kotlin.math.max
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import com.example.sleep_garden.data.flower.Zukan


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
            var showZukan by rememberSaveable { mutableStateOf(false) }

            MaterialTheme(colorScheme = scheme) {
                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = initialRoute) {

                    // Home
                    composable("home") {
                        HomeScreen(
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark },
                            onAlarmClick = { nav.navigate("alarm") },
                            onDexClick = { nav.navigate("zukan") },
                            onSleepClick = {
                                // ★ 睡眠フラグON + 開始時刻保存 → sleep へ
                                setSleepActive(applicationContext, true)
                                setSleepStartAt(applicationContext, System.currentTimeMillis())
                                nav.navigate("sleep") { launchSingleTop = true }
                            }
                        )
                    }
                    composable("zukan") {
                        Zukan(onBack = { nav.popBackStack() })
                    }

                    // スリープ画面（アプリ内フルスクリーン表示）
                    composable("sleep") {
                        // ★ onWake は「ポップアップ閉鎖後にHomeへ戻る」ための遷移だけを担当
                        SleepScreen(
                            onWake = {
                                nav.navigate("home") {
                                    launchSingleTop = true
                                    popUpTo("home") { inclusive = true } // Homeを一枚だけに
                                }
                            }
                        )
                    }

                    // 既存アラーム画面
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

/* ---------------- Home：下端そろえ（以前のUIそのまま） ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onAlarmClick: () -> Unit,
    onDexClick: () -> Unit,
    onSleepClick: () -> Unit
) {
    // ▼ 追加：XP表示のための状態（起動/復帰時に最新反映）
    val ctx = LocalContext.current
    val xpRepo = remember { com.example.sleep_garden.data.XpRepository.getInstance(ctx) }

    var level by remember { mutableStateOf(xpRepo.getLevel()) }
    var currentXp by remember { mutableStateOf(xpRepo.getXp()) }
    var nextReq by remember { mutableStateOf(xpRepo.getRequiredXpFor(level)) }

    // Home に戻ってきた時点で最新値を反映
    LaunchedEffect(Unit) {
        level = xpRepo.getLevel()
        currentXp = xpRepo.getXp()
        nextReq = xpRepo.getRequiredXpFor(level)
    }

    val progress = remember(level, currentXp, nextReq) {
        if (level >= com.example.sleep_garden.data.XpRepository.MAX_LEVEL) 1f
        else if (nextReq <= 0) 0f
        else (currentXp.toFloat() / nextReq.toFloat()).coerceIn(0f, 1f)
    }
    // ▲ ここまで追加

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

            // ===== 上部：XPバー（追加） + 下部：既存ボタン群 =====
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // ▼ 追加：XPパネル
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
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (level < com.example.sleep_garden.data.XpRepository.MAX_LEVEL) {
                                Text(
                                    text = "$currentXp / $nextReq XP",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text(
                                    text = "MAX",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
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
                // ▲ ここまでXPパネル

                Spacer(Modifier.weight(1f)) // 下のボタン群を最下段へ寄せる

                // ===== 画面最下端（安全エリア下端）にボタン群を吸着（既存そのまま） =====
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val buttonScale = 1.18f
                    val rowBtnHeight  = (maxWidth * 0.24f * buttonScale).coerceIn(120.dp, 208.dp)
                    val wideBtnHeight = (maxWidth * 0.22f * buttonScale).coerceIn(110.dp, 196.dp)
                    val innerPadRow   = rowBtnHeight * 0.09f
                    val innerPadWide  = wideBtnHeight * 0.09f

                    // ▼ 下げ幅をここで調整（例: 10.dp → 24.dp）
                    val buttonsYOffset = 22.dp

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .offset(y = buttonsYOffset),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 上段2ボタン
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .offset(y = 17.dp) // 上段だけ少し下げる
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

                        // 下段ワイド（寝る・成長）
                        ImageButton(
                            resId = R.drawable.btn_sleep,
                            contentDesc = "寝る・成長",
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


/* ---------------- スリープ画面（アプリ内フルスクリーン + 起きるボタン + 獲得XPポップアップ） ---------------- */

@Composable
private fun SleepScreen(

    onWake: () -> Unit // ← ポップアップを閉じた後に呼ぶ（= Homeに戻る）
) {
    val ctx = LocalContext.current

    // ポップアップ表示用の状態
    var showGained by remember { mutableStateOf(false) }
    var gainedAmount by remember { mutableStateOf(0) }
    var popupLevel by remember { mutableStateOf(1) }
    var popupCurrentXp by remember { mutableStateOf(0) }
    var popupNextReq by remember { mutableStateOf(0) }
    var popupLeveledTo by remember { mutableStateOf<Int?>(null) }

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
                    .clickable(
                        onClick = {
                            // ★ 起きる押下：ここでXP計算 → ポップアップ表示
                            val start = getSleepStartAt(ctx) ?: System.currentTimeMillis()
                            val minutes = max(0, ((System.currentTimeMillis() - start) / 60_000L).toInt())

                            val xpRepo = XpRepository.getInstance(ctx)
                            val result = xpRepo.addXpAndLevelUp(minutes)

                            gainedAmount = result.added
                            popupLevel = result.newLevel
                            popupCurrentXp = result.newXp
                            popupNextReq = result.newRequired
                            popupLeveledTo = result.leveledUpTo
                            showGained = true
                        }
                    )
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

        // ★ 獲得XPポップアップ（閉じたらフラグOFF→開始時刻クリア→Homeへ）
        if (showGained) {
            GainedXpPopup(
                gained = gainedAmount,
                newLevel = popupLevel,
                currentXp = popupCurrentXp,
                nextReq = popupNextReq,
                leveledUpTo = popupLeveledTo,
                onDismiss = {
                    setSleepActive(ctx, false)
                    setSleepStartAt(ctx, null)
                    showGained = false
                    onWake() // ← Homeへ戻る
                }
            )
        }
    }
}

/* ---------------- 画像そのものを押せるボタン（中身は余白で縮小） ---------------- */

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
            .padding(contentPadding) // 画像だけ一回り内側へ（見切れ防止）
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

/* ---------------- 追加：睡眠開始時刻の保存/取得（XP計算のため） ---------------- */

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

/* ---------------- 追加：獲得XPポップアップ（詳細付き） ---------------- */

@Composable
private fun GainedXpPopup(
    gained: Int,
    newLevel: Int,
    currentXp: Int,
    nextReq: Int,
    leveledUpTo: Int?, // nullで「レベルアップなし」
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .padding(24.dp)
                .clickable(enabled = false) {}
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

                if (leveledUpTo != null) {
                    Text(
                        text = "レベルアップ！ → Lv $leveledUpTo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(6.dp))
                }

                Text(
                    text = "現在レベル：Lv $newLevel",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))

                if (newLevel < XpRepository.MAX_LEVEL) {
                    Text(
                        text = "次のレベルまで：${(nextReq - currentXp).coerceAtLeast(0)} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "レベルは最大です",
                        style = MaterialTheme.typography.bodyMedium, // ← 既存に合わせてください。typography なら修正: MaterialTheme.typography.bodyMedium
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = "タップで閉じる",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
