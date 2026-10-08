package cz.hodinator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.theme.AppColorScheme
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.StopRed
import cz.hodinator.ui.theme.SurfaceDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary

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

        // While hiding, state.toast is already null, so keep the last message around for the exit animation.
        var lastToast by remember { mutableStateOf(state.toast) }
        state.toast?.let { lastToast = it }
        AnimatedVisibility(
            visible = state.toast != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        ) {
            lastToast?.let { ToastNotification(it, onDismiss = viewModel::dismissToast) }
        }
    }
}

@Composable private fun ToastNotification(toast: Toast, onDismiss: () -> Unit) {
    val accent = if (toast.isError) StopRed else PrimaryEmerald
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, accent),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (toast.isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accent,
            )
            Text(
                text = toast.message,
                style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDismiss, Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Zavřít", Modifier.size(14.dp), tint = TextSecondary)
            }
        }
    }
}
