package com.fatih.skor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0F1419)
val Card = Color(0xFF1A212B)
val Accent = Color(0xFF2E7DFF)
val LiveRed = Color(0xFFFF4D4D)
val Dim = Color(0xFF8A96A6)

@Composable
fun SkorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, background = Bg, surface = Card,
            onBackground = Color.White, onSurface = Color.White,
            surfaceVariant = Card, onSurfaceVariant = Dim,
        ),
        content = content,
    )
}
