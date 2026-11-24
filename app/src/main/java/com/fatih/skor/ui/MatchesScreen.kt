package com.fatih.skor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.LEAGUES
import com.fatih.skor.data.Match
import java.time.LocalDate

@Composable
fun MatchesScreen(vm: ScoreVm, modifier: Modifier, onOpen: (Match) -> Unit) {
    val day by vm.day.collectAsStateWithLifecycle()
    val all by vm.matches.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<String?>(null) }
    var liveOnly by remember { mutableStateOf(false) }

    val shown = all.filter { (filter == null || it.league.slug == filter) && (!liveOnly || it.live) }
    val today = LocalDate.now()
    val days = remember { (-3..7).map { today.plusDays(it.toLong()) } }

    Column(modifier.fillMaxSize().background(Bg)) {
        Text("Skor", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        LazyRow(state = rememberLazyListState(2), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(days) { d ->
                val sel = d == day
                Column(
                    Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) Accent else Card)
                        .clickable { vm.setDay(d) }.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (d == today) "Bugün" else d.format(dayFmt).substringBefore(' '), fontSize = 12.sp, color = if (sel) Color.White else Dim)
                    Text(d.dayOfMonth.toString(), fontWeight = FontWeight.Bold)
                }
            }
        }
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = liveOnly, onClick = { liveOnly = !liveOnly }, label = { Text("● Canlı") }) }
            item { FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("Tümü") }) }
            items(LEAGUES) { l -> FilterChip(selected = filter == l.slug, onClick = { filter = if (filter == l.slug) null else l.slug }, label = { Text(l.name) }) }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                loading && all.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                failed && all.isEmpty() -> Text("Veri alınamadı. İnternetini kontrol et, sonra tekrar dene.\n\nHata: " + com.fatih.skor.data.Espn.lastError, color = Dim, modifier = Modifier.align(Alignment.Center).padding(24.dp), textAlign = TextAlign.Center)
                shown.isEmpty() -> Text("Bu gün için maç yok.", color = Dim, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    val groups = shown.groupBy { it.league }.toSortedMap(compareBy { l -> LEAGUES.indexOf(l) })
                    groups.forEach { (league, list) ->
                        item(key = "h" + league.slug) {
                            Text(league.name + " · " + league.country, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Dim, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
                        }
                        items(list, key = { it.league.slug + it.id }) { m -> MatchRow(m) { onOpen(m) } }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchRow(m: Match, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Card)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(52.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(m.statusShort(), color = if (m.live) LiveRed else Dim, fontWeight = if (m.live) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            TeamLine(m.home.logo, m.home.name, (m.homeScore?.let { (m.awayScore ?: 0) < it } ?: false) || !m.finished)
            TeamLine(m.away.logo, m.away.name, (m.awayScore?.let { (m.homeScore ?: 0) < it } ?: false) || !m.finished)
        }
        if (m.homeScore != null) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(m.homeScore.toString(), fontWeight = FontWeight.Bold, color = if (m.live) LiveRed else Color.White)
                Text((m.awayScore ?: 0).toString(), fontWeight = FontWeight.Bold, color = if (m.live) LiveRed else Color.White)
            }
        }
    }
}

@Composable
private fun TeamLine(logo: String?, name: String, strong: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Logo(logo)
        Text(name, modifier = Modifier.padding(start = 10.dp), maxLines = 1, color = if (strong) Color.White else Dim, fontSize = 14.sp)
    }
}
