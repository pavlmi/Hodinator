package cz.hodinator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary


/** Single-line text fields don't strip line breaks from pasted text, so replace them with spaces. */
fun String.withoutLineBreaks(): String = replace(Regex("[\r\n]+"), " ")

@Composable fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryEmerald,
    unfocusedBorderColor = BorderDark,
    focusedContainerColor = SurfaceVariantDark,
    unfocusedContainerColor = SurfaceVariantDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)

/** Text that looks like a plain label but becomes editable on hover / click. */
@Composable fun InlineEditableText(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val shape = RoundedCornerShape(6.dp)

    CommitOnBlurTextField(
        value = value,
        onValueChange = onValueChange,
        onCommit = onCommit,
        textStyle = textStyle,
        modifier = modifier.hoverable(interactionSource).pointerHoverIcon(PointerIcon.Text),
    ) { isFocused, innerTextField ->
        Box(
            Modifier
                .clip(shape)
                .background(if (isHovered || isFocused) SurfaceVariantDark else Color.Transparent)
                .border(
                    1.dp,
                    when {
                        isFocused -> PrimaryEmerald
                        isHovered -> BorderDark
                        else -> Color.Transparent
                    },
                    shape,
                )
                .padding(horizontal = 6.dp, vertical = 3.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isBlank() && placeholder.isNotBlank()) {
                Text(placeholder, style = textStyle.copy(color = TextSecondary))
            }
            innerTextField()
        }
    }
}

@Composable fun TimeInput(value: String, onValueChange: (String) -> Unit, onCommit: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    CommitOnBlurTextField(
        value = value,
        onValueChange = onValueChange,
        onCommit = onCommit,
        textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
        ),
    ) { isFocused, innerTextField ->
        Box(
            Modifier
                .width(64.dp)
                .height(30.dp)
                .clip(shape)
                .background(SurfaceVariantDark)
                .border(1.dp, if (isFocused) PrimaryEmerald else BorderDark, shape),
            contentAlignment = Alignment.Center,
        ) { innerTextField() }
    }
}

/** Single-line field that commits on focus loss. Enter clears focus, so it commits as well. */
@Composable private fun CommitOnBlurTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
    decorationBox: @Composable (isFocused: Boolean, innerTextField: @Composable () -> Unit) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.withoutLineBreaks()) },
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(PrimaryEmerald),
        modifier = modifier
            .onPreviewKeyEvent { event ->
                val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (event.type == KeyEventType.KeyDown && isEnter) {
                    focusManager.clearFocus()
                    true
                } else false
            }
            .onFocusChanged {
                if (isFocused && !it.isFocused) onCommit()
                isFocused = it.isFocused
            },
        decorationBox = { innerTextField -> decorationBox(isFocused, innerTextField) },
    )
}
