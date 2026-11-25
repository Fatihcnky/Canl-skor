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
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.layout.Spacer
import androidx.glance.layout.size
import androidx.glance.layout.width
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
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
import com.fatih.skor.R
import androidx.glance.action.actionParametersOf
import com.fatih.skor.data.WItem
import com.fatih.skor.data.WidgetPayload

private val bg = ColorProvider(Color(0xFF121212))
private val white = ColorProvider(Color.White)
private val dim = ColorProvider(Color(0xBDFFFFFF))
private val red = ColorProvider(Color(0xFFF4568B))

private val accent = ColorProvider(Color(0xFFBBFDFD))
private val lav = ColorProvider(Color(0xFFDED6FE))
private val KeyId = ActionParameters.Key<String>("mid")
private val KeyLg = ActionParameters.Key<String>("mleague")

@Composable
private fun Body(p: WidgetPayload, logos: Map<String, Bitmap>) {
    Column(GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.w_bg)).padding(8.dp)) {
        Row(GlanceModifier.fillMaxWidth().padding(start = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p.title, maxLines = 1, style = TextStyle(color = lav, fontWeight = FontWeight.Bold, fontSize = 15.sp), modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity<MainActivity>()))
            Text("↻ ${p.updated}", style = TextStyle(color = white, fontSize = 11.sp), modifier = GlanceModifier.background(ImageProvider(R.drawable.w_pill)).padding(horizontal = 9.dp, vertical = 3.dp).clickable(actionRunCallback<RefreshAction>()))
        }
        LazyColumn(GlanceModifier.fillMaxSize()) {
            items(p.items.size) { i ->
                val it = p.items[i]
                when (it.type) {
                    "day" -> Text(it.a, style = TextStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp), modifier = GlanceModifier.fillMaxWidth().padding(start = 6.dp, top = 8.dp, bottom = 2.dp))
                    "league" -> Text(it.a, maxLines = 1, style = TextStyle(color = dim, fontSize = 11.sp), modifier = GlanceModifier.fillMaxWidth().padding(start = 6.dp, top = 3.dp, bottom = 2.dp))
                    "match" -> MatchRow(it, logos)
                    "stand" -> StandRow(it, logos)
                    else -> Text(it.a, style = TextStyle(color = dim, fontSize = 13.sp), modifier = GlanceModifier.padding(12.dp))
                }
            }
        }
    }
}

@Composable
private fun Logo(url: String?, logos: Map<String, Bitmap>) {
    val b = url?.let { logos[it] }
    if (b != null) Image(ImageProvider(b), null, modifier = GlanceModifier.size(20.dp))
    else Spacer(GlanceModifier.size(20.dp))
}

@Composable
private fun MatchRow(it: WItem, logos: Map<String, Bitmap>) {
    val c = if (it.live) red else white
    val open = actionStartActivity<MainActivity>(actionParametersOf(KeyId to it.id, KeyLg to it.league))
    Row(
        GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).background(ImageProvider(if (it.live) R.drawable.w_card_live else R.drawable.w_card))
            .padding(horizontal = 8.dp, vertical = 8.dp).clickable(open),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(it.a, maxLines = 1, style = TextStyle(color = if (it.live) red else dim, fontSize = 11.sp, fontWeight = if (it.live) FontWeight.Bold else FontWeight.Normal), modifier = GlanceModifier.width(36.dp))
        Text(it.b, maxLines = 1, style = TextStyle(color = white, fontSize = 12.sp, textAlign = TextAlign.End), modifier = GlanceModifier.defaultWeight())
        Spacer(GlanceModifier.width(4.dp))
        Logo(it.logo1, logos)
        Text(
            if (it.d.isEmpty()) "-" else "${it.d} - ${it.e}", maxLines = 1,
            style = TextStyle(color = c, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center),
            modifier = GlanceModifier.width(46.dp).background(ImageProvider(R.drawable.w_score)).padding(vertical = 2.dp),
        )
        Logo(it.logo2, logos)
        Spacer(GlanceModifier.width(4.dp))
        Text(it.c, maxLines = 1, style = TextStyle(color = white, fontSize = 12.sp), modifier = GlanceModifier.defaultWeight())
    }
}

@Composable
private fun StandRow(it: WItem, logos: Map<String, Bitmap>) {
    Row(GlanceModifier.fillMaxWidth().padding(vertical = 2.dp).background(ImageProvider(R.drawable.w_card)).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(it.a, style = TextStyle(color = dim, fontSize = 12.sp), modifier = GlanceModifier.width(22.dp))
        Logo(it.logo1, logos)
        Text("  " + it.b, maxLines = 1, style = TextStyle(color = white, fontSize = 13.sp), modifier = GlanceModifier.defaultWeight())
        Text(it.c, style = TextStyle(color = dim, fontSize = 12.sp), modifier = GlanceModifier.width(26.dp))
        Text(it.d, style = TextStyle(color = dim, fontSize = 12.sp), modifier = GlanceModifier.width(32.dp))
        Text(it.e, style = TextStyle(color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp), modifier = GlanceModifier.width(28.dp))
    }
}

private suspend fun loadLogos(ctx: Context, p: WidgetPayload): Map<String, Bitmap> = coroutineScope {
    val urls = p.items.flatMap { listOfNotNull(it.logo1, it.logo2) }.distinct().take(40)
    val loader = ctx.imageLoader
    urls.map { u ->
        async {
            val bmp = withTimeoutOrNull(6000) {
                val r = loader.execute(ImageRequest.Builder(ctx).data(u).size(48).allowHardware(false).build())
                (r as? SuccessResult)?.drawable?.toBitmap()
            }
            if (bmp != null) u to bmp else null
        }
    }.awaitAll().filterNotNull().toMap()
}

class MatchesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.matches(context)
        val logos = loadLogos(context, data)
        provideContent { Body(data, logos) }
    }
}

class StandingsWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetData.standings(context)
        val logos = loadLogos(context, data)
        provideContent { Body(data, logos) }
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
