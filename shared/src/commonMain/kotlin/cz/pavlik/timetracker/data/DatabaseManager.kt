package cz.pavlik.timetracker.data

import cz.pavlik.timetracker.models.TimeRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

private fun logApp(message: String) {
    try {
        val userHome = System.getProperty("user.home") ?: "."
        val logFile = File(userHome, "Library/Logs/Hodinator.log")
        logFile.parentFile?.mkdirs()
        logFile.appendText("${Clock.System.now()} [Hodinátor]: $message\n")
        println("[Hodinátor] $message")
    } catch (_: Exception) {}
}

private fun getAppDatabasePath(dbName: String = "time_tracker.db"): String {
    val userHome = System.getProperty("user.home") ?: "."
    val osName = System.getProperty("os.name")?.lowercase() ?: ""

    val appDir = when {
        osName.contains("mac") -> {
            File(userHome, "Library/Application Support/Hodinator")
        }
        osName.contains("win") -> {
            val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
            File(appData, "Hodinator")
        }
        else -> {
            File(userHome, ".local/share/Hodinator")
        }
    }

    if (!appDir.exists()) {
        val created = appDir.mkdirs()
        logApp("Created app directory ${appDir.absolutePath}: $created")
    }

    val dbFile = File(appDir, dbName)
    logApp("Database file path: ${dbFile.absolutePath}")

    // Automatická migrace lokální databáze z vývoje
    val oldDevDb1 = File("time_tracker.db")
    val oldDevDb2 = File(userHome, "Library/Application Support/cz.pavlik.timetracker/time_tracker.db")

    if (!dbFile.exists()) {
        if (oldDevDb1.exists()) {
            try {
                oldDevDb1.copyTo(dbFile, overwrite = true)
                logApp("Migrated dev DB from root: ${oldDevDb1.absolutePath} -> ${dbFile.absolutePath}")
            } catch (e: Exception) {
                logApp("Failed to migrate root DB: ${e.message}")
            }
        } else if (oldDevDb2.exists()) {
            try {
                oldDevDb2.copyTo(dbFile, overwrite = true)
                logApp("Migrated dev DB from cz.pavlik.timetracker: ${oldDevDb2.absolutePath} -> ${dbFile.absolutePath}")
            } catch (e: Exception) {
                logApp("Failed to migrate cz.pavlik.timetracker DB: ${e.message}")
            }
        }
    }

    return dbFile.absolutePath
}

class DatabaseManager(private val dbPath: String = getAppDatabasePath()) {

    private val url = "jdbc:sqlite:$dbPath"

    init {
        try {
            logApp("Initializing DatabaseManager with URL: $url")
            val file = File(dbPath)
            file.parentFile?.mkdirs()

            Class.forName("org.sqlite.JDBC")
            logApp("SQLite JDBC driver loaded successfully.")
        } catch (e: Throwable) {
            logApp("ERROR loading JDBC driver or preparing path: ${e.stackTraceToString()}")
        }
        initDatabase()
    }

    private fun getConnection(): Connection = DriverManager.getConnection(url)

    private fun initDatabase() {
        try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute(
                        """
                        CREATE TABLE IF NOT EXISTS records (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            project_name TEXT NOT NULL,
                            duration_seconds INTEGER NOT NULL,
                            timestamp TEXT NOT NULL
                        )
                        """.trimIndent()
                    )
                }
            }
            logApp("initDatabase completed successfully.")
        } catch (e: Throwable) {
            logApp("ERROR in initDatabase: ${e.stackTraceToString()}")
        }
    }

    suspend fun insertRecord(projectName: String, durationSeconds: Long, timestamp: Instant): TimeRecord =
        withContext(Dispatchers.IO) {
            val finalProjectName = if (projectName.isBlank()) "Bez projektu" else projectName.trim()
            val isoTimestamp = timestamp.toString()

            var generatedId: Long = 0
            try {
                getConnection().use { conn ->
                    val sql = "INSERT INTO records (project_name, duration_seconds, timestamp) VALUES (?, ?, ?)"
                    conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
                        stmt.setString(1, finalProjectName)
                        stmt.setLong(2, durationSeconds)
                        stmt.setString(3, isoTimestamp)
                        stmt.executeUpdate()

                        stmt.generatedKeys.use { rs ->
                            if (rs.next()) {
                                generatedId = rs.getLong(1)
                            }
                        }
                    }
                }
                logApp("Inserted record id=$generatedId, project=$finalProjectName, duration=$durationSeconds")
            } catch (e: Throwable) {
                logApp("ERROR in insertRecord: ${e.stackTraceToString()}")
            }

            TimeRecord(
                id = generatedId,
                projectName = finalProjectName,
                durationSeconds = durationSeconds,
                timestamp = timestamp
            )
        }

    suspend fun updateRecord(id: Long, projectName: String, durationSeconds: Long, timestamp: Instant) =
        withContext(Dispatchers.IO) {
            val finalProjectName = if (projectName.isBlank()) "Bez projektu" else projectName.trim()
            val isoTimestamp = timestamp.toString()

            try {
                getConnection().use { conn ->
                    val sql = "UPDATE records SET project_name = ?, duration_seconds = ?, timestamp = ? WHERE id = ?"
                    conn.prepareStatement(sql).use { stmt ->
                        stmt.setString(1, finalProjectName)
                        stmt.setLong(2, durationSeconds)
                        stmt.setString(3, isoTimestamp)
                        stmt.setLong(4, id)
                        stmt.executeUpdate()
                    }
                }
                logApp("Updated record id=$id, project=$finalProjectName")
            } catch (e: Throwable) {
                logApp("ERROR in updateRecord: ${e.stackTraceToString()}")
            }
        }

    suspend fun deleteRecord(id: Long) = withContext(Dispatchers.IO) {
        try {
            getConnection().use { conn ->
                val sql = "DELETE FROM records WHERE id = ?"
                conn.prepareStatement(sql).use { stmt ->
                    stmt.setLong(1, id)
                    stmt.executeUpdate()
                }
            }
            logApp("Deleted record id=$id")
        } catch (e: Throwable) {
            logApp("ERROR in deleteRecord: ${e.stackTraceToString()}")
        }
    }

    suspend fun getRecords(): List<TimeRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<TimeRecord>()
        try {
            getConnection().use { conn ->
                val sql = "SELECT id, project_name, duration_seconds, timestamp FROM records ORDER BY timestamp DESC"
                conn.createStatement().use { stmt ->
                    stmt.executeQuery(sql).use { rs ->
                        while (rs.next()) {
                            val id = rs.getLong("id")
                            val projName = rs.getString("project_name")
                            val durationSec = rs.getLong("duration_seconds")
                            val timestampStr = rs.getString("timestamp")

                            val instant = try {
                                Instant.parse(timestampStr)
                            } catch (_: Exception) {
                                Instant.fromEpochMilliseconds(0)
                            }
                            records.add(TimeRecord(id, projName, durationSec, instant))
                        }
                    }
                }
            }
            logApp("getRecords returned ${records.size} records.")
        } catch (e: Throwable) {
            logApp("ERROR in getRecords: ${e.stackTraceToString()}")
        }
        records
    }
}
