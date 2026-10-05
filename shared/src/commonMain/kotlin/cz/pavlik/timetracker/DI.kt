package cz.pavlik.timetracker

import cz.pavlik.timetracker.data.DatabaseManager
import org.koin.dsl.module


val appModule = module {
    single { DatabaseManager() }
}
