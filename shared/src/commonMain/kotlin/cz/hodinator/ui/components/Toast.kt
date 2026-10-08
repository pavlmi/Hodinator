package cz.hodinator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.StopRed
import cz.hodinator.ui.theme.SurfaceDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary


@Composable fun ToastNotification(message: String, isError: Boolean, onDismiss: () -> Unit) {
    val accent = if (isError) StopRed else PrimaryEmerald
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, accent),
        shadowElevation = 8.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isError) Icons.Rounded.ErrorOutline else Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = accent,
            )
            Text(
                text = message,
                style = TextStyle(color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDismiss, Modifier.size(24.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Zavřít", Modifier.size(14.dp), tint = TextSecondary)
            }
        }
    }
}
