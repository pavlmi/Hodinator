package cz.pavlik.timetracker.utils

import cz.pavlik.timetracker.models.TimeFilter
import cz.pavlik.timetracker.models.TimeRecord
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.iterator

object TimeUtils {

    fun formatSeconds(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    fun formatDateTime(
        instant: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = instant.toLocalDateTime(timeZone)
        return "%02d.%02d.%04d %02d:%02d".format(
            ldt.dayOfMonth,
            ldt.monthNumber,
            ldt.year,
            ldt.hour,
            ldt.minute
        )
    }

    fun formatDate(
        instant: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = instant.toLocalDateTime(timeZone)
        return "%02d.%02d.%04d".format(
            ldt.dayOfMonth,
            ldt.monthNumber,
            ldt.year
        )
    }

    fun formatDate(date: LocalDate): String {
        return "%02d.%02d.%04d".format(date.dayOfMonth, date.monthNumber, date.year)
    }

    fun dayOfWeekName(date: LocalDate): String = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "Pondělí"
        DayOfWeek.TUESDAY -> "Úterý"
        DayOfWeek.WEDNESDAY -> "Středa"
        DayOfWeek.THURSDAY -> "Čtvrtek"
        DayOfWeek.FRIDAY -> "Pátek"
        DayOfWeek.SATURDAY -> "Sobota"
        DayOfWeek.SUNDAY -> "Neděle"
        else -> ""
    }

    fun formatTime(
        instant: Instant,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val ldt = instant.toLocalDateTime(timeZone)
        return "%02d:%02d".format(ldt.hour, ldt.minute)
    }

    fun formatTime(ldt: LocalDateTime): String {
        return "%02d:%02d".format(ldt.hour, ldt.minute)
    }

    fun parseTime(timeStr: String): Pair<Int, Int>? {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                if (hour in 0..23 && minute in 0..59) {
                    Pair(hour, minute)
                } else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun parseDate(dateStr: String): LocalDate? {
        return try {
            val parts = dateStr.trim().split(".")
            if (parts.size == 3) {
                val day = parts[0].padStart(2, '0').toInt()
                val month = parts[1].padStart(2, '0').toInt()
                val year = parts[2].toInt()
                LocalDate(year, month, day)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun filterRecords(
        records: List<TimeRecord>,
        filter: TimeFilter,
        now: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): List<TimeRecord> {
        val nowLdt = now.toLocalDateTime(timeZone)

        return records.filter { record ->
            val recordLdt = record.timestamp.toLocalDateTime(timeZone)
            when (filter) {
                TimeFilter.ALL -> true
                TimeFilter.TODAY -> {
                    recordLdt.date == nowLdt.date
                }
                TimeFilter.THIS_MONTH -> {
                    recordLdt.year == nowLdt.year && recordLdt.month == nowLdt.month
                }
                TimeFilter.LAST_MONTH -> {
                    val (targetYear, targetMonthNumber) = if (nowLdt.monthNumber == 1) {
                        Pair(nowLdt.year - 1, 12)
                    } else {
                        Pair(nowLdt.year, nowLdt.monthNumber - 1)
                    }
                    recordLdt.year == targetYear && recordLdt.monthNumber == targetMonthNumber
                }
                TimeFilter.CUSTOM -> {
                    val recordDate = recordLdt.date
                    val afterFrom = fromDate == null || recordDate >= fromDate
                    val beforeTo = toDate == null || recordDate <= toDate
                    afterFrom && beforeTo
                }
            }
        }
    }

    fun generateMonthlyCsv(
        records: List<TimeRecord>,
        year: Int,
        monthNumber: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val filtered = records.filter { record ->
            val ldt = record.timestamp.toLocalDateTime(timeZone)
            ldt.year == year && ldt.monthNumber == monthNumber
        }

        val grouped = filtered.groupBy { it.projectName }

        val sb = StringBuilder()
        sb.append("\uFEFF") // UTF-8 BOM
        sb.append("Název úkolu;Celkový čas\n")

        for ((projectName, projRecords) in grouped) {
            val totalSeconds = projRecords.sumOf { it.durationSeconds }
            val formattedTime = formatSeconds(totalSeconds)
            val cleanProjectName = projectName.replace(";", ",")
            sb.append("$cleanProjectName;$formattedTime\n")
        }

        return sb.toString()
    }
}
