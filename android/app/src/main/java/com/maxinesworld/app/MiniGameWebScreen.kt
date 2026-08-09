package com.maxinesworld.app

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.SystemClock
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.maxinesworld.coredesignsystem.theme.Cream
import com.maxinesworld.coredesignsystem.theme.Ink
import com.maxinesworld.coredesignsystem.theme.VillageTeal
import com.maxinesworld.engineminigame.MiniGameResult
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

private const val OFFLINE_HOST = "maxines-world.local"
private const val EMPTY_RESPONSE_TYPE = "text/plain"

/** Reads only a catalog-approved HTML asset. No arbitrary path is accepted. */
internal fun embeddedMiniGameHtml(
    context: android.content.Context,
    game: EmbeddedMiniGame,
): String? = runCatching {
    context.assets.open("${MiniGameCatalog.ASSET_ROOT}/${game.slug}.html").use { input ->
        String(input.readBytes(), StandardCharsets.UTF_8)
    }
}.getOrNull()

internal fun embeddedMiniGameResult(
    game: EmbeddedMiniGame,
    childId: String,
    rewardBreakId: String,
    startedAtEpochMillis: Long,
    endedAtEpochMillis: Long,
): MiniGameResult = MiniGameResult(
    rewardBreakId = rewardBreakId,
    gameId = game.gameId,
    childId = childId,
    startedAtEpochMillis = startedAtEpochMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    roundsCompleted = 0,
    correctOrders = 0,
    pawTokensEarned = 0,
    collectibleId = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniGameWebScreen(
    childId: String,
    rewardBreakId: String,
    gameSlug: String,
    durationMillis: Long,
    onExit: (MiniGameResult) -> Unit,
    onBack: () -> Unit,
) {
    val game = remember(gameSlug) { MiniGameCatalog.find(gameSlug) }
    if (game == null) {
        MiniGameUnavailable(message = "This game is not available offline.", onBack = onBack)
        return
    }
    if (durationMillis <= 0L) {
        MiniGameUnavailable(message = "This reward break has finished.", onBack = onBack)
        return
    }

    val context = LocalContext.current
    val html = remember(game.slug) { embeddedMiniGameHtml(context, game) }
    if (html == null) {
        MiniGameUnavailable(message = "This game is not available offline.", onBack = onBack)
        return
    }

    val startedAt = rememberSaveable(rewardBreakId, game.gameId) { System.currentTimeMillis() }
    val effectiveDuration = durationMillis
    var remainingMillis by remember(effectiveDuration) { mutableLongStateOf(effectiveDuration) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }
    var resultSubmitted by rememberSaveable(rewardBreakId, game.gameId) { mutableStateOf(false) }

    fun submitResult() {
        if (resultSubmitted) return
        resultSubmitted = true
        onExit(
            embeddedMiniGameResult(
                game = game,
                childId = childId,
                rewardBreakId = rewardBreakId,
                startedAtEpochMillis = startedAt,
                endedAtEpochMillis = System.currentTimeMillis(),
            )
        )
    }

    LaunchedEffect(effectiveDuration, resultSubmitted) {
        val deadline = SystemClock.elapsedRealtime() + effectiveDuration
        while (!resultSubmitted) {
            val remaining = (deadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            remainingMillis = remaining
            if (remaining == 0L) {
                submitResult()
                break
            }
            kotlinx.coroutines.delay(250L)
        }
    }

    BackHandler(enabled = !resultSubmitted) {
        showExitConfirmation = true
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(game.title) },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmation = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Leave game")
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = "Time remaining", tint = VillageTeal)
                        Text(
                            formatRemaining(remainingMillis),
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = VillageTeal,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(paddingValues),
        ) {
            MiniGameWebView(
                html = html,
                title = game.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    if (showExitConfirmation && !resultSubmitted) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            icon = { Icon(Icons.Default.Close, contentDescription = null, tint = VillageTeal) },
            title = { Text("Leave this game?") },
            text = { Text("Your reward break will stay available for another game.") },
            confirmButton = {
                TextButton(onClick = ::submitResult) { Text("Leave game") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Keep playing") }
            },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MiniGameWebView(
    // These are pinned, bundled game pages. CSP and OfflineMiniGameClient block network access.
    html: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.semantics { contentDescription = "Mini-game: $title" },
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowContentAccess = false
                settings.allowFileAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.mediaPlaybackRequiresUserGesture = true

                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
                isHorizontalScrollBarEnabled = false
                webViewClient = OfflineMiniGameClient()
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(
                    "https://$OFFLINE_HOST/",
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        },
    )
}

private class OfflineMiniGameClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = true

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url
        val isAllowed = url.scheme == "data" || url.scheme == "about" || url.host == OFFLINE_HOST
        return if (isAllowed) {
            null
        } else {
            WebResourceResponse(
                EMPTY_RESPONSE_TYPE,
                "UTF-8",
                ByteArrayInputStream(ByteArray(0)),
            )
        }
    }
}

@Composable
private fun MiniGameUnavailable(
    message: String,
    onBack: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Cream),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🎮", style = MaterialTheme.typography.displaySmall)
            Text(message, color = Ink)
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSeconds = ((millis + 999L) / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = (totalSeconds % 60L).toString().padStart(2, '0')
    return "$minutes:$seconds"
}
