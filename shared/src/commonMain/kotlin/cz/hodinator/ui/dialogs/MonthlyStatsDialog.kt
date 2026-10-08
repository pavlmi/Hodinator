package cz.hodinator.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cz.hodinator.models.TimeRecord
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.SurfaceDark
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary
import cz.hodinator.ui.theme.TooltipBackground
import cz.hodinator.utils.TimeUtils
import kotlin.math.ceil
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


private val ChartHeight = 220.dp
private val YAxisWidth = 36.dp
private val TooltipWidth = 180.dp
private val TooltipHeight = 46.dp

@Composable fun MonthlyStatsDialog(records: List<TimeRecord>, onDismiss: () -> Unit) {
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.now().toLocalDateTime(timeZone).date }
    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(today.monthNumber) }
    var showTable by remember { mutableStateOf(false) }

    val dailyTotals = remember(records, year, month) {
        TimeUtils.dailyTotalsForMonth(records, year, month, timeZone)
    }
    val totalSeconds = dailyTotals.sumOf { it.second }
    val workedDays = dailyTotals.count { it.second > 0 }
    val longestDay = dailyTotals.maxByOrNull { it.second }?.takeIf { it.second > 0 }

    fun shiftMonth(delta: Int) {
        val index = year * 12 + (month - 1) + delta
        year = index / 12
        month = index % 12 + 1
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth(0.95f).widthIn(max = 980.dp),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Měsíční přehled",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary),
                    )
                    Spacer(Modifier.weight(1f))

                    IconButton(onClick = { shiftMonth(-1) }, Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Předchozí měsíc", tint = TextPrimary)
                    }
                    Text(
                        text = "${TimeUtils.monthName(month)} $year",
                        modifier = Modifier.width(140.dp),
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary),
                    )
                    IconButton(onClick = { shiftMonth(1) }, Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Další měsíc", tint = TextPrimary)
                    }

                    Spacer(Modifier.width(12.dp))
                    IconButton(onClick = onDismiss, Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Zavřít", tint = TextSecondary)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("Odpracováno celkem", TimeUtils.formatHoursMinutes(totalSeconds), null, Modifier.weight(1f))
                    StatTile("Odpracované dny", workedDays.toString(), "z ${dailyTotals.size} dnů v měsíci", Modifier.weight(1f))
                    StatTile(
                        "Průměr na odpracovaný den",
                        if (workedDays > 0) TimeUtils.formatHoursMinutes(totalSeconds / workedDays) else "–",
                        null,
                        Modifier.weight(1f),
                    )
                    StatTile(
                        "Nejdelší den",
                        longestDay?.let { TimeUtils.formatHoursMinutes(it.second) } ?: "–",
                        longestDay?.let { TimeUtils.dayWithDate(it.first) },
                        Modifier.weight(1f),
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Odpracované hodiny po dnech",
                        style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary),
                    )
                    Spacer(Modifier.weight(1f))
                    ViewToggle("Graf", selected = !showTable) { showTable = false }
                    Spacer(Modifier.width(6.dp))
                    ViewToggle("Tabulka", selected = showTable) { showTable = true }
                }

                if (showTable) DailyTable(dailyTotals, today) else DailyBarChart(dailyTotals, today)
            }
        }
    }
}

@Composable private fun StatTile(label: String, value: String, caption: String?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceVariantDark)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            label.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            value,
            softWrap = false,
            style = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary),
        )
        Text(caption ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = TextSecondary, fontSize = 11.sp))
    }
}

