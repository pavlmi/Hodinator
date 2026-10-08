package cz.hodinator.models

import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds


const val NO_PROJECT_NAME = "Bez projektu"
private val WHITESPACE = Regex("\\s+")

/**
 * Collapses all whitespace, including line breaks from text pasted e.g. from Jira, into single spaces,
 * so a project name is always one line. Blank names become [NO_PROJECT_NAME].
 */
fun normalizeProjectName(name: String): String = name.trim().replace(WHITESPACE, " ").ifBlank { NO_PROJECT_NAME }

data class TimeRecord(
    val id: Long = 0,
    val projectName: String,
    val durationSeconds: Long,
    val startTime: Instant,
) {
    val endTime: Instant get() = startTime + durationSeconds.seconds
}

enum class TimeFilter { ALL, TODAY, THIS_MONTH, LAST_MONTH, CUSTOM }
