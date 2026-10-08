package cz.pavlik.timetracker

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cz.pavlik.timetracker.data.AppFiles
import cz.pavlik.timetracker.data.AppLog
import cz.pavlik.timetracker.ui.App
import cz.pavlik.timetracker.ui.TimeTrackerViewModel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin

fun main() {
    AppLog.info("Spuštění aplikace, databáze: ${AppFiles.databaseFile.absolutePath}")
    val koin = startKoin { modules(appModule) }.koin
    val viewModel = koin.get<TimeTrackerViewModel>()

    application {
        Window(
            title = "Hodinátor",
            state = rememberWindowState(width = 1100.dp, height = 800.dp),
            onCloseRequest = {
                // Běžící časovač by se jinak při zavření okna ztratil.
                runBlocking {
                    runCatching { viewModel.saveRunningTimer() }
                        .onFailure { AppLog.error("Uložení běžícího časovače při ukončení selhalo", it) }
                }
                exitApplication()
            },
        ) { App(viewModel) }
    }
}
