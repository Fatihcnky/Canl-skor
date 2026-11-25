package com.fatih.skor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Renkler ve şekiller android/compose-samples > Jetsnack temasından uyarlandı
val Bg = Color(0xFF121212)               // Neutral8
val Card = Color(0xFF1C1C1E)
val Brand = Color(0xFFDED6FE)            // Shadow1
val BrandSecondary = Color(0xFFBBFDFD)   // Ocean2
val Accent = BrandSecondary
val LiveRed = Color(0xFFF4568B)          // Rose4
val Dim = Color(0xBDFFFFFF)              // Neutral1
val BorderGradient = listOf(Color(0xFF86F7FA), Color(0xFF9B86FA)) // Ocean3 -> Shadow3
val LiveGradient = listOf(Color(0xFFC30060), Color(0xFF5500D7), Color(0xFF7F0054)) // Rose8, Lavender7, Rose11
val PillGradient = listOf(Color(0xFF57EFF5), Color(0xFFC8BBFD))   // Ocean4 -> Shadow2
val HairLine = listOf(Color(0x1FFFFFFF), Color(0x1FFFFFFF))        // Neutral3

fun Modifier.gradientBorder(colors: List<Color>, shape: Shape, width: Dp = 1.dp): Modifier =
    border(width, Brush.linearGradient(colors), shape)

/** Jetsnack'teki gibi: seçiliyken dolu, değilken gradyan çerçeveli hap biçimli filtre. */
@Composable
fun SkorChip(selected: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape)
            .then(if (selected) Modifier.background(BrandSecondary) else Modifier.gradientBorder(BorderGradient, CircleShape))
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.Black else Color.White, maxLines = 1)
    }
}

@Composable
fun SkorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Brand, background = Bg, surface = Card,
            onBackground = Color.White, onSurface = Color.White,
            surfaceVariant = Card, onSurfaceVariant = Dim,
        ),
        typography = SkorTypography,
        shapes = Shapes(small = CircleShape, medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(0.dp)),
        content = content,
    )
}

// Material 3 tip ölçeği (compose-samples'taki gibi 4dp ızgarasına oturan satır yükseklikleri)
private val SkorTypography = Typography(
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp),
)
