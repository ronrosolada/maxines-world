package com.maxinesworld.featurelessonplayer

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.maxinesworld.coredesignsystem.components.MaxinesPrimaryButton
import com.maxinesworld.coredesignsystem.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QuickBitsScreen(
    state: QuickBitsUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onPlayVideo: (QuickBitItemUi) -> Unit,
    onStopPlaying: () -> Unit,
    onDownloadSingle: (QuickBitItemUi) -> Unit,
    onDownloadAll: () -> Unit,
    onClearDownloads: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filteredItems = remember(state.items, state.selectedCategory) {
        if (state.selectedCategory == "all") {
            state.items
        } else {
            state.items.filter { it.item.category.equals(state.selectedCategory, ignoreCase = true) }
        }
    }

    var showDownloadDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (filteredItems.isEmpty()) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SunshineGold)
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "No Quick Bits available in this category.",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onSelectCategory("all") },
                            colors = ButtonDefaults.buttonColors(containerColor = VillageTeal)
                        ) {
                            Text("Show All Bits", color = White)
                        }
                    }
                }
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { filteredItems.size })

            // Vertical TikTok-style Pager
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { page -> filteredItems.getOrNull(page)?.item?.id ?: page }
            ) { page ->
                val itemUi = filteredItems[page]
                val isPageActive = pagerState.currentPage == page

                QuickBitTikTokPage(
                    itemUi = itemUi,
                    isPageActive = isPageActive,
                    onDownloadClick = { onDownloadSingle(itemUi) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Navigation & Filter Bar Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.40f),
                            Color.Transparent
                        )
                    )
                )
                .padding(top = 12.dp, bottom = 28.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .semantics {
                                contentDescription = "Back to Home"
                                role = Role.Button
                            }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = White
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Quick Bits",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = White
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = SunshineGold,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "${state.items.size.coerceAtLeast(60)} Shorts",
                                color = DeepNight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Bulk Download Status / Trigger Button
                    Surface(
                        onClick = { showDownloadDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (state.bulkProgress.isRunning) SunshineGold else VillageTeal.copy(alpha = 0.9f),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            if (state.bulkProgress.isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = DeepNight
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${state.bulkProgress.completedVideos}/${state.bulkProgress.totalVideos}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DeepNight
                                )
                            } else {
                                val readyCount = state.items.count { it.isDownloaded }
                                Icon(
                                    imageVector = if (readyCount == state.items.size && state.items.isNotEmpty()) {
                                        Icons.Default.CheckCircle
                                    } else {
                                        Icons.Default.Download
                                    },
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (readyCount == state.items.size && state.items.isNotEmpty()) "All Offline" else "Download All",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = White
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Category Filter Pills
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.categories) { cat ->
                        val isSelected = state.selectedCategory.equals(cat, ignoreCase = true)
                        val label = when (cat.lowercase()) {
                            "all" -> "All Shorts"
                            "animals" -> "🐾 Animals"
                            "space" -> "🚀 Space"
                            "science" -> "🔬 Science"
                            "math" -> "🔢 Math"
                            else -> cat.replaceFirstChar { it.uppercase() }
                        }
                        Surface(
                            onClick = { onSelectCategory(cat) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) SunshineGold else Color.White.copy(alpha = 0.20f),
                            contentColor = if (isSelected) DeepNight else White,
                            modifier = Modifier.height(34.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Swipe up/down hint indicator (fades out after initial view)
        var showSwipeHint by remember { mutableStateOf(true) }
        LaunchedEffect(Unit) {
            delay(4000)
            showSwipeHint = false
        }

        AnimatedVisibility(
            visible = showSwipeHint && filteredItems.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                contentColor = White
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.SwipeVertical,
                        contentDescription = null,
                        tint = SunshineGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Swipe up or down for next video",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Bulk Download Management Modal Dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.OfflinePin, contentDescription = null, tint = VillageTeal)
                    Spacer(Modifier.width(8.dp))
                    Text("Offline Videos Manager", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                val downloadedCount = state.items.count { it.isDownloaded }
                val totalCount = state.items.size
                val downloadedMb = state.downloadedStorageBytes / (1024.0 * 1024.0)
                val totalMb = state.totalStorageBytes / (1024.0 * 1024.0)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Download all ${state.items.size} educational videos to watch anytime without internet.",
                        fontSize = 14.sp,
                        color = Ink
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Cream,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Downloaded: $downloadedCount / $totalCount videos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Ink
                            )
                            Text(
                                String.format("%.1f MB / %.1f MB total", downloadedMb, totalMb),
                                fontSize = 13.sp,
                                color = Ink.copy(alpha = 0.65f)
                            )
                            if (state.bulkProgress.isRunning) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = {
                                        if (state.bulkProgress.totalBytes > 0) {
                                            (state.bulkProgress.downloadedBytes.toFloat() / state.bulkProgress.totalBytes.toFloat())
                                        } else 0f
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = VillageTeal,
                                    trackColor = Color.LightGray
                                )
                                state.bulkProgress.currentItemTitle?.let { title ->
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Downloading: $title",
                                        fontSize = 11.sp,
                                        color = VillageTeal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        onDownloadAll()
                    },
                    enabled = !state.bulkProgress.isRunning && state.items.any { !it.isDownloaded },
                    colors = ButtonDefaults.buttonColors(containerColor = VillageTeal)
                ) {
                    Text("Download All (253 MB)", color = White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Close", color = Ink)
                }
            }
        )
    }
}

@Composable
private fun QuickBitTikTokPage(
    itemUi: QuickBitItemUi,
    isPageActive: Boolean,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isPlaying by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var isMuted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var savedPosition by androidx.compose.runtime.saveable.rememberSaveable(itemUi.item.id) { androidx.compose.runtime.mutableLongStateOf(0L) }

    // ExoPlayer dedicated to this page
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
        }
    }

    // Set Media URI: Local offline file takes priority over LAN streaming URL
    LaunchedEffect(itemUi.isDownloaded, itemUi.localFile) {
        val uri = if (itemUi.isDownloaded && itemUi.localFile != null && itemUi.localFile.exists()) {
            Uri.fromFile(itemUi.localFile)
        } else {
            Uri.parse(itemUi.item.videoUrl)
        }
        player.setMediaItem(MediaItem.fromUri(uri))
        if (savedPosition > 0L) {
            player.seekTo(savedPosition)
        }
        player.prepare()
    }

    // Control Playback on scroll/page activation
    LaunchedEffect(isPageActive) {
        if (isPageActive) {
            player.playWhenReady = true
            player.play()
            isPlaying = true
        } else {
            player.pause()
            player.seekTo(0)
            isPlaying = false
        }
    }

    DisposableEffect(player) {
        onDispose {
            savedPosition = player.currentPosition
            player.stop()
            player.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable {
                if (player.isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.play()
                    isPlaying = true
                }
            }
    ) {
        // Fullscreen ExoPlayer View
        AndroidView(
            factory = { ctx ->
                (LayoutInflater.from(ctx)
                    .inflate(R.layout.view_offline_video_player, null, false) as PlayerView).apply {
                    this.player = player
                    useController = false
                    contentDescription = itemUi.item.title
                }
            },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize()
        )

        // Pause indicator overlay
        if (!isPlaying && isPageActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Paused",
                            tint = White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }

        // Bottom Info Gradient Overlay with Emil Kowalski Stagger Animation
        val overlayAlpha by animateFloatAsState(
            targetValue = if (isPageActive) 1f else 0f,
            animationSpec = tween(durationMillis = 350, easing = LinearOutSlowInEasing),
            label = "OverlayAlpha"
        )
        val overlaySlide by animateFloatAsState(
            targetValue = if (isPageActive) 0f else 30f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "OverlaySlide"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    alpha = overlayAlpha
                    translationY = overlaySlide
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.90f)
                        )
                    )
                )
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp, top = 64.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Title, Creator, Category, and Offline Badge
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (catColor, catLabel) = when (itemUi.item.category.lowercase()) {
                            "animals" -> Coral to "Animals"
                            "space" -> StoryPurple to "Space"
                            "science" -> SkyBlue to "Science"
                            "math" -> SuccessGreen to "Math"
                            else -> SunshineGold to itemUi.item.category
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                catLabel,
                                color = White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (itemUi.isDownloaded) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        "Offline Ready",
                                        color = White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = itemUi.item.title,
                        color = White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${itemUi.item.channel}",
                            color = White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${itemUi.item.durationSeconds / 60}m ${itemUi.item.durationSeconds % 60}s",
                            color = White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                // Right side: Single Download Action Button
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!itemUi.isDownloaded) {
                        IconButton(
                            onClick = onDownloadClick,
                            enabled = !itemUi.isDownloading,
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            if (itemUi.isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = SunshineGold
                                )
                            } else {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download this video",
                                    tint = SunshineGold,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
