package com.fatih.skor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.fatih.skor.ui.SkorApp
import com.fatih.skor.ui.SkorTheme
import com.fatih.skor.widget.WidgetUpdater

class MainActivity : ComponentActivity() {
    private val vm: ScoreVm by viewModels()

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(i: android.content.Intent?) {
        val id = i?.getStringExtra("mid") ?: return
        vm.open(i.getStringExtra("mleague"), id, i.getIntExtra("tab", 0))
        i.removeExtra("mid")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetUpdater.schedule(this)
        com.fatih.skor.widget.GoalService.sync(this) // bildirimler açıksa takip servisini (yeniden) başlat
        handle(intent)
        setContent { SkorTheme { SkorApp(vm) } }
    }
}
