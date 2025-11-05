package com.example.sleep_garden

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
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDark by rememberSaveable { mutableStateOf(systemDark) }
            val scheme = if (isDark) darkColorScheme() else lightColorScheme()

            MaterialTheme(colorScheme = scheme) {
                val nav = rememberNavController()
                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            isDark = isDark,
                            onToggleTheme = { isDark = !isDark },
                            onAlarmClick = { nav.navigate("alarm") },
                            onDexClick = { /* TODO */ },
                            onSleepClick = { /* TODO */ }
                        )
                    }
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

/* ---------------- Home：下端そろえ（安全エリア最下端に吸着） ---------------- */

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
                    IconButton(onClick = { /* アラーム音量はアラーム画面で */ }) {
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
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                // 少し大きめに（前: 1.08f）
                val buttonScale = 1.18f   // 1.12〜1.22 の範囲でお好み調整OK

                val rowBtnHeight  = (maxWidth * 0.24f * buttonScale).coerceIn(120.dp, 208.dp)
                val wideBtnHeight = (maxWidth * 0.22f * buttonScale).coerceIn(110.dp, 196.dp)

// 見切れ防止の内側余白は少し抑えめ（大きくした分だけ絵柄を広く見せる）
                val innerPadRow   = rowBtnHeight * 0.09f   // 前: 0.10f
                val innerPadWide  = wideBtnHeight * 0.09f  // 前: 0.10f


                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)  // 最下端そろえ
                        .navigationBarsPadding()         // システムバー分だけ上に逃がす
                        .offset(y = 10.dp)                // ★ 全体を3dpだけ下へ
                        .padding(bottom = 0.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 上段2ボタン（左右を広く）
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

                    // 下段ワイドボタン（左右を広く）
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
