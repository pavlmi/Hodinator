package cz.hodinator

import cz.hodinator.data.AppFiles
import cz.hodinator.data.DatabaseManager
import cz.hodinator.ui.TimeTrackerViewModel
import org.koin.dsl.module


val appModule = module {
    single { DatabaseManager(AppFiles.databaseFile) }
    single { TimeTrackerViewModel(get()) }
}
