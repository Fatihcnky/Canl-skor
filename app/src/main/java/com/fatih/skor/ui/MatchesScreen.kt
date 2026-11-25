package com.fatih.skor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.LEAGUES
import com.fatih.skor.data.League
import com.fatih.skor.data.Match
import java.time.LocalDate

private val StarOn = Color(0xFFFFC107)

private sealed interface Row2 {
    data class Day(val d: LocalDate) : Row2
    data class Lg(val d: LocalDate, val l: League, val live: Int, val total: Int) : Row2
    data class M(val m: Match) : Row2
}

@Composable
fun MatchesScreen(vm: ScoreVm, modifier: Modifier, ask: () -> Unit, onOpen: (Match) -> Unit) {
    val all by vm.matches.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<String?>(null) }
    var liveOnly by remember { mutableStateOf(false) }
    val collapsed = remember { mutableStateMapOf<String, Boolean>() }

    val shown = all.filter { (filter == null || it.league.slug == filter) && (!liveOnly || it.live) }
    val today = LocalDate.now()
    val rows = remember(shown, collapsed.toMap()) {
        val out = mutableListOf<Row2>()
        shown.groupBy { it.localDate() ?: today }.toSortedMap().forEach { (d, dayList) ->
            out += Row2.Day(d)
            dayList.groupBy { it.league }.toSortedMap(compareBy { l -> LEAGUES.indexOf(l) }).forEach { (l, ms) ->
                out += Row2.Lg(d, l, ms.count { it.live }, ms.size)
                if (collapsed["$d|${l.slug}"] != true) ms.sortedBy { it.time }.forEach { out += Row2.M(it) }
            }
        }
        out
    }
    val listState = rememberLazyListState()
    var scrolled by remember { mutableStateOf(false) }
    LaunchedEffect(rows.size, loading) {
        if (!scrolled && rows.isNotEmpty()) {
            val i = rows.indexOfFirst { it is Row2.Day && !it.d.isBefore(today) }
            if (i > 0) listState.scrollToItem(i)
            scrolled = true
        }
    }

    Column(modifier.fillMaxSize().background(Bg)) {
        Text("Skor", color = Brand, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { SkorChip(selected = liveOnly, label = "● Canlı", onClick = { liveOnly = !liveOnly }) }
            item { SkorChip(selected = filter == null, label = "Tümü", onClick = { filter = null }) }
            items(LEAGUES) { l -> SkorChip(selected = filter == l.slug, label = l.name, onClick = { filter = if (filter == l.slug) null else l.slug }) }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                loading && all.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                failed && all.isEmpty() -> Text("Veri alınamadı. İnternetini kontrol et, sonra tekrar dene.\n\nHata: " + com.fatih.skor.data.Espn.lastError, color = Dim, modifier = Modifier.align(Alignment.Center).padding(24.dp), textAlign = TextAlign.Center)
                shown.isEmpty() -> Text("Maç bulunamadı.", color = Dim, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    items(rows.size, key = {
                        when (val r = rows[it]) { is Row2.Day -> "d${r.d}"; is Row2.Lg -> "l${r.d}${r.l.slug}"; is Row2.M -> "m${r.m.id}" }
                    }) { i ->
                        when (val r = rows[i]) {
                            is Row2.Day -> Text(dayHeader(r.d), fontWeight = FontWeight.Bold, color = Brand, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 18.dp, 16.dp, 2.dp))
                            is Row2.Lg -> LeagueHeader(vm, r.l, r.live, r.total, collapsed["${r.d}|${r.l.slug}"] == true, ask) { collapsed["${r.d}|${r.l.slug}"] = !(collapsed["${r.d}|${r.l.slug}"] ?: false) }
                            is Row2.M -> MatchItem(vm, r.m, ask) { onOpen(r.m) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueHeader(vm: ScoreVm, l: League, live: Int, total: Int, closed: Boolean, ask: () -> Unit, toggle: () -> Unit) {
    val rev by vm.rev.collectAsStateWithLifecycle()
    val star = remember(rev) { vm.store.favLeague(l.slug) }
    val bell = remember(rev) { vm.store.leagueBell(l.slug) }
    Row(Modifier.fillMaxWidth().clickable(onClick = toggle).padding(start = 16.dp, end = 4.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(l.name + " · " + l.country, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Dim, modifier = Modifier.weight(1f))
        Text(
            if (live > 0) "● $live/$total" else "$total",
            color = if (live > 0) Color.White else Dim, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.clip(CircleShape).background(if (live > 0) LiveRed else Card).padding(horizontal = 8.dp, vertical = 2.dp),
        )
        StarBell(star, bell, { vm.starLeague(l.slug); ask() }, { vm.bellLeague(l.slug); ask() })
        Icon(if (closed) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp, "Aç/kapat", tint = Dim, modifier = Modifier.padding(end = 8.dp))
    }
}

@Composable
fun StarBell(star: Boolean, bell: Boolean, onStar: () -> Unit, onBell: () -> Unit) {
    IconButton(onClick = onStar, modifier = Modifier.size(36.dp)) {
        Icon(if (star) Icons.Filled.Star else Icons.Filled.StarBorder, "Favori", tint = if (star) StarOn else Dim, modifier = Modifier.size(20.dp))
    }
    IconButton(onClick = onBell, modifier = Modifier.size(36.dp)) {
        Icon(if (bell) Icons.Filled.Notifications else Icons.Filled.NotificationsNone, "Bildirim", tint = if (bell) BrandSecondary else Dim, modifier = Modifier.size(20.dp))
    }
}

/** Maç satırı + favori (yıldız) ve bildirim (çan) simgeleri. */
@Composable
fun MatchItem(vm: ScoreVm, m: Match, ask: () -> Unit, onClick: () -> Unit) {
    val rev by vm.rev.collectAsStateWithLifecycle()
    val star = remember(rev, m.id) { vm.store.favMatch(m.id) }
    val bell = remember(rev, m.id, m.state) { vm.store.bellOn(m) }
    MatchRow(m, star, bell, { vm.starMatch(m); ask() }, { vm.bellMatch(m); ask() }, onClick)
}

@Composable
fun MatchRow(m: Match, star: Boolean, bell: Boolean, onStar: () -> Unit, onBell: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(20.dp)).background(Card)
            .gradientBorder(if (m.live) LiveGradient else HairLine, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(m.statusShort(), color = if (m.live) LiveRed else Dim, fontWeight = if (m.live) FontWeight.Bold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
        }
        Column(Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TeamLine(m.home.logo, m.home.name, (m.homeScore?.let { (m.awayScore ?: 0) < it } ?: false) || !m.finished)
            TeamLine(m.away.logo, m.away.name, (m.awayScore?.let { (m.homeScore ?: 0) < it } ?: false) || !m.finished)
        }
        if (m.homeScore != null) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(m.homeScore.toString(), fontWeight = FontWeight.Bold, color = if (m.live) LiveRed else Color.White)
                Text((m.awayScore ?: 0).toString(), fontWeight = FontWeight.Bold, color = if (m.live) LiveRed else Color.White)
            }
        }
        Column { StarBell(star, bell, onStar, onBell) }
    }
}

@Composable
private fun TeamLine(logo: String?, name: String, strong: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Logo(logo)
        Text(name, modifier = Modifier.padding(start = 10.dp), maxLines = 1, color = if (strong) Color.White else Dim, style = MaterialTheme.typography.bodyMedium)
    }
}
