package cz.pavlik.timetracker.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.pavlik.timetracker.models.TimeFilter
import cz.pavlik.timetracker.models.TimeRecord
import cz.pavlik.timetracker.ui.utils.BorderDark
import cz.pavlik.timetracker.ui.utils.PrimaryEmerald
import cz.pavlik.timetracker.ui.utils.StopRed
import cz.pavlik.timetracker.ui.utils.SurfaceDark
import cz.pavlik.timetracker.ui.utils.SurfaceVariantDark
import cz.pavlik.timetracker.ui.utils.TextPrimary
import cz.pavlik.timetracker.ui.utils.TextSecondary
import cz.pavlik.timetracker.ui.utils.customColorScheme
import cz.pavlik.timetracker.utils.TimeUtils
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@Composable fun App() {
    MaterialTheme(colorScheme = customColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) { TimeTrackerScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimeTrackerScreen(viewModel: TimeTrackerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<TimeRecord?>(null) }

    Box(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                    customFromStr = state.customFromStr,
                    customToStr = state.customToStr,
                    onFilterSelect = viewModel::onFilterSelected,
                    onFromChange = viewModel::onCustomFromChange,
                    onToChange = viewModel::onCustomToChange,
                    onOpenExportDialog = { showExportDialog = true }
                )

                RecordList(
                    records = state.filteredRecords,
                    onStartAgain = viewModel::duplicateRecord,
                    onCopyRecord = viewModel::copyRecord,
                    onDelete = { record -> recordToDelete = record },
                    onUpdateRecord = viewModel::updateRecord,
                )
            }

            if (showExportDialog) {
                ExportCsvDialog(
                    onDismiss = { showExportDialog = false },
                    onExport = viewModel::exportMonthlyCsv,
                )
            }

            recordToDelete?.let { record ->
                DeleteConfirmDialog(
                    record = record,
                    onDismiss = { recordToDelete = null },
                    onConfirmDelete = {
                        viewModel.deleteRecord(record.id)
                        viewModel.showToast("Záznam byl smazán")
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = state.toastMessage != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
        ) {
            state.toastMessage?.let { msg ->
                ToastNotification(
                    message = msg,
                    onDismiss = viewModel::dismissToast,
                )
            }
        }
    }
}

@Composable private fun ToastNotification(message: String, onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, PrimaryEmerald),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                Modifier.size(20.dp),
                tint = PrimaryEmerald,
            )
            Text(
                text = message,
                style = TextStyle(
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDismiss, Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Zavřít",
                    Modifier.size(14.dp),
                    tint = TextSecondary,
                )
            }
        }
    }
}

@Composable private fun TimerBar(
    projectName: String,
    elapsedSeconds: Long,
    isRunning: Boolean,
    onProjectNameChange: (String) -> Unit,
    onToggleTimer: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = projectName,
                onValueChange = onProjectNameChange,
                placeholder = { Text("Na čem právě pracuješ?", color = TextSecondary, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryEmerald,
                    unfocusedBorderColor = BorderDark,
                    focusedContainerColor = SurfaceVariantDark,
                    unfocusedContainerColor = SurfaceVariantDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                ),
                leadingIcon = {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = if (isRunning) StopRed else PrimaryEmerald,
                    )
                },
            )

            Spacer(Modifier.width(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isRunning) Box(Modifier.size(8.dp).clip(CircleShape).background(StopRed))

                Text(
                    text = TimeUtils.formatSeconds(elapsedSeconds),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (isRunning) TextPrimary else TextSecondary,
                    ),
                )
            }

            Spacer(Modifier.width(16.dp))

            Button(
                onClick = onToggleTimer,
                Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) StopRed else PrimaryEmerald,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(text = if (isRunning) "STOP" else "START", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            }
        }
    }
}

