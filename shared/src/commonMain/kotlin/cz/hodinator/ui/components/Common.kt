package cz.hodinator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.SurfaceDark
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary

/** Background with the border used by all "cards" in the app. */
fun Modifier.cardBackground(color: Color, shape: Shape = RoundedCornerShape(14.dp)): Modifier =
    clip(shape).background(color).border(1.dp, BorderDark, shape)

@Composable fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, BorderDark),
        content = content,
    )
}

@Composable fun SelectablePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
) {
    Box(
        modifier
            .clip(shape)
            .background(if (selected) PrimaryEmerald else SurfaceVariantDark)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            softWrap = false,
            style = TextStyle(
                color = if (selected) Color.White else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

/** With [showLabel] = false only the icon is shown (used when the window is narrow). */
@Composable fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit, showLabel: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        // An explicit min width overrides Material's 58dp default, so the icon-only variant stays compact.
        modifier = Modifier.widthIn(min = 36.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderDark),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
        contentPadding = PaddingValues(horizontal = if (showLabel) 12.dp else 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, contentDescription = if (showLabel) null else label, Modifier.size(16.dp), tint = PrimaryEmerald)
        if (showLabel) {
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, softWrap = false)
        }
    }
}

@Composable fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryEmerald,
    unfocusedBorderColor = BorderDark,
    focusedContainerColor = SurfaceVariantDark,
    unfocusedContainerColor = SurfaceVariantDark,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)

/** Common dialog look: a fixed-width card with content laid out in a column. */
@Composable fun AppDialog(onDismiss: () -> Unit, width: Dp, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            Modifier.width(width),
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, BorderDark),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
        }
    }
}

@Composable fun DialogTitle(text: String) {
    Text(text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary))
}

@Composable fun DialogText(text: String) {
    Text(text, style = TextStyle(fontSize = 13.sp, color = TextSecondary))
}

/** "Zrušit" + confirm buttons aligned to the end. The dialog is dismissed after confirming. */
@Composable fun DialogButtons(
    confirmLabel: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmIcon: ImageVector? = null,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark, contentColor = TextPrimary),
            shape = RoundedCornerShape(8.dp),
        ) { Text("Zrušit") }

        Button(
            onClick = {
                onConfirm()
                onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = confirmColor, contentColor = Color.White),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (confirmIcon != null) {
                Icon(confirmIcon, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(confirmLabel, fontWeight = FontWeight.Bold)
        }
    }
}
