package com.fatih.skor.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fatih.skor.data.Match
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val hm: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
val dayFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale("tr"))

@Composable
fun Logo(url: String?, size: Dp = 22.dp) {
    AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(size))
}

fun Match.timeText(): String = time?.atZone(ZoneId.systemDefault())?.format(hm) ?: "--:--"

/** Sol sütundaki durum metni: canlıysa dakika, bittiyse MS, değilse saat. */
fun Match.statusShort(): String = when {
    live -> if (statusText.contains("HT", true) || statusText.contains("Half", true)) "İY" else clock.ifBlank { statusText }
    finished -> "MS"
    else -> timeText()
}

val dayHeaderFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy EEEE", Locale("tr"))

fun dayHeader(d: java.time.LocalDate): String {
    val base = d.format(dayHeaderFmt)
    return if (d == java.time.LocalDate.now()) "$base · Bugün" else base
}
