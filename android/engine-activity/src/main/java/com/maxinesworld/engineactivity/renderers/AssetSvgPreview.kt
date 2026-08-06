package com.maxinesworld.engineactivity.renderers

import android.graphics.Color as AndroidColor
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

private const val VECTOR_ASSET_ROOT = "content-pack/month-01/assets/vectors"
private val SAFE_ASSET_ID = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

/** Resolves only a content-pack asset id, never an arbitrary filesystem path. */
internal fun svgAssetPath(assetId: String): String? =
    assetId.takeIf { SAFE_ASSET_ID.matches(it) }
        ?.let { "$VECTOR_ASSET_ROOT/$it.svg" }

/**
 * Displays a bundled lesson SVG without granting it script or network access.
 * The composable is deliberately a fallback layer: if an asset cannot be
 * resolved, the caller's accessible text/controls remain visible.
 */
@Composable
internal fun AssetSvgPreview(
    assetId: String,
    modifier: Modifier = Modifier,
) {
    val assetPath = svgAssetPath(assetId) ?: return

    AndroidView(
        modifier = modifier.semantics {
            contentDescription = "Lesson picture"
        },
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                settings.allowContentAccess = false
                settings.allowFileAccess = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(false)
                settings.builtInZoomControls = false
                settings.displayZoomControls = false
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                contentDescription = "Lesson picture"
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean = true

                    override fun shouldInterceptRequest(
                        view: WebView,
                        request: WebResourceRequest,
                    ): WebResourceResponse? {
                        val scheme = request.url.scheme?.lowercase()
                        return if (scheme == "http" || scheme == "https") {
                            WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                ByteArrayInputStream(ByteArray(0)),
                            )
                        } else {
                            null
                        }
                    }
                }
            }
        },
        update = { webView ->
            if (webView.tag != assetPath) {
                webView.tag = assetPath
                val svg = runCatching {
                    webView.context.assets.open(assetPath).use { input ->
                        String(input.readBytes(), StandardCharsets.UTF_8)
                    }
                }.getOrNull()
                if (svg != null) {
                    // Inline the verified, bundled SVG. This avoids relying on
                    // the WebView's initial file URL navigation while keeping
                    // JavaScript and network access disabled.
                    webView.loadDataWithBaseURL(
                        "file:///android_asset/$VECTOR_ASSET_ROOT/",
                        svg,
                        "image/svg+xml",
                        "UTF-8",
                        null,
                    )
                }
            }
        },
    )
}