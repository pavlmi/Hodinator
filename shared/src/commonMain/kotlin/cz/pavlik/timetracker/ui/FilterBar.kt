package cz.pavlik.timetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.pavlik.timetracker.models.TimeFilter
import cz.pavlik.timetracker.ui.components.SectionCard
import cz.pavlik.timetracker.ui.components.SelectablePill
import cz.pavlik.timetracker.ui.components.ToolbarButton
import cz.pavlik.timetracker.ui.components.appTextFieldColors
import cz.pavlik.timetracker.ui.theme.PrimaryEmerald
import cz.pavlik.timetracker.ui.theme.TextSecondary
import cz.pavlik.timetracker.utils.TimeUtils

private val TimeFilter.label: String
    get() = when (this) {
        TimeFilter.ALL -> "Vše"
        TimeFilter.TODAY -> "Dnes"
        TimeFilter.THIS_MONTH -> "Tento měsíc"
        TimeFilter.LAST_MONTH -> "Minulý měsíc"
        TimeFilter.CUSTOM -> "Vlastní rozsah"
    }

@Composable fun FilterBar(
    currentFilter: TimeFilter,
    totalSeconds: Long,
    customFromText: String,
    customToText: String,
    onFilterSelect: (TimeFilter) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onOpenExportDialog: () -> Unit,
    onOpenStatsDialog: () -> Unit,
) {
    SectionCard {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeFilter.entries.forEach { filter ->
                        SelectablePill(filter.label, selected = filter == currentFilter, onClick = { onFilterSelect(filter) })
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToolbarButton(Icons.Rounded.BarChart, "Přehled", onClick = onOpenStatsDialog)
                    ToolbarButton(Icons.Rounded.Download, "Export CSV", onClick = onOpenExportDialog)

                    Column(horizontalAlignment = Alignment.End) {
                        Text("CELKEM", style = TextStyle(color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                        Text(
                            text = TimeUtils.formatSeconds(totalSeconds),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = PrimaryEmerald,
                            ),
                        )
                    }
                }
            }

            if (currentFilter == TimeFilter.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateField(customFromText, onFromChange, "Od (např. 01.10.2026)", Modifier.weight(1f))
                    DateField(customToText, onToChange, "Do (např. 31.10.2026)", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable private fun DateField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, fontSize = 12.sp, color = TextSecondary) },
        singleLine = true,
        isError = value.isNotBlank() && TimeUtils.parseDate(value) == null,
        shape = RoundedCornerShape(8.dp),
        colors = appTextFieldColors(),
    )
}
