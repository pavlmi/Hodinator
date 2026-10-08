package cz.pavlik.timetracker

import cz.pavlik.timetracker.models.TimeFilter
import cz.pavlik.timetracker.models.TimeRecord
import cz.pavlik.timetracker.utils.TimeUtils
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimeUtilsTest {
    private val utc = TimeZone.UTC

    private fun record(isoStart: String, seconds: Long = 3600, name: String = "Projekt") =
        TimeRecord(projectName = name, durationSeconds = seconds, startTime = Instant.parse(isoStart))

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
        assertNull(TimeUtils.parseTime("1200"))
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
}