@Composable private fun ViewToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) SurfaceVariantDark else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = TextStyle(
                color = if (selected) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

/** Rounds the Y axis maximum up to whole steps (in hours) so the axis has at most ~5 grid lines. */
private fun niceAxis(maxSeconds: Long): Pair<Int, Int> {
    val maxHours = ceil(maxSeconds / 3600.0).toInt().coerceAtLeast(1)
    val step = listOf(1, 2, 4, 6, 8, 12).first { maxHours <= it * 5 }
    val axisMax = ((maxHours + step - 1) / step) * step
    return axisMax to step
}

@Composable private fun DailyBarChart(dailyTotals: List<Pair<LocalDate, Long>>, today: LocalDate) {
    val (axisMaxHours, stepHours) = remember(dailyTotals) { niceAxis(dailyTotals.maxOfOrNull { it.second } ?: 0L) }
    val axisMaxSeconds = axisMaxHours * 3600f
    var hoveredIndex by remember(dailyTotals) { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(ChartHeight)) {
            Box(Modifier.width(YAxisWidth).fillMaxHeight()) {
                for (h in 0..axisMaxHours step stepHours) {
                    val fraction = h / axisMaxHours.toFloat()
                    Text(
                        text = "$h h",
                        Modifier
                            .align(Alignment.BottomStart)
                            .offset(y = ChartHeight * -fraction + 6.dp),
                        style = TextStyle(color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace),
                    )
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                val columnWidth = maxWidth / dailyTotals.size

                Canvas(Modifier.fillMaxSize()) {
                    for (h in 0..axisMaxHours step stepHours) {
                        val y = size.height * (1f - h / axisMaxHours.toFloat())
                        drawLine(
                            color = if (h == 0) BorderDark else BorderDark.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f,
                        )
                    }
                }

                Row(Modifier.fillMaxSize()) {
                    dailyTotals.forEachIndexed { index, (_, seconds) ->
                        val interactionSource = remember { MutableInteractionSource() }
                        val isHovered by interactionSource.collectIsHoveredAsState()
                        LaunchedEffect(isHovered) {
                            if (isHovered) hoveredIndex = index
                            else if (hoveredIndex == index) hoveredIndex = null
                        }

                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .hoverable(interactionSource)
                                .background(if (isHovered) SurfaceVariantDark.copy(alpha = 0.6f) else Color.Transparent),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            val fraction = (seconds / axisMaxSeconds).coerceIn(0f, 1f)
                            if (fraction > 0f) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.6f)
                                        .widthIn(max = 18.dp)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(if (isHovered) PrimaryEmerald else PrimaryEmerald.copy(alpha = 0.85f)),
                                )
                            }
                        }
                    }
                }

                hoveredIndex?.let { index ->
                    val (date, seconds) = dailyTotals[index]
                    val barTop = ChartHeight * (1f - (seconds / axisMaxSeconds).coerceIn(0f, 1f))
                    val x = (columnWidth * (index + 0.5f) - TooltipWidth / 2).coerceIn(0.dp, maxWidth - TooltipWidth)
                    val y = (barTop - TooltipHeight - 6.dp).coerceAtLeast(0.dp)

                    Column(
                        Modifier
                            .offset(x = x, y = y)
                            .width(TooltipWidth)
                            .height(TooltipHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TooltipBackground)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            TimeUtils.dayWithDate(date),
                            style = TextStyle(color = TextSecondary, fontSize = 11.sp),
                        )
                        Text(
                            if (seconds > 0) TimeUtils.formatHoursMinutes(seconds) else "Bez záznamu",
                            style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(start = YAxisWidth, top = 6.dp)) {
            dailyTotals.forEachIndexed { index, (date, _) ->
                val isToday = date == today
                val color = when {
                    isToday -> PrimaryEmerald
                    hoveredIndex == index -> TextPrimary
                    TimeUtils.isWeekend(date) -> TextSecondary.copy(alpha = 0.55f)
                    else -> TextSecondary
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        date.dayOfMonth.toString(),
                        style = TextStyle(color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal),
                    )
                    Text(
                        TimeUtils.dayOfWeekShortName(date),
                        style = TextStyle(color = color, fontSize = 9.sp),
                    )
                }
            }
        }
    }
}

@Composable private fun DailyTable(dailyTotals: List<Pair<LocalDate, Long>>, today: LocalDate) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = ChartHeight + 30.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        dailyTotals.forEach { (date, seconds) ->
            val weekend = TimeUtils.isWeekend(date)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    TimeUtils.formatDate(date),
                    modifier = Modifier.width(96.dp),
                    style = TextStyle(
                        color = if (date == today) PrimaryEmerald else TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                )
                Text(
                    TimeUtils.dayOfWeekName(date),
                    modifier = Modifier.weight(1f),
                    style = TextStyle(
                        color = if (weekend) TextSecondary else TextPrimary,
                        fontSize = 12.sp,
                    ),
                )
                Text(
                    if (seconds > 0) TimeUtils.formatHoursMinutes(seconds) else "–",
                    style = TextStyle(
                        color = if (seconds > 0) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (seconds > 0) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
