package cz.hodinator

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cz.hodinator.data.AppFiles
import cz.hodinator.data.AppLog
import cz.hodinator.ui.App
import cz.hodinator.ui.TimeTrackerViewModel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import java.awt.Dimension

fun main() {
    AppLog.info("App started, database: ${AppFiles.databaseFile.absolutePath}")
    val koin = startKoin { modules(appModule) }.koin
    val viewModel = koin.get<TimeTrackerViewModel>()

    application {
        Window(
            title = "Hodinátor",
            state = rememberWindowState(width = 1100.dp, height = 800.dp),
            onCloseRequest = {
                // Otherwise a running timer would be lost when the window closes.
                runBlocking {
                    runCatching { viewModel.saveRunningTimer() }
                        .onFailure { AppLog.error("Failed to save the running timer on exit", it) }
                }
                exitApplication()
            },
        ) {
            // Below this size the layout no longer fits, so don't let the window shrink further.
            LaunchedEffect(Unit) { window.minimumSize = Dimension(720, 480) }
            App(viewModel)
        }
    }
}
