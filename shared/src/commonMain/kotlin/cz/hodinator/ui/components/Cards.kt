package cz.hodinator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import cz.hodinator.ui.theme.BorderDark
import cz.hodinator.ui.theme.SurfaceDark


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
