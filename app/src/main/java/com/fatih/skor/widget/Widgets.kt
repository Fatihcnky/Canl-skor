package com.fatih.skor.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fatih.skor.MainActivity
import com.fatih.skor.data.WidgetPayload

private val bg = ColorProvider(Color(0xFF121212))
private val white = ColorProvider(Color.White)
private val dim = ColorProvider(Color(0xBDFFFFFF))
private val red = ColorProvider(Color(0xFFF4568B))

@Composable
private fun Body(p: WidgetPayload) {
    Column(
        GlanceModifier.fillMaxSize().background(bg).padding(10.dp).clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(p.title, style = TextStyle(color = ColorProvider(Color(0xFFDED6FE)), fontWeight = FontWeight.Bold, fontSize = 15.sp), modifier = GlanceModifier.defaultWeight())
            Text("↻ ${p.updated}", style = TextStyle(color = dim, fontSize = 11.sp), modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()))
        }
        p.rows.forEach { r ->
            Row(GlanceModifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(r.left, maxLines = 1, style = TextStyle(color = white, fontSize = 13.sp), modifier = GlanceModifier.defaultWeight())
                Text(r.mid, style = TextStyle(color = if (r.live) red else white, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.End))
                if (r.right.isNotEmpty()) Text("  " + r.right, style = TextStyle(color = if (r.live) red else dim, fontSize = 12.sp))
            }
        }
    }
}

class MatchesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.matches(context)
        provideContent { Body(data) }
    }
}

class StandingsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.standings(context)
        provideContent { Body(data) }
    }
}

class RefreshAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        MatchesWidget().updateAll(context)
        StandingsWidget().updateAll(context)
    }
}

class MatchesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MatchesWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); WidgetUpdater.schedule(context) }
}

class StandingsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StandingsWidget()
    override fun onEnabled(context: Context) { super.onEnabled(context); WidgetUpdater.schedule(context) }
}
