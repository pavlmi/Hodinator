package cz.pavlik.timetracker.models

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds


/** Name used to store records whose project name was left blank. */
const val NO_PROJECT_NAME = "Bez projektu"

data class TimeRecord(
    val id: Long = 0,
    val projectName: String,
    val durationSeconds: Long,
    val startTime: Instant,
) {
    val endTime: Instant get() = startTime + durationSeconds.seconds
}

enum class TimeFilter { ALL, TODAY, THIS_MONTH, LAST_MONTH, CUSTOM }
