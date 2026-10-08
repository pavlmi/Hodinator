package cz.hodinator.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.components.AppDialog
import cz.hodinator.ui.components.DialogButtons
import cz.hodinator.ui.components.DialogText
import cz.hodinator.ui.components.DialogTitle
import cz.hodinator.ui.theme.StopRed


@Composable fun DeleteConfirmDialog(record: TimeRecord, onDismiss: () -> Unit, onConfirmDelete: () -> Unit) {
    AppDialog(onDismiss = onDismiss, width = 400.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(StopRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Delete, contentDescription = null, Modifier.size(20.dp), tint = StopRed) }
            DialogTitle("Smazat záznam")
        }

        DialogText("Opravdu chcete smazat záznam \"${record.projectName}\"? Tato akce je nevratná.")

        DialogButtons(
            confirmLabel = "Smazat",
            confirmColor = StopRed,
            onDismiss = onDismiss,
            onConfirm = onConfirmDelete,
        )
    }
}