@Composable private fun FilterBar(
    currentFilter: TimeFilter,
    totalSeconds: Long,
    customFromStr: String,
    customToStr: String,
    onFilterSelect: (TimeFilter) -> Unit,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onOpenExportDialog: () -> Unit,
) {
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterPill("Vše", currentFilter == TimeFilter.ALL) { onFilterSelect(TimeFilter.ALL) }
                    FilterPill("Dnes", currentFilter == TimeFilter.TODAY) { onFilterSelect(TimeFilter.TODAY) }
                    FilterPill("Tento měsíc", currentFilter == TimeFilter.THIS_MONTH) { onFilterSelect(TimeFilter.THIS_MONTH) }
                    FilterPill("Minulý měsíc", currentFilter == TimeFilter.LAST_MONTH) { onFilterSelect(TimeFilter.LAST_MONTH) }
                    FilterPill("Vlastní rozsah", currentFilter == TimeFilter.CUSTOM) { onFilterSelect(TimeFilter.CUSTOM) }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenExportDialog,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) { Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Download, contentDescription = "Export CSV", Modifier.size(16.dp), tint = PrimaryEmerald)
                        Spacer(Modifier.width(6.dp))
                        Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    } }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "CELKEM", style = TextStyle(color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold))
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
                    OutlinedTextField(
                        value = customFromStr,
                        onValueChange = onFromChange,
                        Modifier.weight(1f),
                        placeholder = { Text("Od (např. 01.10.2026)", fontSize = 12.sp, color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark,
                        ),
                    )
                    OutlinedTextField(
                        value = customToStr,
                        onValueChange = onToChange,
                        placeholder = { Text("Do (např. 31.10.2026)", fontSize = 12.sp, color = TextSecondary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryEmerald,
                            unfocusedBorderColor = BorderDark,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark,
                        ),
                    )
                }
            }
        }
    }
}

@Composable private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) PrimaryEmerald else SurfaceVariantDark)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = if (selected) Color.White else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

@Composable private fun DayHeader(date: LocalDate, nowDate: LocalDate, dailyTotalSeconds: Long) {
    val dayTitle = remember(date, nowDate) {
        val dayWithDate = "${TimeUtils.dayOfWeekName(date)} ${TimeUtils.formatDate(date)}"
        when {
            date.toEpochDays() == nowDate.toEpochDays() -> "Dnes – $dayWithDate"
            date.toEpochDays() == nowDate.toEpochDays() - 1 -> "Včera – $dayWithDate"
            else -> dayWithDate
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(PrimaryEmerald),
            )
            Text(
                text = dayTitle,
                style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextPrimary,
                ),
            )
        }

        Text(
            text = "Denní celkem: ${TimeUtils.formatSeconds(dailyTotalSeconds)}",
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
            ),
        )
    }
}

@Composable private fun RecordList(
    records: List<TimeRecord>,
    onStartAgain: (TimeRecord) -> Unit,
    onCopyRecord: (TimeRecord) -> Unit,
    onDelete: (TimeRecord) -> Unit,
    onUpdateRecord: (Long, String, Instant, Long) -> Unit,
) {
    if (records.isEmpty()) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(text = "Žádné záznamy k zobrazení", color = TextSecondary, fontSize = 14.sp) }
    } else {
        val timeZone = remember { TimeZone.currentSystemDefault() }
        val nowLdt = remember { Clock.System.now().toLocalDateTime(timeZone) }

        val grouped = remember(records) {
            records.groupBy { it.timestamp.toLocalDateTime(timeZone).date }
        }

        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            grouped.forEach { (date, dayRecords) ->
                val dayTotalSeconds = dayRecords.sumOf { it.durationSeconds }

                item(key = "header_${date}") {
                    DayHeader(
                        date = date,
                        nowDate = nowLdt.date,
                        dailyTotalSeconds = dayTotalSeconds
                    )
                }

                items(dayRecords, key = { it.id }) { record ->
                    RecordItemCard(
                        record = record,
                        onStartAgain = { onStartAgain(record) },
                        onCopyRecord = { onCopyRecord(record) },
                        onDelete = { onDelete(record) },
                        onUpdateRecord = { name, start, duration ->
                            onUpdateRecord(record.id, name, start, duration)
                        },
                    )
                }
            }
        }
    }
}

