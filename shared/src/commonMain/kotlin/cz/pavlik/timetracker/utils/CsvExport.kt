package cz.pavlik.timetracker.utils

import cz.pavlik.timetracker.models.TimeRecord
import kotlinx.datetime.TimeZone

/** CSV pro Excel v české lokalizaci: UTF-8 s BOM a středník jako oddělovač. */
object CsvExport {
    private const val SEPARATOR = ';'
    private const val BOM = "﻿"

    fun defaultMonthlyFileName(year: Int, monthNumber: Int) = "TimeTracker_Prehled_${year}_${"%02d".format(monthNumber)}.csv"

    /** Součet času po projektech za daný měsíc, seřazeno od projektu s nejvíce odpracovaným časem. */
    fun monthlySummary(
        records: List<TimeRecord>,
        year: Int,
        monthNumber: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): String {
        val totalsByProject = TimeUtils.recordsInMonth(records, year, monthNumber, timeZone)
            .groupBy { it.projectName }
            .mapValues { (_, projectRecords) -> projectRecords.sumOf { it.durationSeconds } }
            .entries
            .sortedByDescending { it.value }

        return buildString {
            append(BOM)
            appendRow("Název úkolu", "Celkový čas")
            totalsByProject.forEach { (project, seconds) -> appendRow(project, TimeUtils.formatSeconds(seconds)) }
        }
    }

    private fun StringBuilder.appendRow(vararg cells: String) {
        cells.joinTo(this, separator = SEPARATOR.toString()) { escape(it) }
        append("\r\n")
    }

    private fun escape(cell: String): String =
        if (cell.any { it == SEPARATOR || it == '"' || it == '\n' || it == '\r' }) "\"${cell.replace("\"", "\"\"")}\""
        else cell
}
