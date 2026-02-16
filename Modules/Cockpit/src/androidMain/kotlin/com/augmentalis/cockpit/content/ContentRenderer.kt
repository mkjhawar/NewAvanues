package com.augmentalis.cockpit.content

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.annotationavanue.AnnotationCanvas
import com.augmentalis.annotationavanue.SignatureCapture
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cameraavanue.CameraPreview
import com.augmentalis.cockpit.model.CockpitFrame
import com.augmentalis.cockpit.model.FrameContent
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
@Composable
fun ContentRenderer(
    frame: CockpitFrame,
    onContentStateChanged: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Box(modifier = modifier.fillMaxSize().background(colors.background)) {
        when (val content = frame.content) {
            is FrameContent.Web -> WebContentRenderer(
                url = content.url,
                onUrlChanged = { newUrl ->
                    onContentStateChanged(frame.id, """{"url":"$newUrl"}""")
                }
            )

            is FrameContent.Pdf -> PdfViewer(
                uri = content.uri,
                initialPage = content.currentPage,
                onPageChanged = { page ->
                    onContentStateChanged(frame.id, """{"uri":"${content.uri}","currentPage":$page,"zoom":${content.zoom}}""")
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Image -> ImageViewer(
                uri = content.uri,
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Video -> VideoPlayer(
                uri = content.uri,
                autoPlay = content.isPlaying,
                initialPositionMs = content.playbackPositionMs,
                onPositionChanged = { posMs ->
                    onContentStateChanged(frame.id, """{"uri":"${content.uri}","playbackPositionMs":$posMs}""")
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Note -> NoteEditor(
                initialTitle = frame.title,
                initialContent = content.text,
                onSave = { title, text ->
                    onContentStateChanged(frame.id, """{"text":"${text.replace("\"", "\\\"")}"}""")
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Camera -> CameraPreview(
                onPhotoCaptured = { uri ->
                    onContentStateChanged(frame.id, """{"lastCapturedUri":"$uri"}""")
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.VoiceNote -> VoiceNoteRenderer(
                transcription = content.transcription,
                isRecording = content.isRecording,
                durationMs = content.durationMs,
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Signature -> SignatureCapture(
                onComplete = { strokes ->
                    onContentStateChanged(frame.id, """{"strokeCount":${strokes.size}}""")
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Whiteboard -> AnnotationCanvas(
                onStrokesChanged = { strokes ->
                    onContentStateChanged(frame.id, """{"strokeCount":${strokes.size}}""")
                },
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.ScreenCast -> CastOverlay(
                castState = CastState(),
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Form -> PlaceholderContent(
                label = "Form",
                description = "Form builder — coming soon",
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Map -> PlaceholderContent(
                label = "Map",
                description = "Map integration — coming soon",
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Terminal -> PlaceholderContent(
                label = "Terminal",
                description = "Terminal emulator — coming soon",
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.AiSummary -> PlaceholderContent(
                label = "AI Summary",
                description = "AI summarization — coming soon",
                modifier = Modifier.fillMaxSize()
            )

            is FrameContent.Widget -> PlaceholderContent(
                label = "Widget: ${content.widgetType.name}",
                description = "Widget display — coming soon",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Web content renderer using WebAvanue's WebView infrastructure.
 */
@Composable
private fun WebContentRenderer(
    url: String,
    onUrlChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            android.webkit.WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, finishedUrl: String?) {
                        finishedUrl?.let { onUrlChanged(it) }
                    }
                }
                loadUrl(url.ifBlank { "about:blank" })
            }
        },
        update = { webView ->
            val currentUrl = webView.url
            if (currentUrl != url && url.isNotBlank()) {
                webView.loadUrl(url)
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

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
            color = if (isRecording) colors.error else colors.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        Text(
            text = String.format("%02d:%02d", minutes, secs),
            color = colors.onBackground.copy(alpha = 0.7f),
            fontSize = 32.sp,
            fontWeight = FontWeight.Light
        )

        if (transcription.isNotBlank()) {
            Text(
                text = transcription,
                color = colors.onBackground.copy(alpha = 0.8f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

/**
 * Placeholder for content types that will be implemented in future phases.
 */
@Composable
private fun PlaceholderContent(
    label: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = colors.onBackground.copy(alpha = 0.6f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = description,
                color = colors.onBackground.copy(alpha = 0.4f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
