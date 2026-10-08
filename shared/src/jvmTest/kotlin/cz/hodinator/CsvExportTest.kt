package cz.hodinator

import cz.hodinator.models.TimeRecord
import cz.hodinator.utils.CsvExport
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CsvExportTest {
    private fun record(name: String, seconds: Long, isoStart: String = "2026-10-05T08:00:00Z") =
        TimeRecord(projectName = name, durationSeconds = seconds, startTime = Instant.parse(isoStart))

    @Test fun summarizesMonthByProjectSortedByTime() {
        val records = listOf(
            record("Alfa", 1800),
            record("Beta", 7200),
            record("Alfa", 1800),
            record("Jiný měsíc", 100, isoStart = "2026-09-30T08:00:00Z"),
        )

        val csv = CsvExport.monthlySummary(records, 2026, 10, TimeZone.UTC)

        assertEquals("﻿Název úkolu;Čas\r\nBeta;02:00:00\r\nAlfa;01:00:00\r\n", csv)
    }

    @Test fun quotesCellsWithSpecialCharacters() {
        val csv = CsvExport.monthlySummary(listOf(record("Úkol; \"důležitý\"", 60)), 2026, 10, TimeZone.UTC)

        assertEquals("\"Úkol; \"\"důležitý\"\"\";00:01:00", csv.lines()[1].trimEnd('\r'))
    }

    @Test fun defaultFileNameIsZeroPadded() {
        assertEquals("TimeTracker_Prehled_2026_03.csv", CsvExport.defaultMonthlyFileName(2026, 3))
    }
}
