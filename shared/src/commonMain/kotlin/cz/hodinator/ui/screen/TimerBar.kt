package cz.hodinator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
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
import cz.hodinator.ui.components.*
import cz.hodinator.ui.theme.*
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
            ProjectNameField(
                projectName,
                projectNames,
                onProjectNameChange,
                Modifier.weight(1f),
                placeholder = "Na čem právě pracuješ?",
                leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent) },
            )

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