@Composable private fun RecordItemCard(
    record: TimeRecord,
    onStartAgain: () -> Unit,
    onCopyRecord: () -> Unit,
    onDelete: () -> Unit,
    onUpdateRecord: (String, Instant, Long) -> Unit,
) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val startLdt = remember(record.timestamp) { record.timestamp.toLocalDateTime(timeZone) }
    val endInstant = remember(record.timestamp, record.durationSeconds) {
        Instant.fromEpochMilliseconds(record.timestamp.toEpochMilliseconds() + record.durationSeconds * 1000L)
    }
    val endLdt = remember(endInstant) { endInstant.toLocalDateTime(timeZone) }

    var nameText by remember(record) { mutableStateOf(record.projectName) }
    var dateText by remember(record) { mutableStateOf(TimeUtils.formatDate(record.timestamp, timeZone)) }
    var startText by remember(record) { mutableStateOf(TimeUtils.formatTime(startLdt)) }
    var endText by remember(record) { mutableStateOf(TimeUtils.formatTime(endLdt)) }

    fun resetTexts() {
        nameText = record.projectName
        dateText = TimeUtils.formatDate(record.timestamp, timeZone)
        startText = TimeUtils.formatTime(startLdt)
        endText = TimeUtils.formatTime(endLdt)
    }

    fun commitChanges() {
        val parsedDate = TimeUtils.parseDate(dateText)
        val parsedStart = TimeUtils.parseTime(startText)
        val parsedEnd = TimeUtils.parseTime(endText)
        if (parsedDate == null || parsedStart == null || parsedEnd == null) {
            resetTexts()
            return
        }

        val dayShift = parsedDate.toEpochDays() - startLdt.date.toEpochDays()
        val endDate = LocalDate.fromEpochDays(endLdt.date.toEpochDays() + dayShift)

        val newStartInstant = LocalDateTime(
            parsedDate.year, parsedDate.monthNumber, parsedDate.dayOfMonth,
            parsedStart.first, parsedStart.second, startLdt.second, startLdt.nanosecond
        ).toInstant(timeZone)
        val newEndInstant = LocalDateTime(
            endDate.year, endDate.monthNumber, endDate.dayOfMonth,
            parsedEnd.first, parsedEnd.second, endLdt.second, endLdt.nanosecond
        ).toInstant(timeZone)

        val newDuration = newEndInstant.epochSeconds - newStartInstant.epochSeconds
        if (newDuration <= 0) {
            resetTexts()
            return
        }

        val unchanged = nameText == record.projectName &&
            newStartInstant == record.timestamp &&
            newDuration == record.durationSeconds
        if (!unchanged) onUpdateRecord(nameText, newStartInstant, newDuration)
    }

    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InlineEditableInput(
                    value = nameText,
                    onValueChange = { nameText = it },
                    onCommit = ::commitChanges,
                    textStyle = TextStyle(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp,
                    ),
                    placeholder = "Bez projektu",
                    width = Modifier.fillMaxWidth(),
                )

                InlineEditableInput(
                    value = dateText,
                    onValueChange = { dateText = it },
                    onCommit = ::commitChanges,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Start,
                    ),
                    placeholder = "01.01.2026",
                    width = Modifier.width(100.dp),
                )
            }

            Spacer(Modifier.width(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CleanTimeInput(value = startText, onValueChange = { startText = it }, onCommit = ::commitChanges)

                Text("→", color = TextSecondary, fontSize = 18.sp)

                CleanTimeInput(value = endText, onValueChange = { endText = it }, onCommit = ::commitChanges)
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = TimeUtils.formatSeconds(record.durationSeconds),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp,
                    )
                )

                IconButton(onClick = onStartAgain, Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Spustit znova", Modifier.size(25.dp), tint = PrimaryEmerald)
                }

                IconButton(onClick = onCopyRecord, Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Vytvořit kopii", Modifier.size(15.dp), tint = TextSecondary)
                }

                IconButton(onClick = onDelete, Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Smazat", Modifier.size(16.dp), tint = StopRed.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable private fun InlineEditableInput(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    textStyle: TextStyle,
    placeholder: String = "",
    width: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val showHighlight = isHovered || isFocused

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(PrimaryEmerald),
        modifier = width
            .hoverable(interactionSource)
            .pointerHoverIcon(PointerIcon.Text)
            .clearFocusOnEnter(focusManager)
            .onFocusChanged {
                if (isFocused && !it.isFocused) onCommit()
                isFocused = it.isFocused
            },
        decorationBox = { innerTextField ->
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (showHighlight) SurfaceVariantDark else Color.Transparent)
                    .border(
                        1.dp,
                        if (isFocused) PrimaryEmerald else if (isHovered) BorderDark else Color.Transparent,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isBlank() && placeholder.isNotBlank()) {
                    Text(placeholder, style = textStyle.copy(color = TextSecondary))
                }
                innerTextField()
            }
        }
    )
}

