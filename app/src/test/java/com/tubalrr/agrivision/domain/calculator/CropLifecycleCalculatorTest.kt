package com.tubalrr.agrivision.domain.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CropLifecycleCalculatorTest {
    @Test fun stageMatchingIsCaseInsensitive() {
        assertTrue(CropLifecycleCalculator.isValidStage("harvest"))
        assertEquals("Harvest", CropLifecycleCalculator.normalize("harvest"))
        assertEquals(5, CropLifecycleCalculator.indexOf("Harvest"))
    }

    @Test fun progressCountsDistinctKnownStagesOnly() {
        assertEquals(2, CropLifecycleCalculator.progress(listOf("Planting", "planting", "Harvest", "unknown")))
        assertFalse(CropLifecycleCalculator.isValidStage("unknown"))
    }

    @Test fun finalStageHasNoNextStage() {
        assertNull(CropLifecycleCalculator.nextStage("Sales"))
        assertEquals("Planting", CropLifecycleCalculator.nextStage("Land Preparation"))
    }
}
