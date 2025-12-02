package com.example.sleep_garden.data.flower

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Zukan(
    onBack: (() -> Unit)? = null
) {
    val ctx = LocalContext.current

    val activity = ctx as? ComponentActivity
        ?: throw IllegalStateException("Zukan must be used inside Activity")

    val vm: FlowerViewModel = viewModel(viewModelStoreOwner = activity)

    val flowers by vm.flowers.collectAsState(initial = emptyList())
    var selected by remember { mutableStateOf<Flower?>(null) }

    // 初期データ投入
    LaunchedEffect(Unit) {
        vm.insertInitialFlowers()
    }

    // 戻る処理
    val handleBack = onBack ?: {
        val owner = ctx as? OnBackPressedDispatcherOwner
        if (owner != null) owner.onBackPressedDispatcher.onBackPressed()
        else if (ctx is Activity) ctx.finish()
    }

    // ★ レアリティごとにグルーピング（★1 → ★6）
    val groupedByRarity: Map<Int, List<Flower>> =
        flowers
            .sortedBy { it.rarity }   // 一応ソート
            .groupBy { it.rarity }    // rarity をキーにまとめる

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "花図鑑",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {},
                actions = {
                    IconButton(onClick = { handleBack() }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f)
                )
        ) {

            // ====================
            //   花一覧（レアリティ順）
            // ====================
            if (flowers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("データがありません")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // ★1 〜 ★6 の順で回して、あるレアリティだけ表示
                    (1..6).forEach { star ->
                        val list = groupedByRarity[star] ?: emptyList()
                        if (list.isEmpty()) return@forEach

                        // レアリティ見出し（4カラムぶち抜き）
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RarityHeader(star = star)
                        }

                        // そのレアリティの花たち
                        items(list, key = { it.id }) { f ->
                            ZukanCell(f) { selected = f }
                        }
                    }
                }
            }

            // ====================
            //   黒背景オーバーレイ
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
            //   花の詳細カード（元の仕様そのまま）
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

                        // ---- 画像 ----
                        Surface(
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(390.dp)
                        ) {
                            Image(
                                painter = painterResource(id = f.imageResId),
                                contentDescription = f.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // ---- タイトル + レア度 ----
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

                        // ---- 説明 ----
                        Text(
                            f.description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 10,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { selected = null }) {
                                Text("閉じる")
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ======================================================
   グリッド用セル（元の仕様そのまま）
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

/* ======================================================
   レアリティ見出し
====================================================== */
@Composable
private fun RarityHeader(star: Int) {
    val bg: Color
    val label: String

    when (star) {
        1 -> {
            bg = Color(0xFFE5E7EB) // グレー
            label = "★1（よく見る花）"
        }
        2 -> {
            bg = Color(0xFFBFDBFE) // 薄い青
            label = "★2（ちょっとレア）"
        }
        3 -> {
            bg = Color(0xFFA5B4FC) // 薄い紫
            label = "★3（レア）"
        }
        4 -> {
            bg = Color(0xFFFACC15) // 黄
            label = "★4（スーパーレア）"
        }
        5 -> {
            bg = Color(0xFFF97316) // オレンジ
            label = "★5（ウルトラレア）"
        }
        6 -> {
            bg = Color(0xFFFF4D94) // ピンク
            label = "★6（ゴージャス）"
        }
        else -> {
            bg = Color(0xFFE5E7EB)
            label = "★$star"
        }
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Start,
            color = Color.Black
        )
    }
}
