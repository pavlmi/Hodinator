package cz.hodinator.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.models.TimeFilter
import cz.hodinator.ui.components.SectionCard
import cz.hodinator.ui.components.SelectablePill
import cz.hodinator.ui.components.ToolbarButton
import cz.hodinator.ui.components.appTextFieldColors
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.TextSecondary
import cz.hodinator.utils.TimeUtils

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
        BoxWithConstraints(Modifier.padding(16.dp)) {
            // Below this width the toolbar buttons show only icons so the filters keep enough room.
            val compact = maxWidth < 840.dp

            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Scrolls horizontally instead of wrapping when there still isn't enough room.
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimeFilter.entries.forEach { filter ->
                            SelectablePill(filter.label, selected = filter == currentFilter, onClick = { onFilterSelect(filter) })
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
                    ) {
                        ToolbarButton(Icons.Rounded.BarChart, "Přehled", onClick = onOpenStatsDialog, showLabel = !compact)
                        ToolbarButton(Icons.Rounded.Download, "Export CSV", onClick = onOpenExportDialog, showLabel = !compact)

                        Column(horizontalAlignment = Alignment.End) {
                            Text("CELKEM", softWrap = false, style = TextStyle(color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                            Text(
                                text = TimeUtils.formatSeconds(totalSeconds),
                                softWrap = false,
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
}

@Composable private fun DateField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        singleLine = true,
        isError = value.isNotBlank() && TimeUtils.parseDate(value) == null,
        shape = RoundedCornerShape(8.dp),
        colors = appTextFieldColors(),
    )
}
