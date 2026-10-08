package cz.hodinator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.hodinator.data.AppLog
import cz.hodinator.data.DatabaseManager
import cz.hodinator.models.TimeFilter
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.dialogs.chooseCsvSaveFile
import cz.hodinator.utils.CsvExport
import cz.hodinator.utils.TimeUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds


class TimeTrackerViewModel(private val db: DatabaseManager) : ViewModel() {
    private val _uiState = MutableStateFlow(TimeTrackerState())
    val uiState: StateFlow<TimeTrackerState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var toastJob: Job? = null

    init { launchDbAction("Nepodařilo se načíst záznamy") { reloadRecords() } }

    fun onProjectNameChange(newName: String) = _uiState.update { it.copy(projectName = newName) }

    fun onFilterSelected(filter: TimeFilter) = _uiState.update { it.copy(currentFilter = filter) }

    fun onCustomFromChange(text: String) = _uiState.update { it.copy(customFromText = text) }

    fun onCustomToChange(text: String) = _uiState.update { it.copy(customToText = text) }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            launchDbAction("Záznam se nepodařilo uložit") {
                saveRunningTimer()
                reloadRecords(scrollToTop = true)
            }
        } else startTimer(_uiState.value.projectName)
    }

    /** Starts the timer for the record's project. A running timer is saved first. */
    fun startAgain(record: TimeRecord) = launchDbAction("Záznam se nepodařilo uložit") {
        if (_uiState.value.isRunning) {
            saveRunningTimer()
            reloadRecords(scrollToTop = true)
        }
        startTimer(record.projectName)
    }

    /**
     * Stops the running timer and saves it as a record. Also called when the window closes,
     * so tracked time isn't lost.
     */
    suspend fun saveRunningTimer() {
        val state = _uiState.value
        val startTime = state.startTime ?: return
        timerJob?.cancel()
        _uiState.update { it.copy(startTime = null, elapsedSeconds = 0L, projectName = "") }
        val record = TimeRecord(
            projectName = state.projectName,
            durationSeconds = (Clock.System.now() - startTime).inWholeSeconds,
            startTime = startTime,
        )
        try {
            db.insertRecord(record)
        } catch (e: Exception) {
            // The timer is already stopped, so the log must contain everything needed to restore the record manually.
            AppLog.error("Unsaved record: $record")
            throw e
        }
    }

    fun copyRecord(record: TimeRecord) = launchDbAction("Kopii záznamu se nepodařilo vytvořit") {
        db.insertRecord(record)
        reloadRecords()
        showToast("Kopie záznamu byla vytvořena")
    }

    fun updateRecord(record: TimeRecord) = launchDbAction("Změnu záznamu se nepodařilo uložit") {
        db.updateRecord(record)
        reloadRecords()
    }

    fun deleteRecord(record: TimeRecord) = launchDbAction("Záznam se nepodařilo smazat") {
        db.deleteRecord(record.id)
        reloadRecords()
        showToast("Záznam byl smazán")
    }

    fun exportMonthlyCsv(year: Int, monthNumber: Int) {
        val file = chooseCsvSaveFile(
            title = "Uložit CSV měsíční přehled",
            defaultFileName = CsvExport.defaultMonthlyFileName(year, monthNumber),
        ) ?: return
        val content = CsvExport.monthlySummary(_uiState.value.records, year, monthNumber)

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { file.writeText(content, Charsets.UTF_8) }
                showToast("CSV soubor byl uložen: ${file.name}")
            } catch (e: Exception) {
                AppLog.error("Writing CSV to ${file.absolutePath} failed", e)
                showToast("CSV soubor se nepodařilo uložit", isError = true)
            }
        }
    }

    fun dismissToast() {
        toastJob?.cancel()
        _uiState.update { it.copy(toast = null) }
    }

    private fun startTimer(projectName: String) {
        val startTime = Clock.System.now()
        _uiState.update { it.copy(projectName = projectName, startTime = startTime, elapsedSeconds = 0L) }

        // Elapsed time is derived from the start time, so it doesn't drift and stays correct after the computer sleeps.
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)
                _uiState.update { it.copy(elapsedSeconds = (Clock.System.now() - startTime).inWholeSeconds) }
            }
        }
    }

    /** Reloads all records; with [scrollToTop] the list scrolls to the top, where a newly tracked record appears. */
    private suspend fun reloadRecords(scrollToTop: Boolean = false) {
        val records = db.getRecords()
        _uiState.update {
            it.copy(
                records = records,
                scrollToTopRequest = if (scrollToTop) it.scrollToTopRequest + 1 else it.scrollToTopRequest,
            )
        }
    }

    private fun showToast(message: String, isError: Boolean = false) {
        toastJob?.cancel()
        _uiState.update { it.copy(toast = Toast(message, isError)) }
        toastJob = viewModelScope.launch {
            delay(3.5.seconds)
            _uiState.update { it.copy(toast = null) }
        }
    }

    private fun launchDbAction(errorMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                AppLog.error(errorMessage, e)
                showToast(errorMessage, isError = true)
            }
        }
    }
}

data class Toast(val message: String, val isError: Boolean = false)

data class TimeTrackerState(
    val records: List<TimeRecord> = emptyList(),
    val currentFilter: TimeFilter = TimeFilter.ALL,
    val customFromText: String = "",
    val customToText: String = "",
    val startTime: Instant? = null,
    val elapsedSeconds: Long = 0L,
    val projectName: String = "",
    val toast: Toast? = null,
    /**
     * Incremented whenever the record list should scroll to the top. Kept in the state (rather than sent as
     * a one-off event) so it arrives together with the new records and the list scrolls after showing them.
     */
    val scrollToTopRequest: Int = 0,
) {
    val isRunning: Boolean get() = startTime != null

    val filteredRecords: List<TimeRecord> by lazy {
        TimeUtils.filterRecords(
            records = records,
            filter = currentFilter,
            fromDate = TimeUtils.parseDate(customFromText),
            toDate = TimeUtils.parseDate(customToText),
        )
    }

    val totalSeconds: Long by lazy { filteredRecords.sumOf { it.durationSeconds } }
}
