package cz.hodinator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary
import cz.hodinator.utils.ProjectSuggestions


@Composable fun ProjectNameField(
    projectName: String,
    projectNames: List<String>,
    onProjectNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
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
            placeholder = placeholder?.let { { Text(it, color = TextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = appTextFieldColors(),
            leadingIcon = leadingIcon,
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
