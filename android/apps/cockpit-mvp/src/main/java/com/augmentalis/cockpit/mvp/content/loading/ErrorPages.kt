package com.augmentalis.cockpit.mvp.content.loading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.cockpit.mvp.OceanTheme

/**
 * ErrorPages - Error state UI components
 *
 * Provides error pages for various failure states:
 * - HTTP errors (404, 500, etc.)
 * - Network errors (no connection, timeout)
 * - SSL certificate errors
 * - Generic errors
 */

/**
 * Generic error page
 *
 * @param title Error title
 * @param message Error message
 * @param icon Error icon
 * @param onRetry Retry callback (null to hide retry button)
 * @param modifier Modifier for positioning
 */
@Composable
fun ErrorPage(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Error,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 400.dp)
        ) {
            // Error icon
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                modifier = Modifier.size(64.dp),
                tint = OceanTheme.error
            )

            // Error title
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = OceanTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            // Error message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.textSecondary,
                textAlign = TextAlign.Center
            )

            // Retry button
            onRetry?.let { retry ->
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = retry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanTheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

/**
 * HTTP error page (404, 500, etc.)
 *
 * @param errorCode HTTP error code
 * @param url URL that failed
 * @param onRetry Retry callback
 * @param modifier Modifier for positioning
 */
@Composable
fun HttpErrorPage(
    errorCode: Int,
    url: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (title, message, icon) = when (errorCode) {
        404 -> Triple(
            "Page Not Found",
            "The page you're looking for doesn't exist at:\n$url",
            Icons.Default.SearchOff
        )
        403 -> Triple(
            "Access Denied",
            "You don't have permission to access:\n$url",
            Icons.Default.Lock
        )
        500, 502, 503, 504 -> Triple(
            "Server Error",
            "The server is experiencing issues. Please try again later.\n\nError: HTTP $errorCode",
            Icons.Default.CloudOff
        )
        else -> Triple(
            "HTTP Error $errorCode",
            "Failed to load:\n$url",
            Icons.Default.Error
        )
    }

    ErrorPage(
        title = title,
        message = message,
        icon = icon,
        onRetry = onRetry,
        modifier = modifier
    )
}

/**
 * Network error page (no connection, timeout)
 *
 * @param url URL that failed
 * @param errorMessage Error description
 * @param onRetry Retry callback
 * @param modifier Modifier for positioning
 */
@Composable
fun NetworkErrorPage(
    url: String,
    errorMessage: String = "No internet connection",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ErrorPage(
        title = "Connection Failed",
        message = "$errorMessage\n\nCouldn't reach:\n$url",
        icon = Icons.Default.SignalWifiOff,
        onRetry = onRetry,
        modifier = modifier
    )
}

/**
 * SSL certificate error page
 *
 * @param url URL with SSL error
 * @param errorMessage SSL error description
 * @param onRetry Retry callback (should be null for SSL errors)
 * @param modifier Modifier for positioning
 */
@Composable
fun SslErrorPage(
    url: String,
    errorMessage: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OceanTheme.backgroundStart),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(32.dp)
                .widthIn(max = 400.dp)
        ) {
            // Warning icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "SSL Error",
                modifier = Modifier.size(64.dp),
                tint = Color(0xFFFFB74D)  // Amber warning color
            )

            // Error title
            Text(
                text = "Security Warning",
                style = MaterialTheme.typography.headlineSmall,
                color = OceanTheme.textPrimary,
                textAlign = TextAlign.Center
            )

            // Error message
            Text(
                text = "SSL Certificate Error:\n$errorMessage",
                style = MaterialTheme.typography.bodyMedium,
                color = OceanTheme.textSecondary,
                textAlign = TextAlign.Center
            )

            // URL
            Text(
                text = url,
                style = MaterialTheme.typography.bodySmall,
                color = OceanTheme.textTertiary,
                textAlign = TextAlign.Center
            )

            // Warning message
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                color = Color(0xFFFFB74D).copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFFFB74D)
                    )
                    Text(
                        text = "Your connection is not secure. For your safety, this page cannot be loaded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OceanTheme.textSecondary
                    )
                }
            }

            // No retry button for SSL errors (security)
            // User must fix certificate issue first
        }
    }
}

/**
 * Timeout error page
 *
 * @param url URL that timed out
 * @param onRetry Retry callback
 * @param modifier Modifier for positioning
 */
@Composable
fun TimeoutErrorPage(
    url: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ErrorPage(
        title = "Request Timed Out",
        message = "The page took too long to load:\n$url\n\nPlease check your connection and try again.",
        icon = Icons.Default.HourglassEmpty,
        onRetry = onRetry,
        modifier = modifier
    )
}

/**
 * Generic WebView error page
 *
 * Automatically selects appropriate error page based on error message.
 *
 * @param error Error message
 * @param url URL that failed
 * @param onRetry Retry callback
 * @param modifier Modifier for positioning
 */
@Composable
fun WebViewErrorPage(
    error: String,
    url: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    when {
        error.startsWith("HTTP") && error.contains("404") -> {
            HttpErrorPage(404, url, onRetry, modifier)
        }
        error.startsWith("HTTP") && error.contains("403") -> {
            HttpErrorPage(403, url, onRetry, modifier)
        }
        error.startsWith("HTTP") && error.contains("50") -> {
            val code = error.filter { it.isDigit() }.take(3).toIntOrNull() ?: 500
            HttpErrorPage(code, url, onRetry, modifier)
        }
        error.contains("SSL", ignoreCase = true) ||
        error.contains("certificate", ignoreCase = true) -> {
            SslErrorPage(url, error, null, modifier)  // No retry for SSL
        }
        error.contains("timeout", ignoreCase = true) ||
        error.contains("timed out", ignoreCase = true) -> {
            TimeoutErrorPage(url, onRetry, modifier)
        }
        error.contains("connection", ignoreCase = true) ||
        error.contains("network", ignoreCase = true) -> {
            NetworkErrorPage(url, error, onRetry, modifier)
        }
        else -> {
            ErrorPage(
                title = "Error Loading Page",
                message = "$error\n\nURL: $url",
                icon = Icons.Default.Error,
                onRetry = onRetry,
                modifier = modifier
            )
        }
    }
}
