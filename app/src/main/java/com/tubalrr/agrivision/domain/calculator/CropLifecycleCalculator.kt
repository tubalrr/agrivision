package com.tubalrr.agrivision.domain.calculator

object CropLifecycleCalculator {
    val stages = listOf(
        "Land Preparation",
        "Planting",
        "Growing",
        "Fertilization",
        "Pest/Disease Monitoring",
        "Harvest",
        "Production",
        "Sales"
    )

    fun indexOf(stage: String): Int =
        stages.indexOfFirst { it.equals(stage, ignoreCase = true) }.let { if (it < 0) 0 else it }

    fun completedStages(events: List<String>): Set<String> =
        stages.filter { stage ->
            events.any { it.equals(stage, ignoreCase = true) }
        }.toSet()

    fun progress(events: List<String>): Int {
        if (events.isEmpty()) return 0
        return completedStages(events).size
    }

    fun nextStage(currentStage: String): String? {
        val index = stages.indexOfFirst { it.equals(currentStage, ignoreCase = true) }
        return if (index in 0 until stages.lastIndex) stages[index + 1] else null
    }

    fun isValidStage(stage: String): Boolean =
        stages.any { it.equals(stage, ignoreCase = true) }

    fun normalize(stage: String): String =
        stages.firstOrNull { it.equals(stage, ignoreCase = true) } ?: stages.first()
}
