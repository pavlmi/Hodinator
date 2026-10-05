package cz.pavlik.timetracker

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cz.pavlik.timetracker.ui.App
import org.koin.core.context.startKoin

fun main() {
    startKoin {
        modules(appModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Hodinátor",
        ) { App() }
    }
}
