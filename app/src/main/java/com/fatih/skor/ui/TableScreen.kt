package com.fatih.skor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.LEAGUES

private fun parseColor(s: String?): Color? = runCatching {
    s?.let { Color(android.graphics.Color.parseColor(if (it.startsWith("#")) it else "#$it")) }
}.getOrNull()

@Composable
fun TableScreen(vm: ScoreVm, modifier: Modifier) {
    val league by vm.standLeague.collectAsStateWithLifecycle()
    val groups by vm.standings.collectAsStateWithLifecycle()
    val loading by vm.standLoading.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().background(Bg)) {
        Text("Puan Durumu", color = Brand, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(LEAGUES) { l -> SkorChip(selected = league == l, label = l.name, onClick = { vm.loadStandings(l) }) }
        }
        Box(Modifier.fillMaxSize()) {
            val g = groups
            when {
                loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                g.isNullOrEmpty() -> Text("Puan durumu alınamadı (bu lig şu an tablo yayınlamıyor olabilir).\n\nHata: " + com.fatih.skor.data.Espn.lastError, color = Dim, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center).padding(24.dp))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    g.forEach { group ->
                        item {
                            if (group.name.isNotBlank() && g.size > 1) Text(group.name, color = Dim, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text("#", Modifier.width(34.dp), color = Dim, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                                Text("Takım", Modifier.weight(1f), color = Dim, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                                listOf("O", "G", "B", "M", "AV", "P").forEach { Text(it, Modifier.width(if (it == "AV" || it == "P") 34.dp else 26.dp), color = Dim, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }
                            }
                        }
                        items(group.rows) { r ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).clip(RoundedCornerShape(12.dp)).background(Card).height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(4.dp).fillMaxHeight().background(parseColor(r.color) ?: Color.Transparent))
                                Text(r.rank.toString(), Modifier.width(30.dp), textAlign = TextAlign.Center, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                Logo(r.team.logo, 20.dp)
                                Text(r.team.name, Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                                listOf(r.played, r.won, r.drawn, r.lost).forEach { Text("$it", Modifier.width(26.dp), textAlign = TextAlign.Center, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Dim) }
                                Text(if (r.gd > 0) "+${r.gd}" else "${r.gd}", Modifier.width(34.dp), textAlign = TextAlign.Center, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = Dim)
                                Text("${r.points}", Modifier.width(34.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
