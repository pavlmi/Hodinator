package cz.hodinator.utils

import cz.hodinator.models.TimeFilter
import cz.hodinator.models.TimeRecord
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime


/** Formatting and parsing in Czech format (date `dd.MM.yyyy`, time `HH:mm`) and calculations over records. */
object TimeUtils {

    private val DAY_NAMES = listOf("Pondělí", "Úterý", "Středa", "Čtvrtek", "Pátek", "Sobota", "Neděle")

    private val MONTH_NAMES = listOf(
        "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
        "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec",
    )

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

    /** Parses "H:mm". Returns `null` for an invalid time. */
    fun parseTime(text: String): LocalTime? {
        val parts = text.trim().split(":").map { it.trim().toIntOrNull() ?: return null }
        if (parts.size != 2) return null
        val (hour, minute) = parts
        return runCatching { LocalTime(hour, minute) }.getOrNull()
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
