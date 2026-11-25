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
import androidx.compose.runtime.setValue
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
        Text("Favoriler", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        if (favs.isEmpty()) {
            Text("Henüz favori takım yok. Bir maça gir ve takım adının yanındaki ☆ simgesine dokun.", color = Dim, modifier = Modifier.padding(16.dp))
        } else {
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(favs.values.joinToString(" · "), color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            }
            if (mine.isEmpty()) Text("Önümüzdeki 7 günde favori takımlarının maçı görünmüyor.", color = Dim, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(mine, key = { it.league.slug + it.id }) { m ->
                    val d = m.time?.atZone(ZoneId.systemDefault())?.toLocalDate()
                    Text(
                        (d?.format(dayFmt) ?: "") + " · " + m.league.name, color = Dim, style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
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
    val alert by vm.alertMode.collectAsStateWithLifecycle()
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(-1) }
    val permLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { _ -> if (pending >= 0) vm.setAlert(pending); pending = -1 }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    fun chooseAlert(mode: Int) {
        val need = mode > 0 && android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (need) { pending = mode; permLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) } else vm.setAlert(mode)
    }
    Column(modifier.fillMaxSize().background(Bg).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ayarlar", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Gol bildirimleri", fontWeight = FontWeight.SemiBold)
        Text("Açıkken uygulama arka planda skorları izler ve gol, maç başladı, maç bitti bildirimi gönderir. Telefonda sürekli \"Gol takibi açık\" bildirimi görünür; pil kullanımı biraz artar. Telefon yeniden başlarsa uygulamayı bir kez açman gerekir.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = alert == 0, onClick = { chooseAlert(0) }, label = { Text("Kapalı") })
            FilterChip(selected = alert == 1, onClick = { chooseAlert(1) }, label = { Text("Favori takımlar") })
            FilterChip(selected = alert == 2, onClick = { chooseAlert(2) }, label = { Text("Widget ligi") })
        }
        Text("Widget ligi", fontWeight = FontWeight.SemiBold)
        Text("Ana ekrana eklediğin widget'lar bu ligi gösterir. Favori takımın varsa maç widget'ında onların maçları öne çıkar.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        LEAGUES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { l -> FilterChip(selected = wl == l.slug, onClick = { vm.setWidgetLeague(l.slug) }, label = { Text(l.name) }) }
            }
        }
        Text("Widget nasıl eklenir?", fontWeight = FontWeight.SemiBold)
        Text("Ana ekranda boş bir yere basılı tut > Widget'lar > Skor. \"Maçlar\" ve \"Puan durumu\" olmak üzere iki widget var. Widget'taki ↻ simgesine dokunarak elle yenileyebilirsin; kendiliğinden yaklaşık 15-30 dakikada bir güncellenir.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Text("Veri kaynağı", fontWeight = FontWeight.SemiBold)
        Text("Skorlar ve tablolar ESPN'in herkese açık JSON uç noktalarından gelir. Resmî bir API olmadığı için bazen gecikebilir veya bir lig için veri gelmeyebilir.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { vm.refresh(false); vm.loadStandings(); vm.loadFavMatches() }) { Text("Tüm verileri yenile") }
    }
}
