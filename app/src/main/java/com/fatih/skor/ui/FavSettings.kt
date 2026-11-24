package com.fatih.skor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.LEAGUES
import com.fatih.skor.data.Match
import java.time.ZoneId

@Composable
fun FavoritesScreen(vm: ScoreVm, modifier: Modifier, onOpen: (Match) -> Unit) {
    val favs by vm.favs.collectAsStateWithLifecycle()
    val all by vm.favMatches.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadFavMatches() }
    val mine = all.filter { it.home.id in favs || it.away.id in favs }

    Column(modifier.fillMaxSize().background(Bg)) {
        Text("Favoriler", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        if (favs.isEmpty()) {
            Text("Henüz favori takım yok. Bir maça gir ve takım adının yanındaki ☆ simgesine dokun.", color = Dim, modifier = Modifier.padding(16.dp))
        } else {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(favs.values.joinToString(" · "), color = Dim, fontSize = 13.sp)
            }
            if (mine.isEmpty()) Text("Önümüzdeki 7 günde favori takımlarının maçı görünmüyor.", color = Dim, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(mine, key = { it.league.slug + it.id }) { m ->
                    val d = m.time?.atZone(ZoneId.systemDefault())?.toLocalDate()
                    Text(
                        (d?.format(dayFmt) ?: "") + " · " + m.league.name, color = Dim, fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp, 10.dp, 16.dp, 2.dp),
                    )
                    MatchRow(m) { onOpen(m) }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: ScoreVm, modifier: Modifier) {
    val wl by vm.widgetLeague.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ayarlar", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Widget ligi", fontWeight = FontWeight.SemiBold)
        Text("Ana ekrana eklediğin widget'lar bu ligi gösterir. Favori takımın varsa maç widget'ında onların maçları öne çıkar.", color = Dim, fontSize = 13.sp)
        LEAGUES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { l -> FilterChip(selected = wl == l.slug, onClick = { vm.setWidgetLeague(l.slug) }, label = { Text(l.name) }) }
            }
        }
        Text("Widget nasıl eklenir?", fontWeight = FontWeight.SemiBold)
        Text("Ana ekranda boş bir yere basılı tut > Widget'lar > Skor. \"Maçlar\" ve \"Puan durumu\" olmak üzere iki widget var. Widget'taki ↻ simgesine dokunarak elle yenileyebilirsin; kendiliğinden yaklaşık 15-30 dakikada bir güncellenir.", color = Dim, fontSize = 13.sp)
        Text("Veri kaynağı", fontWeight = FontWeight.SemiBold)
        Text("Skorlar ve tablolar ESPN'in herkese açık JSON uç noktalarından gelir. Resmî bir API olmadığı için bazen gecikebilir veya bir lig için veri gelmeyebilir.", color = Dim, fontSize = 13.sp)
        TextButton(onClick = { vm.refresh(false); vm.loadStandings(); vm.loadFavMatches() }) { Text("Tüm verileri yenile") }
    }
}
