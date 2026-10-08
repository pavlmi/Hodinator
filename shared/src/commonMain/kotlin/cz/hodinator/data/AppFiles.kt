package cz.hodinator.data

import kotlinx.datetime.Clock
import java.io.File


object AppFiles {
    private const val APP_DIR_NAME = "Hodinator"

    private val userHome = File(System.getProperty("user.home") ?: ".")
    private val isMac = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)
    private val isWindows = System.getProperty("os.name").orEmpty().contains("win", ignoreCase = true)

    val dataDir: File
        get() = when {
            isMac -> File(userHome, "Library/Application Support/$APP_DIR_NAME")
            isWindows -> File(System.getenv("APPDATA") ?: "$userHome/AppData/Roaming", APP_DIR_NAME)
            else -> File(userHome, ".local/share/$APP_DIR_NAME")
        }

    val databaseFile: File get() = File(dataDir, "time_tracker.db")

    val logFile: File
        get() = if (isMac) File(userHome, "Library/Logs/$APP_DIR_NAME.log") else File(dataDir, "$APP_DIR_NAME.log")
}

/** Minimal logger writing to [AppFiles.logFile] and stdout. Logging must never crash the app. */
object AppLog {
    fun info(message: String) = write("INFO", message)

    fun error(message: String, throwable: Throwable? = null) =
        write("ERROR", if (throwable == null) message else "$message\n${throwable.stackTraceToString()}")

    private fun write(level: String, message: String) {
        val line = "${Clock.System.now()} $level: $message"
        println(line)
        try {
            AppFiles.logFile.apply { parentFile?.mkdirs() }.appendText(line + "\n")
        } catch (_: Exception) {
        }
    }
}
