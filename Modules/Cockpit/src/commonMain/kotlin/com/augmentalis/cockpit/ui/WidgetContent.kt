package com.augmentalis.cockpit.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Battery5Bar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.cockpit.model.FrameContent
import com.augmentalis.cockpit.model.WidgetType
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Cross-platform widget composable dispatching to 8 mini widget types.
 *
 * Designed for AR glasses where glanceable information is critical.
 * Clock, Timer, and Stopwatch are fully functional using KMP time APIs.
 * Compass, Battery, Connection, Weather, and GPS show status text with
 * graceful "unavailable" fallback on platforms without the relevant sensor APIs.
 *
 * @param content The Widget content model
 * @param modifier Compose modifier
 */
@Composable
fun WidgetContent(
    content: FrameContent.Widget,
    modifier: Modifier = Modifier
) {
    when (content.widgetType) {
        WidgetType.CLOCK -> ClockWidget(modifier)
        WidgetType.TIMER -> TimerWidget(modifier)
        WidgetType.STOPWATCH -> StopwatchWidget(modifier)
        WidgetType.COMPASS -> SensorWidget(
            icon = Icons.Default.Explore,
            label = "Compass",
            fallbackValue = "N 0.0\u00B0",
            modifier = modifier
        )
        WidgetType.BATTERY -> SensorWidget(
            icon = Icons.Default.Battery5Bar,
            label = "Battery",
            fallbackValue = "-- %",
            modifier = modifier
        )
        WidgetType.CONNECTION_STATUS -> SensorWidget(
            icon = Icons.Default.Wifi,
            label = "Connection",
            fallbackValue = "Unknown",
            modifier = modifier
        )
        WidgetType.WEATHER -> SensorWidget(
            icon = Icons.Default.WbSunny,
            label = "Weather",
            fallbackValue = "-- \u00B0C",
            modifier = modifier
        )
        WidgetType.GPS_COORDINATES -> SensorWidget(
            icon = Icons.Default.GpsFixed,
            label = "GPS",
            fallbackValue = "0.0000, 0.0000",
            modifier = modifier
        )
    }
}

/**
 * Live digital clock showing current time and date.
 */
@Composable
private fun ClockWidget(modifier: Modifier = Modifier) {
    val colors = AvanueTheme.colors
    var now by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = Clock.System.now()
            delay(1000)
        }
    }

    val localTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
    val hours = localTime.hour.toString().padStart(2, '0')
    val minutes = localTime.minute.toString().padStart(2, '0')
    val seconds = localTime.second.toString().padStart(2, '0')
    val dateStr = "${localTime.dayOfWeek.name.take(3)} ${localTime.monthNumber}/${localTime.dayOfMonth}/${localTime.year}"

    WidgetContainer(icon = Icons.Default.AccessTime, label = "Clock", modifier = modifier) {
        Text(
            text = "$hours:$minutes",
            color = colors.textPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = ":$seconds",
            color = colors.textPrimary.copy(alpha = 0.4f),
            fontSize = 20.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = dateStr,
            color = colors.textPrimary.copy(alpha = 0.5f),
            fontSize = 14.sp
        )
    }
}

/**
 * Countdown timer with start/stop/reset controls.
 */
@Composable
private fun TimerWidget(modifier: Modifier = Modifier) {
    val colors = AvanueTheme.colors
    var remainingMs by remember { mutableLongStateOf(5 * 60 * 1000L) } // 5 minute default
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && remainingMs > 0) {
            delay(100)
            remainingMs = (remainingMs - 100).coerceAtLeast(0)
            if (remainingMs == 0L) isRunning = false
        }
    }

    val totalSecs = remainingMs / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60

    WidgetContainer(icon = Icons.Default.Timer, label = "Timer", modifier = modifier) {
        Text(
            text = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}",
            color = if (remainingMs == 0L) colors.error else colors.textPrimary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = { isRunning = !isRunning }) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = colors.primary
                )
            }
            IconButton(onClick = {
                isRunning = false
                remainingMs = 5 * 60 * 1000L
            }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = colors.textPrimary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Stopwatch with start/stop/reset controls.
 */
@Composable
private fun StopwatchWidget(modifier: Modifier = Modifier) {
    val colors = AvanueTheme.colors
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(100)
            elapsedMs += 100
        }
    }

    val totalSecs = elapsedMs / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    val tenths = (elapsedMs % 1000) / 100

    WidgetContainer(icon = Icons.Default.Timer, label = "Stopwatch", modifier = modifier) {
        Text(
            text = "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}.${tenths}",
            color = colors.textPrimary,
            fontSize = 42.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = { isRunning = !isRunning }) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = colors.primary
                )
            }
            IconButton(onClick = {
                isRunning = false
                elapsedMs = 0L
            }) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Reset",
                    tint = colors.textPrimary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

/**
 * Generic sensor widget with fallback text for unavailable platform APIs.
 */
@Composable
private fun SensorWidget(
    icon: ImageVector,
    label: String,
    fallbackValue: String,
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors

    WidgetContainer(icon = icon, label = label, modifier = modifier) {
        Text(
            text = fallbackValue,
            color = colors.textPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Live data requires platform sensor API",
            color = colors.textPrimary.copy(alpha = 0.3f),
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Common widget container with icon header and centered content.
 */
@Composable
private fun WidgetContainer(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val colors = AvanueTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                color = colors.textPrimary.copy(alpha = 0.4f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}
