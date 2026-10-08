package cz.hodinator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.components.ToastNotification
import cz.hodinator.ui.dialogs.DeleteConfirmDialog
import cz.hodinator.ui.dialogs.ExportCsvDialog
import cz.hodinator.ui.dialogs.MonthlyStatsDialog
import cz.hodinator.ui.screen.FilterBar
import cz.hodinator.ui.screen.RecordList
import cz.hodinator.ui.screen.TimerBar
import cz.hodinator.ui.theme.AppColorScheme


@Composable fun App(viewModel: TimeTrackerViewModel) {
    MaterialTheme(colorScheme = AppColorScheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            TimeTrackerScreen(viewModel)
        }
    }
}

@Composable private fun TimeTrackerScreen(viewModel: TimeTrackerViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<TimeRecord?>(null) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TimerBar(
                projectName = state.projectName,
                elapsedSeconds = state.elapsedSeconds,
                isRunning = state.isRunning,
                onProjectNameChange = viewModel::onProjectNameChange,
                onToggleTimer = viewModel::toggleTimer,
            )

            FilterBar(
                currentFilter = state.currentFilter,
                totalSeconds = state.totalSeconds,
                customFromText = state.customFromText,
                customToText = state.customToText,
                onFilterSelect = viewModel::onFilterSelected,
                onFromChange = viewModel::onCustomFromChange,
                onToChange = viewModel::onCustomToChange,
                onOpenExportDialog = { showExportDialog = true },
                onOpenStatsDialog = { showStatsDialog = true },
            )

            RecordList(
                records = state.filteredRecords,
                scrollToTopRequest = state.scrollToTopRequest,
                onStartAgain = viewModel::startAgain,
                onCopy = viewModel::copyRecord,
                onDelete = { recordToDelete = it },
                onUpdate = viewModel::updateRecord,
            )
        }

        if (showExportDialog) {
            ExportCsvDialog(
                onDismiss = { showExportDialog = false },
                onExport = viewModel::exportMonthlyCsv,
            )
        }

        if (showStatsDialog) {
            MonthlyStatsDialog(
                records = state.records,
                onDismiss = { showStatsDialog = false },
            )
        }

        recordToDelete?.let { record ->
            DeleteConfirmDialog(
                record = record,
                onDismiss = { recordToDelete = null },
                onConfirmDelete = { viewModel.deleteRecord(record) },
            )
        }

        var lastToast by remember { mutableStateOf(state.toast) }
        state.toast?.let { lastToast = it }
        AnimatedVisibility(
            visible = state.toast != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        ) {
            lastToast?.let { ToastNotification(it.message, it.isError, onDismiss = viewModel::dismissToast) }
        }
    }
}
