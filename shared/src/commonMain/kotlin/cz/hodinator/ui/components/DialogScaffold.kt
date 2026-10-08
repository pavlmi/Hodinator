package cz.hodinator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.SurfaceDark
import cz.hodinator.ui.theme.SurfaceVariantDark
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary


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

@Composable fun DialogTitle(text: String) = Text(text, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary))

@Composable fun DialogText(text: String) = Text(text, style = TextStyle(fontSize = 13.sp, color = TextSecondary))

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
