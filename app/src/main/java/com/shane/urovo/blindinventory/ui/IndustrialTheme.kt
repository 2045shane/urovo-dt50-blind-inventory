package com.shane.urovo.blindinventory.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF0B0E11)
val Panel = Color(0xFF171C21)
val PanelRaised = Color(0xFF222A31)
val Line = Color(0xFF3A4650)
val Amber = Color(0xFFFFC107)
val Paper = Color(0xFFF6F3EC)
val Muted = Color(0xFFC5CED6)
val StartLime = Color(0xFFD6FF3F)
val StopRed = Color(0xFFFF3B30)
val OkGreen = Color(0xFFB6FF5A)

@Composable
fun IndustrialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Amber,
            onPrimary = Color.Black,
            secondary = StartLime,
            onSecondary = Color.Black,
            background = Ink,
            onBackground = Paper,
            surface = Panel,
            onSurface = Paper,
            error = StopRed,
            onError = Color.White,
        ),
        typography = Typography(
            bodyLarge = TextStyle(
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium,
                color = Paper,
            ),
        ),
        content = content,
    )
}
