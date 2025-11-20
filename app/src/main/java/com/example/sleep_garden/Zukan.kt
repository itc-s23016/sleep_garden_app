package com.example.sleep_garden.data.flower

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

    // レアリティの低い順で並べ替えたリスト
    val sortedFlowers = remember(flowers) {
        flowers.sortedWith(
            compareBy<Flower> { it.rarity }  // レアリティの小さい順
                .thenBy { it.id }            // 同じレアリティ内は id 順
        )
    }

    // 図鑑の進捗
    val totalCount = flowers.size
    val foundCount = flowers.count { it.found }
    val progress = if (totalCount > 0) foundCount.toFloat() / totalCount else 0f

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // ===== 図鑑進捗（30 / 50 みたいな表示） =====
                if (totalCount > 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "図鑑達成度  $foundCount / $totalCount",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 上部：レアリティ一覧
                RarityLegend()

                Spacer(modifier = Modifier.height(8.dp))

                if (sortedFlowers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("データがありません")
                    }
                } else {
                    // 下部：レアリティごとに区切り付きのグリッド
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),   // 3列で写真大きめ
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        var lastRarity: Int? = null

                        for (f in sortedFlowers) {
                            // ★ レアリティが変わるところで見出しを追加
                            if (f.rarity != lastRarity) {
                                item(
                                    key = "header_${f.rarity}",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    RaritySectionHeader(rarity = f.rarity)
                                }
                                lastRarity = f.rarity
                            }

                            // 各花セル
                            item(key = f.id) {
                                ZukanCell(f) { selected = f }
                            }
                        }
                    }
                }
            }

            // 黒背景オーバーレイ
            if (selected != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { selected = null }
                )
            }

            // 花の詳細カード（★レアリティによって豪華さが変わる★）
            selected?.let { f ->
                val rarityText = rarityLabel(f.rarity)
                val rarityColor = rarityColor(f.rarity)
                val (borderWidth, borderColor) = rarityDetailBorder(f.rarity, rarityColor)
                val gradientColors = rarityDetailGradientColors(f.rarity)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    tonalElevation = if (f.rarity >= 4) 10.dp else 6.dp,
                    border = BorderStroke(borderWidth, borderColor),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f)
                        .heightIn(min = 300.dp, max = 650.dp)
                ) {
                    val scroll = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = gradientColors
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(scroll)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {

                            // レア度メッセージ（レアリティが高いほど派手に）
                            when {
                                f.rarity >= 6 -> {
                                    Text(
                                        text = "✨ 超ゴージャスな花が咲いた！ ✨",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = rarityColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                f.rarity == 5 -> {
                                    Text(
                                        text = "✨ 超激レアな花が咲いた！ ✨",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = rarityColor,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                f.rarity == 4 -> {
                                    Text(
                                        text = "🌟 超レアな花を発見！ 🌟",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = rarityColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                f.rarity == 3 -> {
                                    Text(
                                        text = "レアな花を見つけた！",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = rarityColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // 画像（レアリティに応じて枠線＋キラキラ）
                            Surface(
                                tonalElevation = 2.dp,
                                shape = RoundedCornerShape(16.dp),
                                border = if (f.rarity >= 3)
                                    BorderStroke(2.dp, rarityColor.copy(alpha = 0.9f))
                                else
                                    null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(390.dp)
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    Image(
                                        painter = painterResource(id = f.imageResId),
                                        contentDescription = f.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    // ★ レアリティ5,6なら画像の上にキラキラ
                                    if (f.rarity >= 5) {
                                        Text(
                                            text = "✨",
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(8.dp),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = "✨",
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(12.dp),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                    }
                                }
                            }

                            // タイトル + レア度バッジ
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

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = rarityColor.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, rarityColor)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = rarityStars(f.rarity),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = rarityColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = rarityText,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = rarityColor
                                        )
                                    }
                                }
                            }

                            // 説明
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
}

/* ======================================================
   上の「レアリティ一覧」カード
====================================================== */
@Composable
private fun RarityLegend() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "レアリティ一覧",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            for (r in 1..6) {   // ★ 6 まで表示
                val color = rarityColor(r)
                val label = rarityLabel(r)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = color.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, color)
                    ) {
                        Text(
                            text = rarityStars(r),
                            style = MaterialTheme.typography.labelMedium,
                            color = color,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/* ======================================================
   レアリティごとのセクション見出し（★1 ノーマル など）
====================================================== */
@Composable
private fun RaritySectionHeader(rarity: Int) {
    val color = rarityColor(rarity)
    val label = rarityLabel(rarity)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f),
            thickness = 1.dp
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = color.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, color),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Text(
                text = "${rarityStars(rarity)}  $label",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Divider(
            modifier = Modifier.weight(1f),
            color = color.copy(alpha = 0.3f),
            thickness = 1.dp
        )
    }
}

/* ======================================================
   レアリティ表示用ヘルパー
====================================================== */

private fun rarityLabel(rarity: Int): String = when (rarity) {
    1 -> "ノーマル"
    2 -> "アンコモン"
    3 -> "レア"
    4 -> "スーパーレア"
    5 -> "ウルトラレア"
    else -> "？？？"
}

private fun rarityStars(rarity: Int): String {
    val max = 6
    val r = rarity.coerceIn(1, max)
    val filled = "★".repeat(r)
    val empty = "☆".repeat(max - r)
    return filled + empty
}

private fun rarityColor(rarity: Int): Color = when (rarity) {
    1 -> Color(0xFF6B7280) // グレー
    2 -> Color(0xFF22C55E) // グリーン
    3 -> Color(0xFF3B82F6) // ブルー
    4 -> Color(0xFF8B5CF6) // パープル
    5 -> Color(0xFFFACC15) // ゴールド
    6 -> Color(0xFFAE0014) // ピンクゴールド系（お好みで変更OK）
    else -> Color(0xFF9CA3AF)
}

/**
 * 詳細画面の枠線スタイル（レアリティ高いほど太く・濃く）
 */
private fun rarityDetailBorder(rarity: Int, baseColor: Color): Pair<Dp, Color> = when (rarity) {
    1 -> 1.dp to baseColor.copy(alpha = 0.4f)
    2 -> 1.dp to baseColor.copy(alpha = 0.7f)
    3 -> 2.dp to baseColor.copy(alpha = 0.9f)
    4 -> 2.dp to baseColor.copy(alpha = 1.0f)
    5 -> 3.dp to baseColor.copy(alpha = 1.0f)
    6 -> 3.dp to baseColor.copy(alpha = 1.0f)
    else -> 1.dp to baseColor.copy(alpha = 0.5f)
}

/**
 * 詳細画面の背景グラデーション（レア度が高いほど派手）
 */
private fun rarityDetailGradientColors(rarity: Int): List<Color> = when (rarity) {
    1 -> listOf(
        Color(0xFFF9FAFB),
        Color(0xFFF3F4F6)
    )
    2 -> listOf(
        Color(0xFFE9FDF3),
        Color(0xFFD1FAE5)
    )
    3 -> listOf(
        Color(0xFFE0F2FE),
        Color(0xFFBFDBFE)
    )
    4 -> listOf(
        Color(0xFFF3E8FF),
        Color(0xFFE9D5FF)
    )
    5 -> listOf(
        Color(0xFFFFF7E0),
        Color(0xFFFFE59E)
    )
    6 -> listOf(
        Color(0xFFFF0000),
        Color(0xFF610068)
    )
    else -> listOf(
        Color(0xFFFF0000),
        Color(0xB57A00B2)
    )
}

/* ======================================================
   グリッド用セル
====================================================== */
@Composable
private fun ZukanCell(flower: Flower, onClick: () -> Unit) {
    val borderForCard =
        if (flower.rarity >= 5) BorderStroke(2.dp, rarityColor(flower.rarity))
        else null

    Surface(
        tonalElevation = if (flower.rarity >= 5) 4.dp else 1.dp,
        shape = MaterialTheme.shapes.small,
        border = borderForCard,
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
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = flower.imageResId),
                        contentDescription = flower.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    val badgeColor = rarityColor(flower.rarity)

                    // 左上のレアリティバッジ
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = badgeColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = rarityStars(flower.rarity),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // ★ レアリティ5,6ならカード全体もキラキラ
                    if (flower.rarity >= 5) {
                        Text(
                            text = "✨",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "✨",
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
