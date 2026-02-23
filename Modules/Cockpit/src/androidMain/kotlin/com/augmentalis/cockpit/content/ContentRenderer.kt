package com.augmentalis.cockpit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.augmentalis.cockpit.ui.ContentAction
import kotlinx.coroutines.flow.SharedFlow
import com.augmentalis.annotationavanue.AnnotationCanvas
import com.augmentalis.annotationavanue.SignatureCapture
import com.augmentalis.annotationavanue.controller.AnnotationSerializer
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.photoavanue.CameraPreview
import com.augmentalis.cockpit.AndroidExternalAppResolver
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.ui.AiSummaryContent
import com.augmentalis.cockpit.ui.ExternalAppContent
import com.augmentalis.cockpit.ui.FormContent
import com.augmentalis.cockpit.ui.TerminalContent
import com.augmentalis.cockpit.ui.WidgetContent
import com.augmentalis.imageavanue.ImageViewer
import com.augmentalis.noteavanue.NoteEditor
import com.augmentalis.pdfavanue.PdfViewer
import com.augmentalis.remotecast.CastOverlay
import com.augmentalis.remotecast.model.CastState
import com.augmentalis.videoavanue.VideoPlayer

/**
 * Master content renderer that dispatches to the appropriate Avanue module
 * Composable based on the frame's content type.
 *
 * Each content type delegates to its standalone module:
 * - Web -> WebAvanue (inline WebView)
 * - PDF -> PDFAvanue (PdfViewer)
 * - Image -> ImageAvanue (ImageViewer)
 * - Video -> VideoAvanue (VideoPlayer)
 * - Note -> NoteAvanue (NoteEditor)
 * - Camera -> CameraAvanue (CameraPreview)
 * - Whiteboard/Signature -> AnnotationAvanue (AnnotationCanvas/SignatureCapture)
 * - ScreenCast -> RemoteCast (CastOverlay)
 */
/**
 * @param contentActionFlow Optional flow of content-specific actions from the
 *   CommandBar. Only actions relevant to this frame's content type are handled;
 *   all others are silently ignored. The flow is collected inside a LaunchedEffect.
 */
