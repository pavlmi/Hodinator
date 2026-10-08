package cz.pavlik.timetracker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val BackgroundDark = Color(0xFF0F1115)
val SurfaceDark = Color(0xFF171A21)
val DayGroupBackground = Color(0xFF13161C)
val SurfaceVariantDark = Color(0xFF20242F)
val TooltipBackground = Color(0xFF0B0D11)
val BorderDark = Color(0xFF2D323E)

val PrimaryEmerald = Color(0xFF10B981)
val StopRed = Color(0xFFEF4444)

val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)

val AppColorScheme = darkColorScheme(
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    primary = PrimaryEmerald,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
)
