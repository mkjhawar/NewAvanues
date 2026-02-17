package com.augmentalis.annotationavanue

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.augmentalis.annotationavanue.model.StrokePoint
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * Simplified signature capture UI — pen-only drawing with clear/done controls.
 * Designed for quick signature or initials input within a Cockpit frame.
 *
 * @param onComplete Called with the list of strokes when the user taps "Done".
 * @param onClear Called when the user clears the signature.
 * @param modifier Layout modifier.
 */
@Composable
fun SignatureCapture(
    onComplete: (List<com.augmentalis.annotationavanue.model.Stroke>) -> Unit = {},
    onClear: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = AvanueTheme.colors
    var strokes by remember { mutableStateOf<List<com.augmentalis.annotationavanue.model.Stroke>>(emptyList()) }
    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }
    val penColor = colors.textPrimary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Sign below",
            color = colors.textPrimary.copy(alpha = 0.6f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(12.dp))

        // Signature area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                .background(colors.surface, RoundedCornerShape(8.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPoints = listOf(StrokePoint(offset.x, offset.y))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPoints = currentPoints + StrokePoint(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                if (currentPoints.size >= 2) {
                                    val newStroke = com.augmentalis.annotationavanue.model.Stroke(
                                        points = currentPoints,
                                        color = penColor.value.toLong(),
                                        width = 3f,
                                        tool = com.augmentalis.annotationavanue.model.AnnotationTool.PEN
                                    )
                                    strokes = strokes + newStroke
                                }
                                currentPoints = emptyList()
                            }
                        )
                    }
            ) {
                // Render committed strokes
                strokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            lineTo(stroke.points[i].x, stroke.points[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(stroke.color.toULong()),
                        style = Stroke(width = stroke.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Render current in-progress stroke
                if (currentPoints.size >= 2) {
                    val path = Path().apply {
                        moveTo(currentPoints.first().x, currentPoints.first().y)
                        for (i in 1 until currentPoints.size) {
                            lineTo(currentPoints[i].x, currentPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = Color(penColor.value),
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                // Signature line
                val lineY = size.height * 0.75f
                drawLine(
                    color = Color(colors.textPrimary.value).copy(alpha = 0.2f),
                    start = androidx.compose.ui.geometry.Offset(20f, lineY),
                    end = androidx.compose.ui.geometry.Offset(size.width - 20f, lineY),
                    strokeWidth = 1f
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                strokes = emptyList()
                onClear()
            }) {
                Text("Clear", color = colors.error)
            }
            TextButton(
                onClick = { onComplete(strokes) },
                enabled = strokes.isNotEmpty()
            ) {
                Text(
                    "Done",
                    color = if (strokes.isNotEmpty()) colors.primary else colors.textPrimary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
