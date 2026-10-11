package com.tubalrr.agrivision.domain.validation

import com.tubalrr.agrivision.domain.model.MapPoint
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

data class MapGeometryValidation(
    val valid: Boolean,
    val message: String = ""
)

object MapGeometryValidator {
    fun validatePolygon(points: List<MapPoint>, label: String): MapGeometryValidation {
        if (points.size < 3) {
            return MapGeometryValidation(false, "$label boundary needs at least 3 points.")
        }
        if (points.any { !it.latitude.isFinite() || it.latitude !in -90.0..90.0 || !it.longitude.isFinite() || it.longitude !in -180.0..180.0 }) {
            return MapGeometryValidation(false, "$label boundary contains an invalid coordinate.")
        }
        val normalized = points.map { it.longitude to it.latitude }
        for (index in 1 until normalized.size) {
            if (samePoint(normalized[index - 1], normalized[index])) {
                return MapGeometryValidation(false, "$label boundary has duplicate adjacent points.")
            }
        }
        if (samePoint(normalized.first(), normalized.last())) {
            return MapGeometryValidation(false, "$label boundary repeats its first point; close the shape by saving without adding the same point again.")
        }

        val centerLat = points.map { it.latitude }.average()
        val projected = points.map { point ->
            val x = point.longitude * cos(Math.toRadians(centerLat))
            val y = point.latitude
            x to y
        }
        val area = signedArea(projected)
        if (kotlin.math.abs(area) < 1e-10) {
            return MapGeometryValidation(false, "$label boundary encloses no area. Move the points to form a shape.")
        }

        val size = projected.size
        for (i in 0 until size) {
            val a1 = projected[i]
            val a2 = projected[(i + 1) % size]
            for (j in i + 1 until size) {
                if (j == i || (j == (i + 1) % size) || (i == 0 && j == size - 1)) continue
                val b1 = projected[j]
                val b2 = projected[(j + 1) % size]
                if (segmentsIntersect(a1, a2, b1, b2)) {
                    return MapGeometryValidation(false, "$label boundary has crossing edges. Rearrange the points so the boundary does not cross itself.")
                }
            }
        }

        return MapGeometryValidation(true)
    }

    private fun samePoint(a: Pair<Double, Double>, b: Pair<Double, Double>): Boolean =
        kotlin.math.abs(a.first - b.first) < 1e-9 &&
            kotlin.math.abs(a.second - b.second) < 1e-9

    private fun signedArea(points: List<Pair<Double, Double>>): Double {
        var area = 0.0
        for (index in points.indices) {
            val next = points[(index + 1) % points.size]
            area += points[index].first * next.second - next.first * points[index].second
        }
        return area / 2.0
    }

    private fun orientation(
        a: Pair<Double, Double>,
        b: Pair<Double, Double>,
        c: Pair<Double, Double>
    ): Double = (b.first - a.first) * (c.second - a.second) -
        (b.second - a.second) * (c.first - a.first)

    private fun onSegment(
        a: Pair<Double, Double>,
        b: Pair<Double, Double>,
        p: Pair<Double, Double>
    ): Boolean =
        p.first >= min(a.first, b.first) - 1e-10 &&
            p.first <= max(a.first, b.first) + 1e-10 &&
            p.second >= min(a.second, b.second) - 1e-10 &&
            p.second <= max(a.second, b.second) + 1e-10

    private fun segmentsIntersect(
        a1: Pair<Double, Double>,
        a2: Pair<Double, Double>,
        b1: Pair<Double, Double>,
        b2: Pair<Double, Double>
    ): Boolean {
        val o1 = orientation(a1, a2, b1)
        val o2 = orientation(a1, a2, b2)
        val o3 = orientation(b1, b2, a1)
        val o4 = orientation(b1, b2, a2)
        val eps = 1e-10

        if ((o1 > eps && o2 < -eps || o1 < -eps && o2 > eps) &&
            (o3 > eps && o4 < -eps || o3 < -eps && o4 > eps)
        ) return true

        if (kotlin.math.abs(o1) <= eps && onSegment(a1, a2, b1)) return true
        if (kotlin.math.abs(o2) <= eps && onSegment(a1, a2, b2)) return true
        if (kotlin.math.abs(o3) <= eps && onSegment(b1, b2, a1)) return true
        if (kotlin.math.abs(o4) <= eps && onSegment(b1, b2, a2)) return true
        return false
    }
}
