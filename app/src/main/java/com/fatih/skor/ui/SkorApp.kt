package com.fatih.skor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fatih.skor.ScoreVm
import com.fatih.skor.data.Match

@Composable
fun SkorApp(vm: ScoreVm) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var detail by remember { mutableStateOf<Match?>(null) }
    var startTab by remember { mutableStateOf(0) }
    val req by vm.openReq.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(req) {
        req?.let { (m, t) -> detail = m; startTab = t; vm.openReq.value = null }
    }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { _ -> }
    val ask: () -> Unit = {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    val open = detail
    if (open != null) {
        BackHandler { detail = null }
        DetailScreen(vm, open, startTab, ask) { detail = null }
    } else {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                val items = listOf(
                    "Maçlar" to Icons.Filled.SportsSoccer,
                    "Puan" to Icons.Filled.EmojiEvents,
                    "Favoriler" to Icons.Filled.Star,
                    "Ayarlar" to Icons.Filled.Settings,
                )
                Row(
                    Modifier.fillMaxWidth().background(Bg).navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
                ) {
                    items.forEachIndexed { i, (label, icon) ->
                        val sel = tab == i
                        Row(
                            Modifier.clip(CircleShape)
                                .then(if (sel) Modifier.background(Brush.horizontalGradient(PillGradient)) else Modifier)
                                .clickable { tab = i }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, label, tint = if (sel) Color.Black else Dim)
                            if (sel) Text(label, color = Color.Black, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp), maxLines = 1)
                        }
                    }
                }
            },
        ) { pad ->
            val m = Modifier.padding(pad)
            when (tab) {
                0 -> MatchesScreen(vm, m, ask) { detail = it; startTab = 0 }
                1 -> TableScreen(vm, m)
                2 -> FavoritesScreen(vm, m, ask) { detail = it; startTab = 0 }
                else -> SettingsScreen(vm, m)
            }
        }
    }
}
