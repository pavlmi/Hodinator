package cz.pavlik.timetracker.data

import cz.pavlik.timetracker.models.NO_PROJECT_NAME
import cz.pavlik.timetracker.models.TimeRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
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

    init {
        dbFile.parentFile?.mkdirs()
    }

    suspend fun getRecords(): List<TimeRecord> = withConnection { conn ->
        conn.prepareStatement("SELECT id, project_name, duration_seconds, timestamp FROM records ORDER BY timestamp DESC")
            .use { stmt ->
                stmt.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                TimeRecord(
                                    id = rs.getLong("id"),
                                    projectName = rs.getString("project_name"),
                                    durationSeconds = rs.getLong("duration_seconds"),
                                    startTime = Instant.parse(rs.getString("timestamp")),
                                )
                            )
                        }
                    }
                }
            }
    }

    /** Inserts [record] as a new row; its `id` is ignored. */
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

    private fun PreparedStatement.bindRecord(record: TimeRecord) {
        setString(1, record.projectName.trim().ifBlank { NO_PROJECT_NAME })
        setLong(2, record.durationSeconds)
        setString(3, record.startTime.toString())
    }

    private suspend fun <T> withConnection(block: (Connection) -> T): T = withContext(Dispatchers.IO) {
        DriverManager.getConnection(url).use { conn ->
            if (!schemaCreated) {
                conn.createStatement().use { it.execute(CREATE_TABLE_SQL) }
                schemaCreated = true
            }
            block(conn)
        }
    }

    private companion object {
        // The `timestamp` column holds the record's start as ISO-8601 text.
        val CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS records (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                project_name TEXT NOT NULL,
                duration_seconds INTEGER NOT NULL,
                timestamp TEXT NOT NULL
            )
        """.trimIndent()
    }
}
