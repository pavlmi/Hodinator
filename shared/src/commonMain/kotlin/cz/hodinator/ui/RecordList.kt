package cz.hodinator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import cz.hodinator.models.NO_PROJECT_NAME
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.components.*
import cz.hodinator.ui.theme.*
import cz.hodinator.utils.TimeUtils
import kotlinx.datetime.*

@Composable fun RecordList(
    records: List<TimeRecord>,
    scrollToTopRequest: Int,
    onStartAgain: (TimeRecord) -> Unit,
    onCopy: (TimeRecord) -> Unit,
    onDelete: (TimeRecord) -> Unit,
    onUpdate: (TimeRecord) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopRequest) {
        if (scrollToTopRequest > 0) listState.animateScrollToItem(0)
    }

    if (records.isEmpty()) {
        Box(Modifier.fillMaxSize().cardBackground(SurfaceDark), contentAlignment = Alignment.Center) {
            Text("Žádné záznamy k zobrazení", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    val timeZone = remember { TimeZone.currentSystemDefault() }
    val recordsByDay = remember(records) { records.groupBy { it.startTime.toLocalDateTime(timeZone).date } }
    val today = remember(records) { Clock.System.now().toLocalDateTime(timeZone).date }

    LazyColumn(Modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        recordsByDay.forEach { (date, dayRecords) ->
            item(key = date.toString()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .cardBackground(DayGroupBackground)
                        .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DayHeader(date, today, dayTotalSeconds = dayRecords.sumOf { it.durationSeconds })

                    dayRecords.forEach { record ->
                        key(record.id) {
                            RecordCard(
                                record = record,
                                timeZone = timeZone,
                                onStartAgain = { onStartAgain(record) },
                                onCopy = { onCopy(record) },
                                onDelete = { onDelete(record) },
                                onUpdate = onUpdate,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun DayHeader(date: LocalDate, today: LocalDate, dayTotalSeconds: Long) {
    val title = when (date) {
        today -> "Dnes – ${TimeUtils.dayWithDate(date)}"
        today.minus(1, DateTimeUnit.DAY) -> "Včera – ${TimeUtils.dayWithDate(date)}"
        else -> TimeUtils.dayWithDate(date)
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(PrimaryEmerald))
            Text(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary.copy(alpha = 0.8f)),
            )
        }

        Text(
            "Denní celkem: ${TimeUtils.formatSeconds(dayTotalSeconds)}",
            softWrap = false,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
            ),
        )
    }
}

@Composable private fun RecordCard(
    record: TimeRecord,
    timeZone: TimeZone,
    onStartAgain: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (TimeRecord) -> Unit,
) {
    val start = remember(record) { record.startTime.toLocalDateTime(timeZone) }
    val end = remember(record) { record.endTime.toLocalDateTime(timeZone) }

    var nameText by remember(record) { mutableStateOf(record.projectName) }
    var dateText by remember(record) { mutableStateOf(TimeUtils.formatDate(start.date)) }
    var startText by remember(record) { mutableStateOf(TimeUtils.formatTime(start.time)) }
    var endText by remember(record) { mutableStateOf(TimeUtils.formatTime(end.time)) }

    fun resetTexts() {
        nameText = record.projectName
        dateText = TimeUtils.formatDate(start.date)
        startText = TimeUtils.formatTime(start.time)
        endText = TimeUtils.formatTime(end.time)
    }

    fun commitChanges() {
        val newDate = TimeUtils.parseDate(dateText)
        val newStartTime = TimeUtils.parseTime(startText)
        val newEndTime = TimeUtils.parseTime(endText)
        if (newDate == null || newStartTime == null || newEndTime == null) return resetTexts()

        // Changing the date shifts start and end equally (keeps records spanning midnight intact).
        val dayShift = newDate.toEpochDays() - start.date.toEpochDays()
        val newEndDate = end.date.plus(dayShift, DateTimeUnit.DAY)
        // Seconds aren't editable in the UI, so keep them from the original record.
        val newStart = LocalDateTime(newDate, LocalTime(newStartTime.hour, newStartTime.minute, start.second, start.nanosecond))
            .toInstant(timeZone)
        val newEnd = LocalDateTime(newEndDate, LocalTime(newEndTime.hour, newEndTime.minute, end.second, end.nanosecond))
            .toInstant(timeZone)

        val newDuration = (newEnd - newStart).inWholeSeconds
        if (newDuration <= 0) return resetTexts()

        val updated = record.copy(projectName = nameText, startTime = newStart, durationSeconds = newDuration)
        if (updated != record) onUpdate(updated)
    }

    Row(
        Modifier.fillMaxWidth().cardBackground(SurfaceDark, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            InlineEditableText(
                value = nameText,
                onValueChange = { nameText = it },
                onCommit = ::commitChanges,
                textStyle = TextStyle(fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth(),
                placeholder = NO_PROJECT_NAME,
            )
            InlineEditableText(
                value = dateText,
                onValueChange = { dateText = it },
                onCommit = ::commitChanges,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextSecondary, fontSize = 11.sp),
                modifier = Modifier.width(100.dp),
                placeholder = "01.01.2026",
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TimeInput(value = startText, onValueChange = { startText = it }, onCommit = ::commitChanges)
            Text("→", color = TextSecondary, fontSize = 18.sp)
            TimeInput(value = endText, onValueChange = { endText = it }, onCommit = ::commitChanges)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                TimeUtils.formatSeconds(record.durationSeconds),
                softWrap = false,
                style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp),
            )
            RecordAction(Icons.Rounded.PlayArrow, "Spustit znovu", PrimaryEmerald, iconSize = 25.dp, onClick = onStartAgain)
            RecordAction(Icons.Rounded.ContentCopy, "Vytvořit kopii", TextSecondary, iconSize = 15.dp, onClick = onCopy)
            RecordAction(Icons.Rounded.Delete, "Smazat", StopRed.copy(alpha = 0.8f), iconSize = 16.dp, onClick = onDelete)
        }
    }
}

@Composable private fun RecordAction(icon: ImageVector, description: String, tint: Color, iconSize: Dp, onClick: () -> Unit) {
    IconButton(onClick = onClick, Modifier.size(28.dp)) {
        Icon(icon, contentDescription = description, Modifier.size(iconSize), tint = tint)
    }
}
