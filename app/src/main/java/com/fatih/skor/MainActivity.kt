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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WidgetUpdater.schedule(this)
        setContent { SkorTheme { SkorApp(vm) } }
    }
}