@Composable private fun CleanTimeInput(value: String, onValueChange: (String) -> Unit, onCommit: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        modifier = Modifier
            .clearFocusOnEnter(focusManager)
            .onFocusChanged {
                if (isFocused && !it.isFocused) onCommit()
                isFocused = it.isFocused
            },
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(PrimaryEmerald),
        decorationBox = { innerTextField ->
            Box(
                Modifier
                    .width(64.dp)
                    .height(30.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariantDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { innerTextField() }
        }
    )
}

private fun Modifier.clearFocusOnEnter(focusManager: FocusManager): Modifier = onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
        focusManager.clearFocus()
        true
    } else false
}

@Composable private fun ExportCsvDialog(onDismiss: () -> Unit, onExport: (year: Int, monthNumber: Int) -> Unit) {
    val currentLdt = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var selectedYear by remember { mutableStateOf(currentLdt.year) }
    var selectedMonth by remember { mutableStateOf(currentLdt.monthNumber) }

    val months = listOf(
        1 to "Leden",
        2 to "Únor",
        3 to "Březen",
        4 to "Duben",
        5 to "Květen",
        6 to "Červen",
        7 to "Červenec",
        8 to "Srpen",
        9 to "Září",
        10 to "Říjen",
        11 to "Listopad",
        12 to "Prosinec"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.width(440.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Exportovat měsíční přehled",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary,
                    ),
                )

                Text(
                    text = "Vyberte měsíc a rok pro vygenerování souboru .csv s agregovanými časy podle projektů.",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = TextSecondary,
                    ),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Měsíc:", style = TextStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold))

                    val rows = months.chunked(4)
                    for (row in rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for ((mNum, mName) in row) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedMonth == mNum) PrimaryEmerald else SurfaceVariantDark)
                                        .clickable { selectedMonth = mNum }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = mName,
                                        style = TextStyle(
                                            color = if (selectedMonth == mNum) Color.White else TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedMonth == mNum) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Rok:", style = TextStyle(fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .clickable { selectedYear-- },
                            contentAlignment = Alignment.Center,
                        ) { Text("-", color = TextPrimary, fontWeight = FontWeight.Bold) }

                        Text(
                            text = selectedYear.toString(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary,
                            ),
                        )

                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceVariantDark)
                                .clickable { selectedYear++ },
                            contentAlignment = Alignment.Center,
                        ) { Text("+", color = TextPrimary, fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Zrušit") }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            onExport(selectedYear, selectedMonth)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryEmerald,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Exportovat CSV", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable private fun DeleteConfirmDialog(record: TimeRecord, onDismiss: () -> Unit, onConfirmDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.width(400.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StopRed.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            Modifier.size(20.dp),
                            tint = StopRed,
                        )
                    }

                    Text(
                        text = "Smazat záznam",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = TextPrimary,
                        ),
                    )
                }

                Text(
                    text = "Opravdu chcete smazat záznam \"${record.projectName.ifBlank { "Bez projektu" }}\"? Tato akce je nevratná.",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = TextSecondary,
                    ),
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariantDark,
                            contentColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Zrušit") }

                    Spacer(Modifier.width(12.dp))

                    Button(
                        onClick = {
                            onConfirmDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StopRed,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Smazat", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
