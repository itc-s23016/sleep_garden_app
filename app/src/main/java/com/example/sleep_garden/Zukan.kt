package com.example.sleep_garden

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.sleep_garden.data.Flower
import com.example.sleep_garden.data.FlowerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Zukan(
    onBack: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    // ✅ ActivityスコープでViewModelを共有
    val vm: FlowerViewModel = viewModel(viewModelStoreOwner = ctx as ComponentActivity)

    // 🔙 戻る処理
    val handleBack: () -> Unit = onBack ?: run {
        {
            val owner = ctx as? OnBackPressedDispatcherOwner
            if (owner != null) owner.onBackPressedDispatcher.onBackPressed()
            else if (ctx is Activity) ctx.finish()
        }
    }

    val flowers by vm.flowers.collectAsState()
    var selected by remember { mutableStateOf<Flower?>(null) }

    // 🌱 起動時にDBから読込
    LaunchedEffect(Unit) {
        vm.refresh()
        println("🌸 DB中の花数 = ${vm.flowers.value.size}")
        vm.flowers.value.forEach { println("→ ${it.name}, found=${it.found}") }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("花図鑑") }) }
    ) { pad ->
        Box(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {

            // 🌼 一覧 or 空表示
            if (flowers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("データがありません（${flowers.size}件）")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(flowers, key = { it.id ?: it.name.hashCode().toLong() }) { f ->
                        ZukanCell(f) { selected = f }
                    }
                }
            }

            // 🌸 詳細オーバーレイ
            if (selected != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable { selected = null }
                )
            }

            // 🌼 詳細カード
            selected?.let { f ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f)
                        .heightIn(min = 280.dp, max = 620.dp)
                ) {
                    val scroll = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .verticalScroll(scroll)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 画像
                        Surface(
                            tonalElevation = 2.dp,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        ) {
                            val img = rememberUrlImageBitmap(f.imageUrl)
                            when {
                                f.imageUrl.isNullOrBlank() -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("Null", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                img.value != null -> {
                                    Image(
                                        bitmap = img.value!!,
                                        contentDescription = f.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                else -> {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(f.name)
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                f.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text("☆${f.rarity}", style = MaterialTheme.typography.titleMedium)
                        }

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

            // 🔙 戻るボタン
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

@Composable
private fun ZukanCell(flower: Flower, onClick: () -> Unit) {
    val img = rememberUrlImageBitmap(flower.imageUrl)
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
                    Text("？", style = MaterialTheme.typography.headlineLarge)
                }
            }
            img.value != null -> {
                Image(
                    bitmap = img.value!!,
                    contentDescription = flower.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            flower.imageUrl.isNullOrBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Null")
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ---- URL画像ローダ（Coilなし簡易版） ----
private val bitmapCache = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
}

@Composable
private fun rememberUrlImageBitmap(url: String?): State<ImageBitmap?> {
    val state = remember(url) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            state.value = null
            return@LaunchedEffect
        }

        bitmapCache.get(url)?.let {
            state.value = it.asImageBitmap()
            return@LaunchedEffect
        }

        val bmp = withContext(Dispatchers.IO) {
            try {
                val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 5000
                    readTimeout = 5000
                    instanceFollowRedirects = true
                }
                conn.inputStream.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                null
            }
        }

        bmp?.let {
            bitmapCache.put(url, it)
            state.value = it.asImageBitmap()
        }
    }
    return state
}
