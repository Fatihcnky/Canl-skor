package com.fatih.skor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.Match

@Composable
fun SkorApp(vm: ScoreVm) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var detail by remember { mutableStateOf<Match?>(null) }
    val open = detail
    if (open != null) {
        BackHandler { detail = null }
        DetailScreen(vm, open) { detail = null }
    } else {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                NavigationBar(containerColor = Card) {
                    val items = listOf(
                        "Maçlar" to Icons.Filled.SportsSoccer,
                        "Puan" to Icons.Filled.EmojiEvents,
                        "Favoriler" to Icons.Filled.Star,
                        "Ayarlar" to Icons.Filled.Settings,
                    )
                    items.forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(
                            selected = tab == i, onClick = { tab = i },
                            icon = { Icon(icon, label) }, label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Bg),
                        )
                    }
                }
            },
        ) { pad ->
            val m = Modifier.padding(pad)
            when (tab) {
                0 -> MatchesScreen(vm, m) { detail = it }
                1 -> TableScreen(vm, m)
                2 -> FavoritesScreen(vm, m) { detail = it }
                else -> SettingsScreen(vm, m)
            }
        }
    }
}
