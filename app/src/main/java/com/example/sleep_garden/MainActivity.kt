package com.example.sleep_garden

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

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

            // ===== 画面最下端（安全エリア下端）にボタン群を吸着 =====
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val buttonScale = 1.18f
                val rowBtnHeight  = (maxWidth * 0.24f * buttonScale).coerceIn(120.dp, 208.dp)
                val wideBtnHeight = (maxWidth * 0.22f * buttonScale).coerceIn(110.dp, 196.dp)
                val innerPadRow   = rowBtnHeight * 0.09f
                val innerPadWide  = wideBtnHeight * 0.09f

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .offset(y = 10.dp),
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
                    .clickable(onClick = onWake)
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
