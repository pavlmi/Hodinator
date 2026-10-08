package cz.hodinator.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.ui.components.AppDialog
import cz.hodinator.ui.components.DialogButtons
import cz.hodinator.ui.components.DialogText
import cz.hodinator.ui.components.DialogTitle
import cz.hodinator.ui.components.SelectablePill
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary
import cz.hodinator.utils.TimeUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


private val LabelStyle = TextStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)

@Composable fun ExportCsvDialog(onDismiss: () -> Unit, onExport: (year: Int, monthNumber: Int) -> Unit) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    var selectedYear by remember { mutableStateOf(today.year) }
    var selectedMonth by remember { mutableStateOf(today.monthNumber) }

    AppDialog(onDismiss = onDismiss, width = 440.dp) {
        DialogTitle("Exportovat měsíční přehled")
        DialogText("Vyberte měsíc a rok pro vygenerování souboru .csv s agregovanými časy podle projektů.")

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Měsíc:", style = LabelStyle)
            (1..12).chunked(4).forEach { monthsInRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    monthsInRow.forEach { month ->
                        SelectablePill(
                            label = TimeUtils.monthName(month),
                            selected = month == selectedMonth,
                            onClick = { selectedMonth = month },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Rok:", style = LabelStyle)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StepButton("-") { selectedYear-- }
                Text(
                    selectedYear.toString(),
                    style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary),
                )
                StepButton("+") { selectedYear++ }
            }
        }

        Spacer(Modifier.height(8.dp))

        DialogButtons(
            confirmLabel = "Exportovat CSV",
            confirmColor = PrimaryEmerald,
            confirmIcon = Icons.Rounded.Download,
            onDismiss = onDismiss,
            onConfirm = { onExport(selectedYear, selectedMonth) },
        )
    }
}

@Composable private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceVariantDark).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = TextPrimary, fontWeight = FontWeight.Bold) }
}
