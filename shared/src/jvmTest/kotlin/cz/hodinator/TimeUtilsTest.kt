package cz.hodinator

import cz.hodinator.models.NO_PROJECT_NAME
import cz.hodinator.models.TimeFilter
import cz.hodinator.models.TimeRecord
import cz.hodinator.models.normalizeProjectName
import cz.hodinator.utils.TimeUtils
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeUtilsTest {
    private val utc = TimeZone.UTC

    private fun record(isoStart: String, seconds: Long = 3600, name: String = "Projekt") =
        TimeRecord(projectName = name, durationSeconds = seconds, startTime = Instant.parse(isoStart))

    @Test fun createsRecordForDay() {
        val record = TimeUtils.recordForDay("Práce", LocalDate(2026, 10, 30), LocalTime(8, 0), LocalTime(17, 0), utc)
        assertEquals(TimeRecord(projectName = "Práce", durationSeconds = 9 * 3600, startTime = Instant.parse("2026-10-30T08:00:00Z")), record)
        assertNull(TimeUtils.recordForDay("Práce", LocalDate(2026, 10, 30), LocalTime(17, 0), LocalTime(8, 0), utc))
    }

    @Test fun formatsDurations() {
        assertEquals("00:00:00", TimeUtils.formatSeconds(0))
        assertEquals("27:46:40", TimeUtils.formatSeconds(100_000))
        assertEquals("0 h", TimeUtils.formatHoursMinutes(59))
        assertEquals("45 min", TimeUtils.formatHoursMinutes(45 * 60))
        assertEquals("2 h", TimeUtils.formatHoursMinutes(7200))
        assertEquals("7 h 32 min", TimeUtils.formatHoursMinutes(7 * 3600 + 32 * 60 + 10))
    }

    @Test fun parsesDates() {
        assertEquals(LocalDate(2026, 10, 1), TimeUtils.parseDate("01.10.2026"))
        assertEquals(LocalDate(2026, 10, 1), TimeUtils.parseDate(" 1.10.2026 "))
        assertNull(TimeUtils.parseDate("31.02.2026"))
        assertNull(TimeUtils.parseDate("2026-10-01"))
        assertNull(TimeUtils.parseDate("aa.bb.cccc"))
        assertNull(TimeUtils.parseDate(""))
    }

    @Test fun parsesTimes() {
        assertEquals(LocalTime(8, 5), TimeUtils.parseTime("8:05"))
        assertEquals(LocalTime(23, 59), TimeUtils.parseTime("23:59"))
        assertNull(TimeUtils.parseTime("24:00"))
        assertNull(TimeUtils.parseTime("12:60"))
        assertEquals(LocalTime(8, 0), TimeUtils.parseTime("0800"))
        assertEquals(LocalTime(8, 30), TimeUtils.parseTime("830"))
        assertEquals(LocalTime(9, 0), TimeUtils.parseTime("9"))
        assertEquals(LocalTime(17, 15), TimeUtils.parseTime("17.15"))
        assertNull(TimeUtils.parseTime("2400"))
        assertNull(TimeUtils.parseTime("12345"))
        assertNull(TimeUtils.parseTime(""))
        assertEquals("08:00", TimeUtils.normalizeTime("0800"))
        assertEquals("8:7x", TimeUtils.normalizeTime("8:7x"))
    }

    @Test fun autoFormatsTimeWhileTyping() {
        assertEquals("08:00", TimeUtils.autoFormatTime("080", "0800"))
        assertEquals("080", TimeUtils.autoFormatTime("08", "080"))
        assertEquals("0800", TimeUtils.autoFormatTime("08:00", "0800"))
        assertEquals("2500", TimeUtils.autoFormatTime("250", "2500"))
    }

    @Test fun formatsCzechNames() {
        assertEquals("Čtvrtek 01.10.2026", TimeUtils.dayWithDate(LocalDate(2026, 10, 1)))
        assertEquals("Ne", TimeUtils.dayOfWeekShortName(LocalDate(2026, 10, 4)))
        assertEquals("Prosinec", TimeUtils.monthName(12))
    }

    @Test fun filtersRecords() {
        val now = Instant.parse("2026-01-15T12:00:00Z")
        val today = record("2026-01-15T08:00:00Z")
        val thisMonth = record("2026-01-02T08:00:00Z")
        val lastMonth = record("2025-12-31T08:00:00Z")
        val older = record("2025-11-30T08:00:00Z")
        val all = listOf(today, thisMonth, lastMonth, older)

        fun filter(f: TimeFilter, from: LocalDate? = null, to: LocalDate? = null) =
            TimeUtils.filterRecords(all, f, now, utc, from, to)

        assertEquals(all, filter(TimeFilter.ALL))
        assertEquals(listOf(today), filter(TimeFilter.TODAY))
        assertEquals(listOf(today, thisMonth), filter(TimeFilter.THIS_MONTH))
        assertEquals(listOf(lastMonth), filter(TimeFilter.LAST_MONTH))
        assertEquals(
            listOf(thisMonth, lastMonth),
            filter(TimeFilter.CUSTOM, LocalDate(2025, 12, 1), LocalDate(2026, 1, 10)),
        )
        assertEquals(all, filter(TimeFilter.CUSTOM))
    }

    @Test fun listsDaysWithoutRecords() {
        val now = Instant.parse("2026-03-04T12:00:00Z")
        fun days(f: TimeFilter, records: List<TimeRecord> = emptyList(), from: LocalDate? = null, to: LocalDate? = null) =
            TimeUtils.daysToShow(records, f, now, utc, from, to)
        fun range(from: LocalDate, to: LocalDate) = generateSequence(to) { it.minus(1, DateTimeUnit.DAY) }.takeWhile { it >= from }.toList()

        assertEquals(listOf(LocalDate(2026, 3, 4)), days(TimeFilter.TODAY))
        assertEquals(range(LocalDate(2026, 3, 1), LocalDate(2026, 3, 4)), days(TimeFilter.THIS_MONTH))
        assertEquals(range(LocalDate(2026, 2, 1), LocalDate(2026, 2, 28)), days(TimeFilter.LAST_MONTH))
        assertEquals(range(LocalDate(2026, 2, 27), LocalDate(2026, 3, 4)), days(TimeFilter.ALL, listOf(record("2026-02-27T08:00:00Z"))))
        assertEquals(range(LocalDate(2026, 3, 2), LocalDate(2026, 3, 4)), days(TimeFilter.CUSTOM, from = LocalDate(2026, 3, 2), to = LocalDate(2026, 12, 31)))
    }

    @Test fun sumsProjectsPerDay() {
        val records = listOf(
            record("2026-02-03T08:00:00Z", 1800, "Alfa"),
            record("2026-02-03T09:00:00Z", 3600, "Beta"),
            record("2026-02-03T11:00:00Z", 2400, "Alfa"),
            record("2026-02-04T08:00:00Z", 600, "Alfa"),
            record("2026-03-01T08:00:00Z", 600, "Gama"),
        )
        assertEquals(
            mapOf(
                LocalDate(2026, 2, 3) to listOf("Alfa" to 4200L, "Beta" to 3600L),
                LocalDate(2026, 2, 4) to listOf("Alfa" to 600L),
            ),
            TimeUtils.projectTotalsByDay(records, 2026, 2, utc),
        )
    }

    @Test fun computesDailyTotalsForWholeMonth() {
        val records = listOf(
            record("2026-02-03T08:00:00Z", 3600),
            record("2026-02-03T13:00:00Z", 1800),
            record("2026-02-28T08:00:00Z", 60),
            record("2026-03-01T08:00:00Z", 9999),
        )

        val totals = TimeUtils.dailyTotalsForMonth(records, 2026, 2, utc)

        assertEquals(28, totals.size)
        assertEquals(LocalDate(2026, 2, 1), totals.first().first)
        assertEquals(5400L, totals.single { it.first.dayOfMonth == 3 }.second)
        assertEquals(60L, totals.last().second)
        assertEquals(5460L, totals.sumOf { it.second })
    }

    @Test fun normalizesProjectNames() {
        assertEquals("A B C", normalizeProjectName("  A\n\nB \t C\r\n"))
        assertEquals(NO_PROJECT_NAME, normalizeProjectName(" \n "))
    }
}
