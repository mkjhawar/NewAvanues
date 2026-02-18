package com.augmentalis.annotationavanue.controller

import com.augmentalis.annotationavanue.model.StrokePoint
import kotlin.math.sqrt

/**
 * Smooths raw freehand stroke points using Catmull-Rom to cubic Bezier conversion.
 * Returns interpolated points that produce smooth curves when rendered with lineTo.
 *
 * Architecture: Pure KMP math — no platform dependencies.
 * Called at render time, NOT stored. Raw points are the source of truth.
 */
object BezierSmoother {

    private const val DEFAULT_TENSION = 0.3f
    private const val SEGMENTS_PER_CURVE = 8

    /**
     * Smooth a list of raw points into a denser, smoother point list.
     * Uses Catmull-Rom spline interpolation with configurable tension.
     *
     * @param points Raw input points (minimum 2 required)
     * @param tension Smoothing tension. 0 = straight lines, 1 = maximum smoothing. Default 0.3
     * @return Smoothed points ready for rendering with lineTo
     */
    fun smooth(points: List<StrokePoint>, tension: Float = DEFAULT_TENSION): List<StrokePoint> {
        if (points.size < 3) return points

        val result = mutableListOf<StrokePoint>()
        result.add(points.first())

        for (i in 0 until points.size - 1) {
            val p0 = points[maxOf(0, i - 1)]
            val p1 = points[i]
            val p2 = points[minOf(points.size - 1, i + 1)]
            val p3 = points[minOf(points.size - 1, i + 2)]

            // Catmull-Rom control points → cubic Bezier control points
            val cp1x = p1.x + (p2.x - p0.x) * tension / 3f
            val cp1y = p1.y + (p2.y - p0.y) * tension / 3f
            val cp2x = p2.x - (p3.x - p1.x) * tension / 3f
            val cp2y = p2.y - (p3.y - p1.y) * tension / 3f

            // Interpolate cubic Bezier
            for (t in 1..SEGMENTS_PER_CURVE) {
                val u = t.toFloat() / SEGMENTS_PER_CURVE
                val x = cubicBezier(p1.x, cp1x, cp2x, p2.x, u)
                val y = cubicBezier(p1.y, cp1y, cp2y, p2.y, u)
                val pressure = lerp(p1.pressure, p2.pressure, u)
                result.add(StrokePoint(x, y, pressure))
            }
        }

        return result
    }

    /**
     * Simplify a dense point list using Ramer-Douglas-Peucker algorithm.
     * Reduces point count while preserving stroke shape — useful before serialization
     * for strokes that have been smoothed multiple times.
     *
     * @param points Input points
     * @param epsilon Distance threshold (points within this distance of the simplified line are removed)
     * @return Simplified point list
     */
    fun simplify(points: List<StrokePoint>, epsilon: Float = 2f): List<StrokePoint> {
        if (points.size < 3) return points

        var maxDist = 0f
        var maxIndex = 0

        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = simplify(points.subList(0, maxIndex + 1), epsilon)
            val right = simplify(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun cubicBezier(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val mt = 1f - t
        return mt * mt * mt * p0 +
            3f * mt * mt * t * p1 +
            3f * mt * t * t * p2 +
            t * t * t * p3
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun perpendicularDistance(point: StrokePoint, lineStart: StrokePoint, lineEnd: StrokePoint): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLenSq = dx * dx + dy * dy
        if (lineLenSq == 0f) {
            val px = point.x - lineStart.x
            val py = point.y - lineStart.y
            return sqrt(px * px + py * py)
        }
        val num = kotlin.math.abs(dy * point.x - dx * point.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x)
        return num / sqrt(lineLenSq)
    }
}
