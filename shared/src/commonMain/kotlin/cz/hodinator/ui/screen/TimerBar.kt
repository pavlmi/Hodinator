package cz.hodinator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import cz.hodinator.ui.components.*
import cz.hodinator.ui.theme.*
import cz.hodinator.utils.ProjectSuggestions
import cz.hodinator.utils.TimeUtils


@Composable fun TimerBar(
    projectName: String,
    projectNames: List<String>,
    elapsedSeconds: Long,
    isRunning: Boolean,
    onProjectNameChange: (String) -> Unit,
    onToggleTimer: () -> Unit,
) {
    val accent = if (isRunning) StopRed else PrimaryEmerald

    SectionCard {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProjectNameField(projectName, projectNames, accent, onProjectNameChange, Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (isRunning) Box(Modifier.size(8.dp).clip(CircleShape).background(StopRed))
                Text(
                    text = TimeUtils.formatSeconds(elapsedSeconds),
                    softWrap = false,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (isRunning) TextPrimary else TextSecondary,
                    ),
                )
            }

            Button(
                onClick = onToggleTimer,
                Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isRunning) "STOP" else "START", softWrap = false, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            }
        }
    }
}

@Composable private fun ProjectNameField(
    projectName: String,
    projectNames: List<String>,
    accent: Color,
    onProjectNameChange: (String) -> Unit,
    modifier: Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(projectName)) }
    val value = if (fieldValue.text == projectName) fieldValue else TextFieldValue(projectName, TextRange(projectName.length))
    var isFocused by remember { mutableStateOf(false) }
    var isDismissed by remember { mutableStateOf(false) }
    var fieldWidth by remember { mutableStateOf(0) }
    val suggestions = remember(projectName, projectNames) { ProjectSuggestions.suggest(projectNames, projectName) }
    var highlighted by remember(suggestions) { mutableStateOf(-1) }
    val showSuggestions = isFocused && !isDismissed && suggestions.isNotEmpty()

    fun pick(name: String) {
        fieldValue = TextFieldValue(name, TextRange(name.length))
        isDismissed = true
        onProjectNameChange(name)
    }

    Box(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                val text = it.text.withoutLineBreaks()
                if (text != projectName) isDismissed = false
                fieldValue = if (text == it.text) it else TextFieldValue(text, TextRange(text.length))
                onProjectNameChange(text)
            },
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { fieldWidth = it.size.width }
                .onFocusChanged { isFocused = it.isFocused }
                .onPreviewKeyEvent { event ->
                    if (!showSuggestions || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { highlighted = (highlighted + 1) % suggestions.size; true }
                        Key.DirectionUp -> { highlighted = if (highlighted <= 0) suggestions.lastIndex else highlighted - 1; true }
                        Key.Enter, Key.NumPadEnter -> if (highlighted >= 0) { pick(suggestions[highlighted]); true } else false
                        Key.Escape -> { isDismissed = true; true }
                        else -> false
                    }
                },
            placeholder = { Text("Na čem právě pracuješ?", color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = appTextFieldColors(),
            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent) },
        )

        DropdownMenu(
            expanded = showSuggestions,
            onDismissRequest = { isDismissed = true },
            Modifier.width(with(LocalDensity.current) { fieldWidth.toDp() }),
            properties = PopupProperties(focusable = false),
            containerColor = SurfaceVariantDark,
        ) {
            suggestions.forEachIndexed { index, name ->
                DropdownMenuItem(
                    text = { Text(name, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { pick(name) },
                    Modifier.background(if (index == highlighted) BorderDark else Color.Transparent),
                )
            }
        }
    }
}
