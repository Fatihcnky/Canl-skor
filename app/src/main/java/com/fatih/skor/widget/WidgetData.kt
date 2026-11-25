package com.fatih.skor.widget

import android.content.Context
import com.fatih.skor.data.Espn
import com.fatih.skor.data.Match
import com.fatih.skor.data.Store
import com.fatih.skor.data.WItem
import com.fatih.skor.data.WidgetPayload
import com.fatih.skor.data.leagueOf
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object WidgetData {
    private val hm = DateTimeFormatter.ofPattern("HH:mm")
    private val dayHm = DateTimeFormatter.ofPattern("EEE HH:mm", Locale("tr"))

    private fun now() = LocalTime.now().format(hm)

    private val dayLabel = DateTimeFormatter.ofPattern("EEEE • dd/MM", Locale("tr"))

    private fun item(m: Match): WItem {
        val z = m.time?.atZone(ZoneId.systemDefault())
        val st = when {
            m.live -> m.clock.ifBlank { "Canlı" }
            m.finished -> "MS"
            else -> z?.format(hm) ?: "-"
        }
        return WItem(
            "match", a = st, b = m.home.short, c = m.away.short,
            d = m.homeScore?.toString() ?: "", e = m.awayScore?.toString() ?: "",
            live = m.live, logo1 = m.home.logo, logo2 = m.away.logo,
        )
    }

    private fun grouped(list: List<Match>): List<WItem> {
        val today = LocalDate.now()
        val out = mutableListOf<WItem>()
        list.sortedBy { it.time }.groupBy { it.localDate() }.forEach { (d, ms) ->
            val label = when (d) {
                today -> "Bugün"; today.plusDays(1) -> "Yarın"; today.minusDays(1) -> "Dün"
                else -> d.format(dayLabel).replaceFirstChar { it.uppercase() }
            }
            out += WItem("day", a = label)
            ms.forEach { out += item(it) }
        }
        return out
    }

    suspend fun matches(ctx: Context): WidgetPayload {
        val store = Store(ctx)
        val league = leagueOf(store.widgetLeague)
        val favs = store.favs()
        val today = LocalDate.now()
        var title = league.name
        var list: List<Match>? = null
        if (favs.isNotEmpty()) {
            val all = Espn.range(today.minusDays(1), today.plusDays(6))
            val mine = all?.filter { it.home.id in favs || it.away.id in favs }.orEmpty()
            if (mine.isNotEmpty()) { list = mine; title = "Favori maçlar" }
            else if (all != null) list = all.filter { it.league == league }
        }
        if (list == null) list = Espn.league(league, today.minusDays(1), today.plusDays(6))
        if (list == null) {
            return store.loadCache("matches") ?: WidgetPayload(title, now(), listOf(WItem("msg", a = "Veri alınamadı")))
        }
        val items = grouped(list.take(40))
        val payload = WidgetPayload(title, now(), items.ifEmpty { listOf(WItem("msg", a = "Yakında maç yok")) })
        store.saveCache("matches", payload)
        return payload
    }

    suspend fun standings(ctx: Context): WidgetPayload {
        val store = Store(ctx)
        val league = leagueOf(store.widgetLeague)
        val g = Espn.standings(league)?.firstOrNull()
        if (g == null) {
            return store.loadCache("standings") ?: WidgetPayload(league.name, now(), listOf(WItem("msg", a = "Veri alınamadı")))
        }
        val rows = g.rows.take(9).map { r ->
            WItem("stand", a = "${r.rank}", b = r.team.short, c = "${r.played}", d = "${if (r.gd > 0) "+" else ""}${r.gd}", e = "${r.points}", logo1 = r.team.logo)
        }
        val payload = WidgetPayload(league.name + " · Puan", now(), rows)
        store.saveCache("standings", payload)
        return payload
    }
}
