package cz.pavlik.timetracker.models

import kotlinx.datetime.Instant

data class TimeRecord(
    val id: Long = 0,
    val projectName: String,
    val durationSeconds: Long,
    val timestamp: Instant
)

enum class TimeFilter { ALL, TODAY, THIS_MONTH, LAST_MONTH, CUSTOM }
