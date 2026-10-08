package cz.pavlik.timetracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cz.pavlik.timetracker.data.AppLog
import cz.pavlik.timetracker.data.DatabaseManager
import cz.pavlik.timetracker.models.TimeFilter
import cz.pavlik.timetracker.models.TimeRecord
import cz.pavlik.timetracker.utils.CsvExport
import cz.pavlik.timetracker.utils.TimeUtils
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

    init {
        launchDbAction("Nepodařilo se načíst záznamy") { reloadRecords() }
    }

    fun onProjectNameChange(newName: String) = _uiState.update { it.copy(projectName = newName) }

    fun onFilterSelected(filter: TimeFilter) = _uiState.update { it.copy(currentFilter = filter) }

    fun onCustomFromChange(text: String) = _uiState.update { it.copy(customFromText = text) }

    fun onCustomToChange(text: String) = _uiState.update { it.copy(customToText = text) }

    fun toggleTimer() {
        if (_uiState.value.isRunning) {
            launchDbAction("Záznam se nepodařilo uložit") {
                saveRunningTimer()
                reloadRecords()
            }
        } else {
            startTimer(_uiState.value.projectName)
        }
    }

    /** Spustí časovač pro projekt daného záznamu. Případný běžící časovač se nejdřív uloží. */
    fun startAgain(record: TimeRecord) = launchDbAction("Záznam se nepodařilo uložit") {
        if (_uiState.value.isRunning) {
            saveRunningTimer()
            reloadRecords()
        }
        startTimer(record.projectName)
    }

    /**
     * Zastaví běžící časovač a uloží ho jako záznam. Volá se i při zavírání okna,
     * aby se rozpracovaný čas neztratil.
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
            // Časovač už je zastavený, takže v logu musí zůstat vše potřebné k ruční obnově záznamu.
            AppLog.error("Neuložený záznam: $record")
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
                AppLog.error("Zápis CSV do ${file.absolutePath} selhal", e)
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

        // Uplynulý čas se počítá z času spuštění, takže nedrifuje a počítá správně i po uspání počítače.
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1.seconds)
                _uiState.update { it.copy(elapsedSeconds = (Clock.System.now() - startTime).inWholeSeconds) }
            }
        }
    }

    private suspend fun reloadRecords() {
        val records = db.getRecords()
        _uiState.update { it.copy(records = records) }
    }

    private fun showToast(message: String, isError: Boolean = false) {
        toastJob?.cancel()
        _uiState.update { it.copy(toast = Toast(message, isError)) }
        toastJob = viewModelScope.launch {
            delay(3.5.seconds)
            _uiState.update { it.copy(toast = null) }
        }
    }

    /** Spustí databázovou operaci; při chybě ji zaloguje a zobrazí uživateli [errorMessage]. */
    private fun launchDbAction(errorMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
) {
    val isRunning: Boolean get() = startTime != null

    // Lazy, aby se filtr nepřepočítával při každém ticku časovače víc než jednou.
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
