package cz.hodinator

import cz.hodinator.data.DatabaseManager
import cz.hodinator.models.NO_PROJECT_NAME
import cz.hodinator.models.TimeRecord
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.io.File
import java.sql.DriverManager
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseManagerTest {
    private val tempDir: File = createTempDirectory("hodinator-test").toFile()
    private val dbFile = File(tempDir, "test.db")
    private val db = DatabaseManager(dbFile)

    @AfterTest fun cleanUp() {
        tempDir.deleteRecursively()
    }

    @Test fun insertsUpdatesAndDeletesRecords() = runBlocking {
        val older = TimeRecord(projectName = "  Alfa  ", durationSeconds = 60, startTime = Instant.parse("2026-10-01T08:00:00.123456Z"))
        val newer = TimeRecord(projectName = " ", durationSeconds = 120, startTime = Instant.parse("2026-10-02T08:00:00Z"))
        db.insertRecord(older)
        db.insertRecord(newer)

        val (loadedNewer, loadedOlder) = db.getRecords()
        assertEquals(NO_PROJECT_NAME, loadedNewer.projectName)
        assertEquals("Alfa", loadedOlder.projectName)
        assertEquals(older.startTime, loadedOlder.startTime)

        val edited = loadedOlder.copy(projectName = "Beta", durationSeconds = 90)
        db.updateRecord(edited)
        assertEquals(edited, db.getRecords().single { it.id == edited.id })

        db.deleteRecord(loadedNewer.id)
        assertEquals(listOf(edited), db.getRecords())
    }

    @Test fun normalizesProjectNamesWithLineBreaks() = runBlocking {
        db.insertRecord(TimeRecord(projectName = "[PW-1] Úkol\n\n\n  z Jiry\r\n", durationSeconds = 60, startTime = Instant.parse("2026-10-01T08:00:00Z")))
        // A record saved by an older version, before names were normalized.
        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use {
                it.execute("INSERT INTO records (project_name, duration_seconds, timestamp) VALUES ('Starý\n\n]', 60, '2026-10-02T08:00:00Z')")
            }
        }

        assertEquals(listOf("Starý ]", "[PW-1] Úkol z Jiry"), db.getRecords().map { it.projectName })
    }
}
