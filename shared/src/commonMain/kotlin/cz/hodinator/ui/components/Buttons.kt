package cz.hodinator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary


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

@Composable fun ToolbarButton(icon: ImageVector, label: String, onClick: () -> Unit, showLabel: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
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
