package com.example.sleep_garden.data.flower

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Zukan(
    onBack: (() -> Unit)? = null
) {
    val ctx = LocalContext.current

    // ★ 安全に Activity を取得
    val activity = ctx as? ComponentActivity
        ?: throw IllegalStateException("Zukan must be used inside Activity")

    val vm: FlowerViewModel = viewModel(viewModelStoreOwner = activity)

    val flowers by vm.flowers.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Flower?>(null) }

    // ★ 初回だけ初期データ投入
    LaunchedEffect(Unit) {
        vm.insertInitialFlowers()
    }

    // ★ 戻る処理
    val handleBack = onBack ?: {
        val owner = ctx as? OnBackPressedDispatcherOwner
        if (owner != null) owner.onBackPressedDispatcher.onBackPressed()
        else if (ctx is Activity) ctx.finish()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("花図鑑") }) }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f))
        ) {

            // ====================
            //   花一覧
            // ====================
            if (flowers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("データがありません")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(flowers, key = { it.id }) { f ->
                        ZukanCell(f) { selected = f }
                    }
                }
            }

            // ====================
            //   オーバーレイ
            // ====================
            if (selected != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { selected = null }
                )
            }

            // ====================
            //   詳細カード
            // ====================
            selected?.let { f ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f)
                        .heightIn(min = 300.dp, max = 650.dp)
                ) {
                    val scroll = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .verticalScroll(scroll)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // ---- 画像表示：imageResId ----
                        Surface(
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            Image(
                                painter = painterResource(id = f.imageResId),
                                contentDescription = f.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // ---- タイトル + レアリティ ----
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                f.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text("☆${f.rarity}", style = MaterialTheme.typography.titleMedium)
                        }

                        // ---- 説明文 ----
                        Text(
                            f.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { selected = null }) { Text("閉じる") }
                        }
                    }
                }
            }

            // ====================
            //   戻るボタン
            // ====================
            if (selected == null) {
                Button(
                    onClick = handleBack,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Text("戻る")
                }
            }
        }
    }
}


/* ======================================================
   花１枠（4列グリッド用）
====================================================== */

@Composable
private fun ZukanCell(flower: Flower, onClick: () -> Unit) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(enabled = flower.found, onClick = onClick)
    ) {
        when {
            !flower.found -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("未発見 🌱", style = MaterialTheme.typography.labelLarge)
                }
            }

            else -> {
                Image(
                    painter = painterResource(id = flower.imageResId),
                    contentDescription = flower.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