@Composable
fun ContentRenderer(
    frame: CockpitFrame,
    onContentStateChanged: (String, FrameContent) -> Unit = { _, _ -> },
    contentActionFlow: SharedFlow<ContentAction>? = null,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        when (val content = frame.content) {
            is FrameContent.Web -> WebContentRenderer(
                url = content.url,
                onUrlChanged = { newUrl ->
                    onContentStateChanged(frame.id, content.copy(url = newUrl))
                },
                contentActionFlow = contentActionFlow
            )

            is FrameContent.Pdf -> {
                // Track current page for content action dispatch (prev/next page)
                val pdfPage = remember { mutableStateOf(content.currentPage) }

                if (contentActionFlow != null) {
                    LaunchedEffect(contentActionFlow) {
                        contentActionFlow.collect { action ->
                            when (action) {
                                ContentAction.PDF_PREV_PAGE -> {
                                    val newPage = (pdfPage.value - 1).coerceAtLeast(0)
                                    pdfPage.value = newPage
                                    onContentStateChanged(frame.id, content.copy(currentPage = newPage))
                                }
                                ContentAction.PDF_NEXT_PAGE -> {
                                    val newPage = pdfPage.value + 1
                                    pdfPage.value = newPage
                                    onContentStateChanged(frame.id, content.copy(currentPage = newPage))
                                }
                                else -> { /* Not a PDF action */ }
                            }
                        }
                    }
                }

                PdfViewer(
                    uri = content.uri,
                    initialPage = pdfPage.value,
                    onPageChanged = { page ->
                        pdfPage.value = page
                        onContentStateChanged(frame.id, content.copy(currentPage = page))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is FrameContent.Image -> {
                val zoom = remember { mutableFloatStateOf(1f) }
                val rotation = remember { mutableFloatStateOf(0f) }

                if (contentActionFlow != null) {
                    LaunchedEffect(contentActionFlow) {
                        contentActionFlow.collect { action ->
                            when (action) {
                                ContentAction.IMAGE_ZOOM_IN -> zoom.floatValue = (zoom.floatValue * 1.25f).coerceAtMost(5f)
                                ContentAction.IMAGE_ZOOM_OUT -> zoom.floatValue = (zoom.floatValue / 1.25f).coerceAtLeast(0.2f)
                                ContentAction.IMAGE_ROTATE -> rotation.floatValue = (rotation.floatValue + 90f) % 360f
                                else -> { /* Not an image action */ }
                            }
                        }
                    }
                }

                ImageViewer(
                    uri = content.uri,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom.floatValue
                            scaleY = zoom.floatValue
                            rotationZ = rotation.floatValue
                        }
                )
            }

            is FrameContent.Video -> {
                val isPlaying = remember { mutableStateOf(content.isPlaying) }

                if (contentActionFlow != null) {
                    LaunchedEffect(contentActionFlow) {
                        contentActionFlow.collect { action ->
                            when (action) {
                                ContentAction.VIDEO_PLAY_PAUSE -> isPlaying.value = !isPlaying.value
                                ContentAction.VIDEO_REWIND -> {
                                    val newPos = (content.playbackPositionMs - 10_000L).coerceAtLeast(0L)
                                    onContentStateChanged(frame.id, content.copy(playbackPositionMs = newPos))
                                }
                                ContentAction.VIDEO_FULLSCREEN -> { /* Layout concern — no-op at content level */ }
                                else -> { /* Not a video action */ }
                            }
                        }
                    }
                }

                VideoPlayer(
                    uri = content.uri,
                    autoPlay = isPlaying.value,
                    initialPositionMs = content.playbackPositionMs,
                    onPositionChanged = { posMs ->
                        onContentStateChanged(frame.id, content.copy(playbackPositionMs = posMs))
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is FrameContent.Note -> NoteEditor(
                initialTitle = frame.title,
                initialContent = content.markdownContent,
                onSave = { _, markdownContent ->
                    onContentStateChanged(frame.id, content.copy(markdownContent = markdownContent))
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Camera -> CameraPreview(
                onPhotoCaptured = { _ ->
                    // Photo capture is a side-effect (saved to gallery/attachment storage).
                    // Camera content tracks lens/flash/zoom settings, not captured URIs.
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.VoiceNote -> VoiceNoteRenderer(
                transcription = content.transcript,
                isRecording = content.isRecording,
                durationMs = content.durationMs,
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Voice -> VoiceNoteRenderer(
                transcription = "",
                isRecording = content.isRecording,
                durationMs = content.durationMs,
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Signature -> SignatureCapture(
                onComplete = { strokes ->
                    onContentStateChanged(frame.id, content.copy(
                        isSigned = strokes.isNotEmpty(),
                        signatureData = AnnotationSerializer.strokesToJson(strokes)
                    ))
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Whiteboard -> AnnotationCanvas(
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.ScreenCast -> CastOverlay(
                castState = CastState(
                    deviceId = content.sourceDeviceId,
                    deviceName = content.sourceDeviceName,
                    isConnected = content.isConnected
                ),
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Form -> FormContent(
                content = content,
                onContentStateChanged = { updatedForm ->
                    onContentStateChanged(frame.id, updatedForm)
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Map -> MapContentRenderer(
                latitude = content.latitude,
                longitude = content.longitude,
                zoomLevel = content.zoomLevel.toInt(),
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Terminal -> TerminalContent(
                content = content,
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.AiSummary -> AiSummaryContent(
                content = content,
                onGenerateSummary = {
                    // AI module integration deferred — Modules/AI:LLM not yet available.
                    // Reflect the unavailability in the content state so the UI can show it.
                    onContentStateChanged(
                        frame.id,
                        content.copy(summary = "AI summary generation is not available yet.")
                    )
                },
                onContentStateChanged = { updatedSummary ->
                    onContentStateChanged(frame.id, updatedSummary)
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Widget -> WidgetContent(
                content = content,
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.ExternalApp -> {
                val context = LocalContext.current
                val resolver = remember { AndroidExternalAppResolver(context) }
                val status = remember(content.packageName) {
                    resolver.resolveApp(content.packageName)
                }
                ExternalAppContent(
                    content = content,
                    status = status,
                    onLaunchAdjacent = {
                        resolver.launchAdjacent(content.packageName, content.activityName)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Web content renderer using WebAvanue's WebView infrastructure.
 *
 * Handles content actions: WEB_BACK, WEB_FORWARD, WEB_REFRESH, WEB_ZOOM_IN, WEB_ZOOM_OUT.
 * The WebView reference is retained via [remember] + [mutableStateOf] so that
 * the LaunchedEffect can dispatch actions to the live platform view.
 */
@Composable
private fun WebContentRenderer(
    url: String,
    onUrlChanged: (String) -> Unit,
    contentActionFlow: SharedFlow<ContentAction>? = null,
    modifier: Modifier = Modifier
) {
    val webViewRef = remember { mutableStateOf<android.webkit.WebView?>(null) }

    // Collect content actions and route to WebView
    if (contentActionFlow != null) {
        LaunchedEffect(contentActionFlow) {
            contentActionFlow.collect { action ->
                val wv = webViewRef.value ?: return@collect
                when (action) {
                    ContentAction.WEB_BACK -> if (wv.canGoBack()) wv.goBack()
                    ContentAction.WEB_FORWARD -> if (wv.canGoForward()) wv.goForward()
                    ContentAction.WEB_REFRESH -> wv.reload()
                    ContentAction.WEB_ZOOM_IN -> wv.zoomIn()
                    ContentAction.WEB_ZOOM_OUT -> wv.zoomOut()
                    else -> { /* Not a web action — ignore */ }
                }
            }
        }
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, finishedUrl: String?) {
                        if (finishedUrl != null && finishedUrl.isSafeWebUrl()) {
                            onUrlChanged(finishedUrl)
                        }
                    }
                }
                val safeUrl = url.ifBlank { "about:blank" }
                loadUrl(if (safeUrl.isSafeWebUrl()) safeUrl else "about:blank")
                webViewRef.value = this
            }
        },
        update = { webView ->
            webViewRef.value = webView
            val currentUrl = webView.url
            if (currentUrl != url && url.isNotBlank() && url.isSafeWebUrl()) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/** URL scheme allowlist — rejects javascript:, data:, file:, content: schemes. */
private fun String.isSafeWebUrl(): Boolean =
    startsWith("https://") || startsWith("http://") || this == "about:blank"

/**
 * Voice note renderer showing recording state, waveform, and transcription.
 */
@Composable
private fun VoiceNoteRenderer(
    transcription: String,
    isRecording: Boolean,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (isRecording) "Recording..." else "Voice Note",
            color = if (isRecording) colors.error else colors.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        Text(
            text = String.format("%02d:%02d", minutes, secs),
            color = colors.textPrimary.copy(alpha = 0.7f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Light
        )

        if (transcription.isNotBlank()) {
            Text(
                text = transcription,
                color = colors.textPrimary.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * Map content renderer using OpenStreetMap via embedded WebView.
 *
 * Loads an OpenStreetMap embed URL with the given coordinates and zoom level.
 * This approach avoids Google Maps SDK dependency and works across all Android devices.
 */
@Composable
private fun MapContentRenderer(
    latitude: Double,
    longitude: Double,
    zoomLevel: Int,
    modifier: Modifier = Modifier
) {
    val mapUrl = remember(latitude, longitude, zoomLevel) {
        val bbox = calculateBoundingBox(latitude, longitude, zoomLevel)
        "https://www.openstreetmap.org/export/embed.html" +
            "?bbox=${bbox.west},${bbox.south},${bbox.east},${bbox.north}" +
            "&layer=mapnik&marker=$latitude,$longitude"
    }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: android.webkit.WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): Boolean {
                        val host = request?.url?.host ?: return true
                        return !host.endsWith("openstreetmap.org")
                    }
                }
                loadUrl(mapUrl)
            }
        },
        update = { webView ->
            val currentUrl = webView.url
            if (currentUrl != mapUrl) {
                webView.loadUrl(mapUrl)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Bounding box for OpenStreetMap embed URL calculation.
 */
private data class MapBBox(
    val west: Double,
    val south: Double,
    val east: Double,
    val north: Double
)

/**
 * Approximate bounding box from center coordinates and zoom level.
 * Higher zoom = smaller bbox = more detail.
 */
private fun calculateBoundingBox(lat: Double, lon: Double, zoom: Int): MapBBox {
    // Each zoom level halves the degrees shown; zoom 10 ≈ ±0.1°
    val span = 360.0 / (1 shl zoom.coerceIn(1, 18))
    return MapBBox(
        west = lon - span,
        south = lat - span / 2,
        east = lon + span,
        north = lat + span / 2
    )
}
