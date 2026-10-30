package cz.hodinator.data

import cz.hodinator.models.DayMark
import cz.hodinator.models.normalizeProjectName
import cz.hodinator.models.TimeRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement


/**
 * Access to the SQLite database of time records. Each call opens its own connection on [Dispatchers.IO].
 * Errors propagate to the caller so they can be shown to the user.
 */
class DatabaseManager(dbFile: File) {
    private val url = "jdbc:sqlite:${dbFile.absolutePath}"

    @Volatile private var schemaCreated = false

    init { dbFile.parentFile?.mkdirs() }

    suspend fun getRecords(): List<TimeRecord> = withConnection { conn ->
        conn.prepareStatement("SELECT id, project_name, duration_seconds, timestamp FROM records ORDER BY timestamp DESC")
            .use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                TimeRecord(
                                    id = rs.getLong("id"),
                                    // Also normalized on read, so older records with line breaks display and export cleanly.
                                    projectName = normalizeProjectName(rs.getString("project_name")),
                                    durationSeconds = rs.getLong("duration_seconds"),
                                    startTime = Instant.parse(rs.getString("timestamp")),
                                )
                            )
                        }
                    }
                }
            }
    }

    suspend fun insertRecord(record: TimeRecord) = withConnection { conn ->
        conn.prepareStatement("INSERT INTO records (project_name, duration_seconds, timestamp) VALUES (?, ?, ?)")
            .use { stmt ->
                stmt.bindRecord(record)
                stmt.executeUpdate()
            }
        Unit
    }

    suspend fun updateRecord(record: TimeRecord) = withConnection { conn ->
        conn.prepareStatement("UPDATE records SET project_name = ?, duration_seconds = ?, timestamp = ? WHERE id = ?")
            .use { stmt ->
                stmt.bindRecord(record)
                stmt.setLong(4, record.id)
                stmt.executeUpdate()
            }
        Unit
    }

    suspend fun deleteRecord(id: Long) = withConnection { conn ->
        conn.prepareStatement("DELETE FROM records WHERE id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeUpdate()
        }
        Unit
    }

    suspend fun getDayMarks(): Map<LocalDate, DayMark> = withConnection { conn ->
        conn.prepareStatement("SELECT date, mark FROM day_marks").use { stmt ->
            stmt.executeQuery().use { rs ->
                buildMap {
                    while (rs.next()) {
                        val mark = DayMark.entries.find { it.name == rs.getString("mark") } ?: continue
                        put(LocalDate.parse(rs.getString("date")), mark)
                    }
                }
            }
        }
    }

    /** Marks the day, or removes its mark when [mark] is null. */
    suspend fun setDayMark(date: LocalDate, mark: DayMark?) = withConnection { conn ->
        val sql = if (mark == null) "DELETE FROM day_marks WHERE date = ?" else "INSERT OR REPLACE INTO day_marks (date, mark) VALUES (?, ?)"
        conn.prepareStatement(sql).use { stmt ->
            stmt.setString(1, date.toString())
            mark?.let { stmt.setString(2, it.name) }
            stmt.executeUpdate()
        }
        Unit
    }

    private fun PreparedStatement.bindRecord(record: TimeRecord) {
        setString(1, normalizeProjectName(record.projectName))
        setLong(2, record.durationSeconds)
        setString(3, record.startTime.toString())
    }

    private suspend fun <T> withConnection(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        DriverManager.getConnection(url).use { conn ->
            if (!schemaCreated) {
                conn.createStatement().use { stmt -> SCHEMA_SQL.forEach { stmt.execute(it) } }
                schemaCreated = true
            }
            block(conn)
        }
    }

    private companion object {
        // `records.timestamp` holds the record's start as ISO-8601 text, `day_marks.mark` a DayMark name.
        val SCHEMA_SQL = listOf(
            """
                CREATE TABLE IF NOT EXISTS records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    project_name TEXT NOT NULL,
                    duration_seconds INTEGER NOT NULL,
                    timestamp TEXT NOT NULL
                )
            """.trimIndent(),
            "CREATE TABLE IF NOT EXISTS day_marks (date TEXT PRIMARY KEY, mark TEXT NOT NULL)",
        )
    }
}
