package cz.pavlik.timetracker

import cz.pavlik.timetracker.data.DatabaseManager
import cz.pavlik.timetracker.models.NO_PROJECT_NAME
import cz.pavlik.timetracker.models.TimeRecord
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseManagerTest {
    private val tempDir: File = createTempDirectory("hodinator-test").toFile()
    private val db = DatabaseManager(File(tempDir, "test.db"))

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
}
