package cz.pavlik.timetracker

import cz.pavlik.timetracker.data.AppFiles
import cz.pavlik.timetracker.data.DatabaseManager
import cz.pavlik.timetracker.ui.TimeTrackerViewModel
import org.koin.dsl.module

val appModule = module {
    single { DatabaseManager(AppFiles.databaseFile) }
    single { TimeTrackerViewModel(get()) }
}
