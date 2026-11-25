package com.fatih.skor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.Detail
import com.fatih.skor.data.Espn
import com.fatih.skor.data.Lineup
import com.fatih.skor.data.Match
import com.fatih.skor.data.StandRow
import com.fatih.skor.data.Team
import kotlinx.coroutines.delay
import java.time.ZoneId

private val Green = Color(0xFF2EB85C)
private val Red = Color(0xFFE5534B)
private val Grey = Color(0xFF8D93A0)

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

    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF23009E), Bg), endY = 1000f))) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") }
            Text(m.league.name, color = Dim, style = MaterialTheme.typography.bodyMedium)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TeamBox(m.home, favs.containsKey(m.home.id), Modifier.weight(1f)) { vm.toggleFav(m.home.id, m.home.name) }
            Column(Modifier.width(110.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (m.homeScore != null) {
                    Text("${m.homeScore} - ${m.awayScore ?: 0}", style = MaterialTheme.typography.headlineLarge, color = if (m.live) LiveRed else Color.White)
                    Text(if (m.live) m.clock.ifBlank { m.statusText } else if (m.finished) "Maç sonu" else m.statusText, color = if (m.live) LiveRed else Dim, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(m.timeText(), style = MaterialTheme.typography.headlineMedium)
                    Text(m.time?.atZone(ZoneId.systemDefault())?.format(dayFmt) ?: "", color = Dim, style = MaterialTheme.typography.bodyMedium)
                }
            }
            TeamBox(m.away, favs.containsKey(m.away.id), Modifier.weight(1f)) { vm.toggleFav(m.away.id, m.away.name) }
        }
        val tabs = listOf("Ayrıntılar", "Olaylar", "İstatistik", "Kadro", "Tablo")
        ScrollableTabRow(selectedTabIndex = tab, containerColor = Color.Transparent, contentColor = Brand, edgePadding = 8.dp) {
            tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t, maxLines = 1) }) }
        }
        Box(Modifier.fillMaxSize()) {
            val det = d
            if (!loaded) CircularProgressIndicator(Modifier.align(Alignment.Center))
            else if (det == null) Text("Maç detayı alınamadı.\n\nHata: " + Espn.lastError, color = Dim, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(24.dp))
            else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (tab) {
                    0 -> InfoTab(m, det)
                    1 -> EventsTab(m, det)
                    2 -> StatsTab(det)
                    3 -> LineupTab(m, det)
                    else -> TableTab(m)
                }
            }
        }
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Card)
            .gradientBorder(HairLine, RoundedCornerShape(20.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

// ---------------------------------------------------------------- Ayrıntılar

@Composable
private fun InfoTab(m: Match, det: Detail) {
    val i = det.info ?: com.fatih.skor.data.Info(null, null, null, null, emptyList())
    Panel {
        InfoRow("🏆", m.league.name)
        InfoRow("🗓", (m.time?.atZone(ZoneId.systemDefault())?.format(java.time.format.DateTimeFormatter.ofPattern("d.MM.yyyy · HH:mm")) ?: "-"))
        i.venue?.let { InfoRow("🏟", listOfNotNull(it, i.city).joinToString(", ")) }
        i.referee?.let { InfoRow("🟨", "Hakem: $it") }
        i.attendance?.let { InfoRow("👥", "Seyirci: $it") }
        if (i.broadcasts.isNotEmpty()) InfoRow("📺", i.broadcasts.joinToString(", "))
    }
    if (det.formHome.isNotEmpty() || det.formAway.isNotEmpty()) {
        Panel {
            Text("Takım formu", style = MaterialTheme.typography.titleMedium, color = Brand, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(m.home to det.formHome, m.away to det.formAway).forEach { (t, games) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(t.short, style = MaterialTheme.typography.labelMedium, color = Dim, maxLines = 1)
                        games.forEach { g ->
                            val c = when (g.result.uppercase()) { "W" -> Green; "L" -> Red; else -> Grey }
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.copy(alpha = 0.25f)).padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(g.score, fontWeight = FontWeight.Bold, color = c, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Logo(g.oppLogo, 18.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, modifier = Modifier.width(32.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

// ---------------------------------------------------------------- Olaylar

@Composable
private fun EventsTab(m: Match, det: Detail) {
    if (det.events.isEmpty()) { Text("Henüz olay yok.", color = Dim); return }
    // Skor akışı: olaylar kronolojik gelir; gol olaylarında o anki skor hesaplanır
    var h = 0; var a = 0
    val withScore = det.events.map { e ->
        if (e.kind == "goal") { if (e.teamId == m.home.id) h++ else a++ ; e to "$h - $a" } else e to null
    }
    Panel {
        withScore.reversed().forEach { (e, score) ->
            val home = e.teamId == m.home.id
            val icon = when (e.kind) { "goal" -> "⚽"; "yellow" -> "🟨"; "red" -> "🟥"; "sub" -> "🔁"; else -> "📺" }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (home) Arrangement.Start else Arrangement.End) {
                if (home) {
                    Text(e.minute, color = Dim, modifier = Modifier.width(48.dp), style = MaterialTheme.typography.labelMedium)
                    Text("$icon  ${e.text}", style = MaterialTheme.typography.bodyMedium)
                    score?.let { ScoreChip(it) }
                } else {
                    score?.let { ScoreChip(it) }
                    Text("${e.text}  $icon", style = MaterialTheme.typography.bodyMedium)
                    Text(e.minute, color = Dim, modifier = Modifier.width(48.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun ScoreChip(s: String) {
    Text(
        s, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(horizontal = 8.dp).clip(CircleShape).gradientBorder(BorderGradient, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

// ---------------------------------------------------------------- İstatistik

@Composable
private fun StatsTab(det: Detail) {
    if (det.stats.isEmpty()) { Text("İstatistik henüz yok.", color = Dim); return }
    Panel {
        det.stats.forEach { s ->
            val hv = s.home.trim('%').toFloatOrNull() ?: 0f
            val av = s.away.trim('%').toFloatOrNull() ?: 0f
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.home, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp))
                    Text(s.label, Modifier.weight(1f), color = Dim, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                    Text(s.away, fontWeight = FontWeight.Bold, modifier = Modifier.width(56.dp), textAlign = TextAlign.End)
                }
                Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.weight(maxOf(hv, 0.01f)).fillMaxSize().clip(CircleShape).background(if (hv >= av) BrandSecondary else Grey))
                    Box(Modifier.weight(maxOf(av, 0.01f)).fillMaxSize().clip(CircleShape).background(if (av >= hv) LiveRed else Grey))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Kadro

private fun formationRows(formation: String?, starters: Int): List<Int>? {
    val parts = formation?.split("-")?.mapNotNull { it.trim().toIntOrNull() } ?: return null
    if (parts.isEmpty() || parts.sum() != starters - 1) return null
    return listOf(1) + parts
}

@Composable
private fun LineupTab(m: Match, det: Detail) {
    if (det.lineups.isEmpty()) { Text("Kadro henüz açıklanmadı.", color = Dim); return }
    var sel by remember { mutableStateOf(0) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        det.lineups.forEachIndexed { i, l -> SkorChip(selected = sel == i, label = l.team.short, onClick = { sel = i }) }
    }
    val l = det.lineups[sel.coerceIn(0, det.lineups.lastIndex)]
    val rows = formationRows(l.formation, l.starters.size)
    Text((l.formation ?: "Diziliş yok"), color = Dim, style = MaterialTheme.typography.labelMedium)
    if (rows != null) Pitch(l, rows) else Panel {
        l.starters.forEach { p -> Text("${p.jersey.padStart(2)}  ${p.name}  ${p.pos}", style = MaterialTheme.typography.bodyMedium) }
    }
    if (l.bench.isNotEmpty()) Panel {
        Text("Yedekler", style = MaterialTheme.typography.titleMedium, color = Brand)
        l.bench.forEach { p -> Text("${p.jersey.padStart(2)}  ${p.name}", color = Dim, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Pitch(l: Lineup, rows: List<Int>) {
    val players = l.starters.sortedBy { if (it.place > 0) it.place else Int.MAX_VALUE }
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(0.75f).clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1B5E20), Color(0xFF2E7D32)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val line = Color.White.copy(alpha = 0.35f); val st = Stroke(width = 3f)
            drawRect(line, Offset(12f, 12f), Size(size.width - 24f, size.height - 24f), style = st)
            drawLine(line, Offset(12f, size.height / 2), Offset(size.width - 12f, size.height / 2), strokeWidth = 3f)
            drawCircle(line, size.width * 0.13f, Offset(size.width / 2, size.height / 2), style = st)
            val bw = size.width * 0.5f; val bh = size.height * 0.14f
            drawRect(line, Offset((size.width - bw) / 2, 12f), Size(bw, bh), style = st)
            drawRect(line, Offset((size.width - bw) / 2, size.height - 12f - bh), Size(bw, bh), style = st)
        }
        var idx = 0
        rows.forEachIndexed { r, n ->
            val yFrac = if (rows.size == 1) 0.5f else 0.90f - 0.80f * (r.toFloat() / (rows.size - 1))
            for (k in 0 until n) {
                val p = players.getOrNull(idx++) ?: continue
                val xFrac = (k + 1f) / (n + 1f)
                Column(
                    Modifier.offset(x = maxWidth * xFrac - 30.dp, y = maxHeight * yFrac - 24.dp).width(60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                        Text(p.jersey, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(p.name.substringAfterLast(' '), color = Color.White, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Tablo

@Composable
private fun TableTab(m: Match) {
    var rows by remember { mutableStateOf<List<StandRow>?>(null) }
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(m.league.slug) {
        val g = Espn.standings(m.league)
        rows = g?.firstOrNull { gr -> gr.rows.any { it.team.id == m.home.id || it.team.id == m.away.id } }?.rows ?: g?.firstOrNull()?.rows
        done = true
    }
    if (!done) { CircularProgressIndicator(); return }
    val list = rows
    if (list.isNullOrEmpty()) { Text("Bu müsabaka için puan durumu yok.", color = Dim); return }
    Panel {
        list.forEach { r ->
            val mine = r.team.id == m.home.id || r.team.id == m.away.id
            Row(
                Modifier.fillMaxWidth().then(if (mine) Modifier.clip(RoundedCornerShape(10.dp)).background(Brand.copy(alpha = 0.18f)) else Modifier).padding(vertical = 5.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${r.rank}", Modifier.width(28.dp), color = Dim, style = MaterialTheme.typography.bodyMedium)
                Logo(r.team.logo, 20.dp)
                Text(r.team.name, Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                Text("${r.played}", Modifier.width(28.dp), textAlign = TextAlign.Center, color = Dim, style = MaterialTheme.typography.bodyMedium)
                Text("${r.points}", Modifier.width(32.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TeamBox(t: Team, fav: Boolean, modifier: Modifier, onFav: () -> Unit) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Logo(t.logo, 56.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(t.name, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f, fill = false))
            IconButton(onClick = onFav) { Icon(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, "Favori", tint = if (fav) Color(0xFFFFC107) else Dim) }
        }
    }
}
