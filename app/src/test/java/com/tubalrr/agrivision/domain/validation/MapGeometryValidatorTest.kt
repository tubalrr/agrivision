package com.tubalrr.agrivision.domain.validation

import com.tubalrr.agrivision.domain.model.MapPoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapGeometryValidatorTest {
    private val square = listOf(
        MapPoint(10.0, 10.0),
        MapPoint(10.0, 10.001),
        MapPoint(10.001, 10.001),
        MapPoint(10.001, 10.0)
    )

    @Test
    fun validPolygonPasses() {
        assertTrue(MapGeometryValidator.validatePolygon(square, "Field").valid)
    }

    @Test
    fun duplicateAdjacentPointFails() {
        val points = square.toMutableList().apply { add(2, square[1]) }
        assertFalse(MapGeometryValidator.validatePolygon(points, "Field").valid)
    }

    @Test
    fun zeroAreaPolygonFails() {
        val points = listOf(
            MapPoint(10.0, 10.0),
            MapPoint(10.001, 10.001),
            MapPoint(10.002, 10.002)
        )
        assertFalse(MapGeometryValidator.validatePolygon(points, "Field").valid)
    }

    @Test
    fun selfCrossingPolygonFails() {
        val points = listOf(
            MapPoint(10.0, 10.0),
            MapPoint(10.001, 10.001),
            MapPoint(10.0, 10.001),
            MapPoint(10.001, 10.0)
        )
        assertFalse(MapGeometryValidator.validatePolygon(points, "Incident").valid)
    }

    @Test
    fun invalidCoordinateFails() {
        val points = listOf(
            MapPoint(91.0, 10.0),
            MapPoint(10.001, 10.001),
            MapPoint(10.0, 10.0)
        )
        assertFalse(MapGeometryValidator.validatePolygon(points, "Farm").valid)
    }
}
