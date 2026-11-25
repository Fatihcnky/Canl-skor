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
import androidx.compose.runtime.remember
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
fun FavoritesScreen(vm: ScoreVm, modifier: Modifier, ask: () -> Unit, onOpen: (Match) -> Unit) {
    val favs by vm.favs.collectAsStateWithLifecycle()
    val all by vm.favMatches.collectAsStateWithLifecycle()
    val rev by vm.rev.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.loadFavMatches() }
    val favM = remember(rev) { vm.store.favMatchIds() }
    val favL = remember(rev) { vm.store.favLeagueSlugs() }
    val mine = all.filter { it.home.id in favs || it.away.id in favs || it.id in favM || it.league.slug in favL }
    val byDay = mine.groupBy { it.localDate() ?: java.time.LocalDate.now() }.toSortedMap()

    Column(modifier.fillMaxSize().background(Bg)) {
        Text("Favoriler", color = Brand, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
        if (favs.isEmpty() && favM.isEmpty() && favL.isEmpty()) {
            Text("Henüz favori yok. Maç listesinde bir maça veya lige ☆ ile, maç sayfasında takım adının yanındaki ☆ ile favori ekle. Favoriye eklenenlerin bildirimi (🔔) otomatik açılır.", color = Dim, modifier = Modifier.padding(16.dp))
        } else {
            val names = favs.values.toList() + favL.map { com.fatih.skor.data.leagueOf(it).name }
            if (names.isNotEmpty()) Text(names.joinToString(" · "), color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 16.dp))
            if (mine.isEmpty()) Text("Önümüzdeki günlerde favori maç görünmüyor.", color = Dim, modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                byDay.forEach { (d, list) ->
                    item(key = "d$d") {
                        Text(dayHeader(d), fontWeight = FontWeight.Bold, color = Brand, style = androidx.compose.material3.MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 2.dp))
                    }
                    items(list, key = { "m" + it.league.slug + it.id }) { m -> MatchItem(vm, m, ask) { onOpen(m) } }
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
        Text("Ayarlar", color = Brand, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val rev by vm.rev.collectAsStateWithLifecycle()
        Text("Bildirimler", fontWeight = FontWeight.SemiBold)
        Text("Favori takım/maç/lig ve 🔔'i açık maçlar için bildirim gönderilir. Telefonun arka planda çalışması için Android sessiz bir bildirim satırı gösterir (durum çubuğunda simge ve metin çıkmaz). Telefon yeniden başlarsa uygulamayı bir kez açman gerekir.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        SwitchRow("Bildirimler açık", alert != 0) { chooseAlert(if (it) 1 else 0) }
        val flags = listOf(
            "start" to "Maç başladı", "ht" to "Devre arası", "h2" to "İkinci yarı başladı", "end" to "Maç sonucu",
            "goal" to "Goller", "sub" to "Oyuncu değişikliği", "card" to "Kartlar", "lineup" to "Kadro açıklandı (ilk 11)",
            "remind" to "Maç öncesi hatırlatma",
        )
        flags.forEach { (k, label) ->
            val on = remember(rev) { vm.store.flag(k) }
            SwitchRow(label, on) { vm.setFlag(k, it) }
        }
        val rm = remember(rev) { vm.store.remindMin }
        Text("Hatırlatma: maçtan kaç dakika önce?", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 15, 30, 60, 120).forEach { mnt -> SkorChip(selected = rm == mnt, label = "$mnt dk", onClick = { vm.setRemind(mnt) }) }
        }
        Text("Widget ligi", fontWeight = FontWeight.SemiBold)
        Text("Ana ekrana eklediğin widget'lar bu ligi gösterir. Favori takımın varsa maç widget'ında onların maçları öne çıkar.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        LEAGUES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { l -> SkorChip(selected = wl == l.slug, label = l.name, onClick = { vm.setWidgetLeague(l.slug) }) }
            }
        }
        Text("Widget nasıl eklenir?", fontWeight = FontWeight.SemiBold)
        Text("Ana ekranda boş bir yere basılı tut > Widget'lar > Skor. \"Maçlar\" ve \"Puan durumu\" olmak üzere iki widget var. Widget'taki ↻ simgesine dokunarak elle yenileyebilirsin; kendiliğinden yaklaşık 15-30 dakikada bir güncellenir.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        Text("Veri kaynağı", fontWeight = FontWeight.SemiBold)
        Text("Skorlar ve tablolar ESPN'in herkese açık JSON uç noktalarından gelir. Resmî bir API olmadığı için bazen gecikebilir veya bir lig için veri gelmeyebilir.", color = Dim, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
        TextButton(onClick = { vm.refresh(false); vm.loadStandings(); vm.loadFavMatches() }) { Text("Tüm verileri yenile") }
    }
}

@Composable
private fun SwitchRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Switch(checked = on, onCheckedChange = onChange)
    }
}
