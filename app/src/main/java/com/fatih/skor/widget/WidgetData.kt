package com.fatih.skor.widget

import android.content.Context
import com.fatih.skor.data.Espn
import com.fatih.skor.data.Match
import com.fatih.skor.data.Store
import com.fatih.skor.data.WRow
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

    private fun row(m: Match): WRow {
        val z = m.time?.atZone(ZoneId.systemDefault())
        val today = z?.toLocalDate() == LocalDate.now()
        val mid = when {
            m.homeScore != null -> "${m.homeScore}-${m.awayScore ?: 0}"
            z != null -> z.format(if (today) hm else dayHm)
            else -> "-"
        }
        val right = when {
            m.live -> m.clock.ifBlank { "Canlı" }
            m.finished -> "MS"
            else -> ""
        }
        return WRow("${m.home.short} - ${m.away.short}", mid, right, m.live)
    }

    suspend fun matches(ctx: Context): WidgetPayload {
        val store = Store(ctx)
        val league = leagueOf(store.widgetLeague)
        val favs = store.favs()
        val today = LocalDate.now()
        var title = league.name
        var list: List<Match>? = null
        if (favs.isNotEmpty()) {
            val all = Espn.range(today.minusDays(1), today.plusDays(5))
            val mine = all?.filter { it.home.id in favs || it.away.id in favs }.orEmpty()
            if (mine.isNotEmpty()) { list = mine; title = "Favori maçlar" }
            else if (all != null) list = all.filter { it.league == league }
        }
        if (list == null) list = Espn.league(league, today.minusDays(1), today.plusDays(4))
        if (list == null) {
            return store.loadCache("matches") ?: WidgetPayload(title, now(), listOf(WRow("Veri alınamadı", "", "")))
        }
        val live = list.filter { it.live }
        val next = list.filter { it.state == "pre" }.sortedBy { it.time }
        val done = list.filter { it.finished }.sortedByDescending { it.time }
        val rows = (live + next.take(5) + done.take(3)).take(7).map(::row)
        val payload = WidgetPayload(title, now(), rows.ifEmpty { listOf(WRow("Yakında maç yok", "", "")) })
        store.saveCache("matches", payload)
        return payload
    }

    suspend fun standings(ctx: Context): WidgetPayload {
        val store = Store(ctx)
        val league = leagueOf(store.widgetLeague)
        val g = Espn.standings(league)?.firstOrNull()
        if (g == null) {
            return store.loadCache("standings") ?: WidgetPayload(league.name, now(), listOf(WRow("Veri alınamadı", "", "")))
        }
        val rows = g.rows.take(9).map { r ->
            WRow("${r.rank}. ${r.team.short}", "O${r.played}  AV${if (r.gd > 0) "+" else ""}${r.gd}", "${r.points} P")
        }
        val payload = WidgetPayload(league.name + " · Puan", now(), rows)
        store.saveCache("standings", payload)
        return payload
    }
}
