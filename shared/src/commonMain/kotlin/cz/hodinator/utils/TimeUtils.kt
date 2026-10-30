package cz.hodinator.utils

import cz.hodinator.models.TimeFilter
import cz.hodinator.models.TimeRecord
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime


/** Formatting and parsing in Czech format (date `dd.MM.yyyy`, time `HH:mm`) and calculations over records. */
object TimeUtils {
    private val DAY_NAMES = listOf("Pondělí", "Úterý", "Středa", "Čtvrtek", "Pátek", "Sobota", "Neděle")
    private val MONTH_NAMES = listOf("Leden", "Únor", "Březen", "Duben", "Květen", "Červen", "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec")

    /** E.g. "07:32:05". */
    fun formatSeconds(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    /** E.g. "7 h 32 min", "45 min", "0 h". */
    fun formatHoursMinutes(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "$hours h $minutes min"
            hours > 0 -> "$hours h"
            minutes > 0 -> "$minutes min"
            else -> "0 h"
        }
    }

    fun formatDate(date: LocalDate): String = "%02d.%02d.%04d".format(date.dayOfMonth, date.monthNumber, date.year)

    fun formatTime(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

    fun dayOfWeekName(date: LocalDate): String = DAY_NAMES[date.dayOfWeek.isoDayNumber - 1]

    fun dayOfWeekShortName(date: LocalDate): String = dayOfWeekName(date).take(2)

    /** E.g. "Pondělí 05.10.2026". */
    fun dayWithDate(date: LocalDate): String = "${dayOfWeekName(date)} ${formatDate(date)}"

    fun isWeekend(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    fun monthName(monthNumber: Int): String = MONTH_NAMES[monthNumber - 1]

    /** Parses "d.M.yyyy" (with or without leading zeros). Returns `null` for an invalid date. */
    fun parseDate(text: String): LocalDate? {
        val parts = text.trim().split(".").map { it.trim().toIntOrNull() ?: return null }
        if (parts.size != 3) return null
        val (day, month, year) = parts
        return runCatching { LocalDate(year, month, day) }.getOrNull()
    }

    /** Parses "H:mm" or "H.mm", or digits only: "8" (hours), "800" or "0800". Returns `null` for an invalid time. */
    fun parseTime(text: String): LocalTime? {
        val trimmed = text.trim()
        val (hour, minute) = if (trimmed.all { it.isDigit() }) {
            when (trimmed.length) {
                1, 2 -> trimmed.toInt() to 0
                3, 4 -> trimmed.dropLast(2).toInt() to trimmed.takeLast(2).toInt()
                else -> return null
            }
        } else {
            val parts = trimmed.split(':', '.').map { it.trim().toIntOrNull() ?: return null }
            if (parts.size != 2) return null
            parts[0] to parts[1]
        }
        return runCatching { LocalTime(hour, minute) }.getOrNull()
    }

    /** While typing, turns four digits such as "0800" into "08:00". Deleting (e.g. the colon) is left alone. */
    fun autoFormatTime(oldText: String, newText: String): String =
        if (newText.length > oldText.length && newText.length == 4 && newText.all { it.isDigit() }) normalizeTime(newText) else newText

    /** Reformats a valid time to "HH:mm", e.g. "0800" to "08:00"; invalid text is returned unchanged. */
    fun normalizeTime(text: String): String = parseTime(text)?.let(::formatTime) ?: text

    /** A record on [date] from [from] to [to], or `null` when the range is empty. */
    fun recordForDay(
        projectName: String,
        date: LocalDate,
        from: LocalTime,
        to: LocalTime,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): TimeRecord? {
        val start = LocalDateTime(date, from).toInstant(timeZone)
        val durationSeconds = (LocalDateTime(date, to).toInstant(timeZone) - start).inWholeSeconds
        return if (durationSeconds > 0) TimeRecord(projectName = projectName, durationSeconds = durationSeconds, startTime = start) else null
    }

    fun filterRecords(
        records: List<TimeRecord>,
        filter: TimeFilter,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
    ): List<TimeRecord> {
        if (filter == TimeFilter.ALL) return records

        val today = now.toLocalDateTime(timeZone).date
        val lastMonth = today.minus(1, DateTimeUnit.MONTH)

        return records.filter { record ->
            val date = record.startTime.toLocalDateTime(timeZone).date
            when (filter) {
                TimeFilter.ALL -> true
                TimeFilter.TODAY -> date == today
                TimeFilter.THIS_MONTH -> date.isInMonthOf(today)
                TimeFilter.LAST_MONTH -> date.isInMonthOf(lastMonth)
                TimeFilter.CUSTOM -> (fromDate == null || date >= fromDate) && (toDate == null || date <= toDate)
            }
        }
    }

    /**
     * Days listed for [filter], newest first: the whole range of the filter up to today (or the latest record),
     * so days without records are listed too. An open range starts at the earliest record.
     */
    fun daysToShow(
        records: List<TimeRecord>,
        filter: TimeFilter,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
    ): List<LocalDate> {
        val today = now.toLocalDateTime(timeZone).date
        val recordDates = records.map { it.startTime.toLocalDateTime(timeZone).date }
        val earliest = recordDates.minOrNull() ?: today
        val latest = maxOf(today, recordDates.maxOrNull() ?: today)
        val lastMonthStart = LocalDate(today.year, today.month, 1).minus(1, DateTimeUnit.MONTH)

        val (start, end) = when (filter) {
            TimeFilter.ALL -> earliest to latest
            TimeFilter.TODAY -> today to today
            TimeFilter.THIS_MONTH -> LocalDate(today.year, today.month, 1) to latest
            TimeFilter.LAST_MONTH -> lastMonthStart to lastMonthStart.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            TimeFilter.CUSTOM -> (fromDate ?: earliest) to minOf(toDate ?: latest, latest)
        }
        return generateSequence(end) { it.minus(1, DateTimeUnit.DAY) }.takeWhile { it >= start }.toList()
    }

    /** Records starting in the given month. */
    fun recordsInMonth(
        records: List<TimeRecord>,
        year: Int,
        monthNumber: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<TimeRecord> = records.filter {
        val date = it.startTime.toLocalDateTime(timeZone).date
        date.year == year && date.monthNumber == monthNumber
    }

    /** Seconds per project for each day of the month that has records, longest first. A record counts toward the day it starts. */
    fun projectTotalsByDay(
        records: List<TimeRecord>,
        year: Int,
        monthNumber: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): Map<LocalDate, List<Pair<String, Long>>> =
        recordsInMonth(records, year, monthNumber, timeZone)
            .groupBy { it.startTime.toLocalDateTime(timeZone).date }
            .mapValues { (_, dayRecords) ->
                dayRecords.groupBy { it.projectName }
                    .map { (name, projectRecords) -> name to projectRecords.sumOf { it.durationSeconds } }
                    .sortedByDescending { it.second }
            }

    /** Seconds worked for every day of the month (including days without records). A record counts toward the day it starts. */
    fun dailyTotalsForMonth(
        records: List<TimeRecord>,
        year: Int,
        monthNumber: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): List<Pair<LocalDate, Long>> {
        val totalsByDate = recordsInMonth(records, year, monthNumber, timeZone)
            .groupBy { it.startTime.toLocalDateTime(timeZone).date }
            .mapValues { (_, dayRecords) -> dayRecords.sumOf { it.durationSeconds } }

        val firstDay = LocalDate(year, monthNumber, 1)
        return generateSequence(firstDay) { it.plus(1, DateTimeUnit.DAY) }
            .takeWhile { it.monthNumber == monthNumber }
            .map { it to (totalsByDate[it] ?: 0L) }
            .toList()
    }

    private fun LocalDate.isInMonthOf(other: LocalDate) = year == other.year && month == other.month
}
