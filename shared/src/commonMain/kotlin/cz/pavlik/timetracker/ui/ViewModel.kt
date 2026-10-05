package cz.pavlik.timetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.pavlik.timetracker.data.DatabaseManager
import cz.pavlik.timetracker.models.TimeFilter
import cz.pavlik.timetracker.models.TimeRecord
import cz.pavlik.timetracker.utils.CsvExportUtils
import cz.pavlik.timetracker.utils.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.time.Duration.Companion.seconds


class TimeTrackerViewModel : ViewModel(), KoinComponent {
    private val dbManager: DatabaseManager by inject()
    private val _uiState = MutableStateFlow(TimeTrackerState())
    val uiState: StateFlow<TimeTrackerState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var toastJob: Job? = null

    init { loadRecords() }

    fun loadRecords() { viewModelScope.launch(Dispatchers.IO) {
        val loaded = dbManager.getRecords()
        _uiState.update { it.copy(records = loaded) }
    } }

    fun onProjectNameChange(newName: String) {
        _uiState.update { it.copy(projectName = newName) }
    }

    fun onFilterSelected(filter: TimeFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }

    fun onCustomFromChange(dateStr: String) {
        _uiState.update { it.copy(customFromStr = dateStr) }
    }

    fun onCustomToChange(dateStr: String) {
        _uiState.update { it.copy(customToStr = dateStr) }
    }

    fun toggleTimer() {
        val currentState = _uiState.value
        if (currentState.isRunning) {

            timerJob?.cancel()
            val stopTime = currentState.startTime ?: Clock.System.now()
            val name = currentState.projectName
            val duration = currentState.elapsedSeconds

            _uiState.update {
                it.copy(
                    isRunning = false,
                    elapsedSeconds = 0L,
                    startTime = null,
                    projectName = "",
                )
            }

            viewModelScope.launch(Dispatchers.IO) {
                dbManager.insertRecord(
                    projectName = name,
                    durationSeconds = duration,
                    timestamp = stopTime
                )
                loadRecords()
            }
        } else {
            val now = Clock.System.now()
            _uiState.update {
                it.copy(
                    isRunning = true,
                    startTime = now,
                    elapsedSeconds = 0L,
                )
            }

            timerJob = viewModelScope.launch {
                while (true) {
                    delay(1.seconds)
                    _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun duplicateRecord(record: TimeRecord) {
        _uiState.update { it.copy(projectName = record.projectName) }
        if (!_uiState.value.isRunning) toggleTimer()
    }

    fun copyRecord(record: TimeRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            dbManager.insertRecord(
                projectName = record.projectName,
                durationSeconds = record.durationSeconds,
                timestamp = record.timestamp,
            )
            loadRecords()
            showToast("Kopie záznamu byla vytvořena")
        }
    }

    fun deleteRecord(recordId: Long) { viewModelScope.launch(Dispatchers.IO) {
        dbManager.deleteRecord(recordId)
        loadRecords()
    } }

    fun updateRecord(id: Long, name: String, startInstant: Instant, durationSeconds: Long) { viewModelScope.launch(Dispatchers.IO) {
        dbManager.updateRecord(id, name, durationSeconds, startInstant)
        loadRecords()
    } }

    fun exportMonthlyCsv(year: Int, monthNumber: Int): File? {
        val csvContent = TimeUtils.generateMonthlyCsv(_uiState.value.records, year, monthNumber)
        val defaultFileName = "TimeTracker_Prehled_${year}_${"%02d".format(monthNumber)}.csv"
        val savedFile = CsvExportUtils.saveCsvFile(defaultFileName, csvContent)
        if (savedFile != null) showToast("CSV soubor byl úspěšně uložen: ${savedFile.name}")
        return savedFile
    }

    fun showToast(message: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = message) }
        toastJob = viewModelScope.launch {
            delay(3.5.seconds)
            _uiState.update { it.copy(toastMessage = null) }
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = null) }
    }
}

data class TimeTrackerState(
    val records: List<TimeRecord> = emptyList(),
    val currentFilter: TimeFilter = TimeFilter.ALL,
    val customFromStr: String = "",
    val customToStr: String = "",
    val isRunning: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val startTime: Instant? = null,
    val projectName: String = "",
    val toastMessage: String? = null
) {
    val filteredRecords: List<TimeRecord>
        get() {
            if (currentFilter == TimeFilter.ALL) return records

            val from = TimeUtils.parseDate(customFromStr)
            val to = TimeUtils.parseDate(customToStr)
            return TimeUtils.filterRecords(
                records = records,
                filter = currentFilter,
                now = Clock.System.now(),
                fromDate = from,
                toDate = to,
            )
        }

    val totalSeconds get() = filteredRecords.sumOf { it.durationSeconds }
}
