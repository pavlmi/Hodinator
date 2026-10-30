package cz.hodinator.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.components.AppDialog
import cz.hodinator.ui.components.DialogButtons
import cz.hodinator.ui.components.DialogTitle
import cz.hodinator.ui.components.ProjectNameField
import cz.hodinator.ui.components.appTextFieldColors
import cz.hodinator.ui.components.withoutLineBreaks
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.StopRed
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary
import cz.hodinator.utils.TimeUtils
import kotlinx.datetime.LocalDate


private const val DEFAULT_NAME = "Práce"
private val LabelStyle = TextStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)

@Composable fun AddRecordDialog(date: LocalDate, projectNames: List<String>, onDismiss: () -> Unit, onAdd: (TimeRecord) -> Unit) {
    var name by remember { mutableStateOf(projectNames.firstOrNull() ?: DEFAULT_NAME) }
    var fromText by remember { mutableStateOf("08:00") }
    var toText by remember { mutableStateOf("17:00") }
    val from = TimeUtils.parseTime(fromText)
    val to = TimeUtils.parseTime(toText)
    val record = if (from != null && to != null) TimeUtils.recordForDay(name, date, from, to) else null

    AppDialog(onDismiss = onDismiss, width = 420.dp) {
        DialogTitle("Přidat záznam – ${TimeUtils.dayWithDate(date)}")

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Aktivita:", style = LabelStyle)
            ProjectNameField(name, projectNames, onProjectNameChange = { name = it }, Modifier.fillMaxWidth())
        }

        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField("Od:", fromText, isError = from == null) { fromText = it }
            TimeField("Do:", toText, isError = to == null) { toText = it }
            Text(
                when {
                    from == null || to == null -> "Zadej čas ve tvaru HH:mm"
                    record == null -> "Konec musí být po začátku"
                    else -> TimeUtils.formatHoursMinutes(record.durationSeconds)
                },
                Modifier.weight(1f),
                style = TextStyle(
                    color = if (record == null) StopRed else TextPrimary,
                    fontSize = if (record == null) 12.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (record == null) null else FontFamily.Monospace,
                ),
            )
        }

        DialogButtons(
            confirmLabel = "Přidat",
            confirmColor = PrimaryEmerald,
            confirmIcon = Icons.Rounded.Add,
            confirmEnabled = record != null,
            onDismiss = onDismiss,
            onConfirm = { record?.let(onAdd) },
        )
    }
}

@Composable private fun TimeField(label: String, value: String, isError: Boolean, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = LabelStyle)
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.withoutLineBreaks()) },
            Modifier.width(96.dp),
            singleLine = true,
            isError = isError,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
            shape = RoundedCornerShape(10.dp),
            colors = appTextFieldColors(),
        )
    }
}
