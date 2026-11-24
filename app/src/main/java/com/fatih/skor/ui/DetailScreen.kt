package com.fatih.skor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.Detail
import com.fatih.skor.data.Match
import com.fatih.skor.data.Team
import kotlinx.coroutines.delay
import java.time.ZoneId

@Composable
fun DetailScreen(vm: ScoreVm, base: Match, onBack: () -> Unit) {
    var d by remember { mutableStateOf<Detail?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }
    val favs by vm.favs.collectAsStateWithLifecycle()
    val m = d?.match ?: base

    // Canlı maçta 30 sn'de bir yenile
    LaunchedEffect(base.id) {
        while (true) {
            d = vm.detail(base) ?: d
            loaded = true
            if (!(d?.match ?: base).live) break
            delay(30_000)
        }
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Text(m.league.name, color = Dim, fontSize = 14.sp)
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamBox(m.home, favs.containsKey(m.home.id), Modifier.weight(1f)) { vm.toggleFav(m.home.id, m.home.name) }
            Column(Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (m.homeScore != null) {
                    Text("${m.homeScore} - ${m.awayScore ?: 0}", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = if (m.live) LiveRed else Color.White)
                    Text(if (m.live) m.clock.ifBlank { m.statusText } else if (m.finished) "Maç sonu" else m.statusText, color = if (m.live) LiveRed else Dim, fontSize = 13.sp)
                } else {
                    Text(m.timeText(), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(m.time?.atZone(ZoneId.systemDefault())?.format(dayFmt) ?: "", color = Dim, fontSize = 13.sp)
                }
            }
            TeamBox(m.away, favs.containsKey(m.away.id), Modifier.weight(1f)) { vm.toggleFav(m.away.id, m.away.name) }
        }
        m.venue?.let { Text(it, color = Dim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
        TabRow(selectedTabIndex = tab, containerColor = Bg) {
            listOf("Olaylar", "İstatistik", "Kadro").forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
        }
        Box(Modifier.fillMaxSize()) {
            val det = d
            if (!loaded) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else if (det == null) Text("Maç detayı alınamadı.", color = Dim, modifier = Modifier.align(Alignment.Center))
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (tab) {
                    0 -> if (det.events.isEmpty()) Text("Henüz olay yok.", color = Dim) else det.events.forEach { e ->
                        val home = e.teamId == m.home.id
                        val icon = when (e.kind) { "goal" -> "⚽"; "yellow" -> "🟨"; "red" -> "🟥"; "sub" -> "🔁"; else -> "📺" }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (home) Arrangement.Start else Arrangement.End) {
                            if (home) { Text(e.minute, color = Dim, modifier = Modifier.width(44.dp), fontSize = 13.sp); Text("$icon  ${e.text}") }
                            else { Text("${e.text}  $icon"); Text(e.minute, color = Dim, modifier = Modifier.width(44.dp), textAlign = TextAlign.End, fontSize = 13.sp) }
                        }
                    }
                    1 -> if (det.stats.isEmpty()) Text("İstatistik henüz yok.", color = Dim) else det.stats.forEach { s ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(s.home, Modifier.width(60.dp), fontWeight = FontWeight.Bold)
                            Text(s.label, Modifier.weight(1f), color = Dim, textAlign = TextAlign.Center, fontSize = 13.sp)
                            Text(s.away, Modifier.width(60.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }
                    }
                    else -> if (det.lineups.isEmpty()) Text("Kadro henüz açıklanmadı.", color = Dim) else det.lineups.forEach { l ->
                        Text(l.team.name + (l.formation?.let { " ($it)" } ?: ""), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                        l.starters.forEach { p -> Text("${p.jersey.padStart(2)}  ${p.name}  ${p.pos}", fontSize = 14.sp) }
                        if (l.bench.isNotEmpty()) {
                            Text("Yedekler", color = Dim, fontSize = 12.sp)
                            l.bench.forEach { p -> Text("${p.jersey.padStart(2)}  ${p.name}", color = Dim, fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamBox(t: Team, fav: Boolean, modifier: Modifier, onFav: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Logo(t.logo, 56.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.name, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, fontSize = 14.sp, modifier = Modifier.weight(1f, fill = false))
            IconButton(onClick = onFav) { Icon(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, "Favori", tint = if (fav) Color(0xFFFFC107) else Dim) }
        }
    }
}
