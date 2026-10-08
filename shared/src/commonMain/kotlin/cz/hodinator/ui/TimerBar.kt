package cz.hodinator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hodinator.ui.components.SectionCard
import cz.hodinator.ui.components.appTextFieldColors
import cz.hodinator.ui.theme.PrimaryEmerald
import cz.hodinator.ui.theme.StopRed
import cz.hodinator.ui.theme.TextPrimary
import cz.hodinator.ui.theme.TextSecondary
import cz.hodinator.utils.TimeUtils

@Composable fun TimerBar(
    projectName: String,
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
            OutlinedTextField(
                value = projectName,
                onValueChange = onProjectNameChange,
                placeholder = { Text("Na čem právě pracuješ?", color = TextSecondary, fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = appTextFieldColors(),
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent) },
            )

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

            Button(
                onClick = onToggleTimer,
                modifier = Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isRunning) "STOP" else "START", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp))
            }
        }
    }
}
