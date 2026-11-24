package com.fatih.skor.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private fun JsonElement?.o(): JsonObject? = this as? JsonObject
private fun JsonElement?.a(): JsonArray? = this as? JsonArray
private fun JsonElement?.s(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
private fun JsonObject?.str(k: String): String? = this?.get(k).s()
private fun JsonObject?.obj(k: String): JsonObject? = this?.get(k).o()
private fun JsonObject?.arr(k: String): JsonArray? = this?.get(k).a()

/**
 * Veri kaynağı: ESPN'in herkese açık (anahtarsız) JSON uç noktaları.
 * Resmî/garantili bir API değildir; biçim değişirse alanlar boş gelebilir, bu yüzden her şey toleranslı ayrıştırılır.
 */
object Espn {
    private const val SITE = "https://site.api.espn.com/apis/site/v2/sports/soccer"
    private const val V2 = "https://site.api.espn.com/apis/v2/sports/soccer"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
    private val ymd = DateTimeFormatter.ofPattern("yyyyMMdd")

    private suspend fun get(url: String): JsonObject? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Android) Skor").build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) null else json.parseToJsonElement(r.body!!.string()).o()
            }
        }.getOrNull()
    }

    private fun parseTime(s: String?): Instant? {
        if (s == null) return null
        val fixed = if (Regex("T\\d{2}:\\d{2}Z$").containsMatchIn(s)) s.dropLast(1) + ":00Z" else s
        return runCatching { Instant.parse(fixed) }.getOrNull()
    }

    private fun team(t: JsonObject?): Team? {
        t ?: return null
        val id = t.str("id") ?: return null
        val name = t.str("displayName") ?: t.str("name") ?: return null
        val logo = t.str("logo") ?: t.arr("logos")?.firstOrNull().o().str("href")
        return Team(id, name, t.str("shortDisplayName") ?: name, logo ?: "https://a.espncdn.com/i/teamlogos/soccer/500/$id.png")
    }

    private fun parseEvent(e: JsonObject, league: League): Match? {
        val comp = e.arr("competitions")?.firstOrNull().o() ?: return null
        val cs = comp.arr("competitors")?.mapNotNull { it.o() } ?: return null
        val h = cs.firstOrNull { it.str("homeAway") == "home" } ?: return null
        val a = cs.firstOrNull { it.str("homeAway") == "away" } ?: return null
        val ht = team(h.obj("team")) ?: return null
        val at = team(a.obj("team")) ?: return null
        val st = e.obj("status") ?: comp.obj("status")
        val type = st.obj("type")
        val state = type.str("state") ?: "pre"
        return Match(
            id = e.str("id") ?: return null,
            league = league,
            time = parseTime(e.str("date") ?: comp.str("date")),
            state = state,
            statusText = type.str("shortDetail") ?: type.str("detail") ?: "",
            clock = st.str("displayClock") ?: "",
            home = ht, away = at,
            homeScore = if (state == "pre") null else h.str("score")?.toIntOrNull(),
            awayScore = if (state == "pre") null else a.str("score")?.toIntOrNull(),
            venue = comp.obj("venue").str("fullName"),
        )
    }

    suspend fun league(league: League, from: LocalDate, to: LocalDate): List<Match>? {
        val url = "$SITE/${league.slug}/scoreboard?dates=${ymd.format(from)}-${ymd.format(to)}&limit=300"
        val root = get(url) ?: return null
        return root.arr("events")?.mapNotNull { it.o()?.let { e -> parseEvent(e, league) } } ?: emptyList()
    }

    /** Seçili günün (yerel saat) tüm lig maçları. Hepsi başarısızsa null. */
    suspend fun day(d: LocalDate, leagues: List<League> = LEAGUES): List<Match>? = range(d.minusDays(1), d.plusDays(1), leagues)
        ?.filter { it.localDate() == d }

    suspend fun range(from: LocalDate, to: LocalDate, leagues: List<League> = LEAGUES): List<Match>? = coroutineScope {
        val parts = leagues.map { l -> async { league(l, from, to) } }.awaitAll()
        if (parts.all { it == null }) null
        else parts.filterNotNull().flatten().distinctBy { it.id }.sortedBy { it.time }
    }

    suspend fun standings(league: League): List<StandGroup>? {
        val root = get("$V2/${league.slug}/standings") ?: return null
        val groups = root.arr("children")?.mapNotNull { it.o() } ?: return null
        return groups.map { g ->
            val entries = g.obj("standings").arr("entries")?.mapNotNull { it.o() } ?: emptyList()
            val rows = entries.mapIndexedNotNull { i, en ->
                val t = team(en.obj("team")) ?: return@mapIndexedNotNull null
                val stats = en.arr("stats")?.mapNotNull { it.o() } ?: emptyList()
                fun st(vararg names: String): Int? = names.firstNotNullOfOrNull { n ->
                    stats.firstOrNull { it.str("name") == n }?.let { s ->
                        (s["value"] as? JsonPrimitive)?.content?.toDoubleOrNull()?.toInt()
                    }
                }
                StandRow(
                    rank = st("rank") ?: (i + 1), team = t,
                    played = st("gamesPlayed") ?: 0, won = st("wins") ?: 0,
                    drawn = st("ties", "draws") ?: 0, lost = st("losses") ?: 0,
                    gd = st("pointDifferential", "goalDifference") ?: 0,
                    points = st("points") ?: 0,
                    color = en.obj("note").str("color"),
                )
            }.sortedBy { it.rank }
            StandGroup(g.str("name") ?: "", rows)
        }.filter { it.rows.isNotEmpty() }
    }

    suspend fun detail(league: League, id: String, base: Match): Detail? {
        val root = get("$SITE/${league.slug}/summary?event=$id") ?: return null
        // Güncel skor/durum
        val comp = root.obj("header").arr("competitions")?.firstOrNull().o()
        var match: Match? = null
        if (comp != null) {
            val cs = comp.arr("competitors")?.mapNotNull { it.o() } ?: emptyList()
            val h = cs.firstOrNull { it.str("homeAway") == "home" }
            val a = cs.firstOrNull { it.str("homeAway") == "away" }
            val type = comp.obj("status").obj("type")
            val state = type.str("state") ?: base.state
            if (h != null && a != null) {
                match = base.copy(
                    state = state,
                    statusText = type.str("shortDetail") ?: type.str("detail") ?: base.statusText,
                    clock = comp.obj("status").str("displayClock") ?: base.clock,
                    homeScore = if (state == "pre") null else h.str("score")?.toIntOrNull() ?: base.homeScore,
                    awayScore = if (state == "pre") null else a.str("score")?.toIntOrNull() ?: base.awayScore,
                )
            }
        }
        val events = root.arr("keyEvents")?.mapNotNull { it.o() }?.mapNotNull { k ->
            val type = k.obj("type").str("text") ?: return@mapNotNull null
            val low = type.lowercase()
            val kind = when {
                "goal" in low || "penalty - scored" in low -> "goal"
                "yellow" in low -> "yellow"
                "red" in low -> "red"
                "substitution" in low -> "sub"
                "var" in low -> "var"
                else -> return@mapNotNull null
            }
            val who = k.arr("participants")?.firstOrNull().o().obj("athlete").str("displayName")
            Ev(
                minute = k.obj("clock").str("displayValue") ?: "",
                kind = kind,
                text = who ?: k.str("shortText") ?: k.str("text") ?: type,
                teamId = k.obj("team").str("id"),
            )
        } ?: emptyList()
        val teams = root.obj("boxscore").arr("teams")?.mapNotNull { it.o() } ?: emptyList()
        val homeStats = teams.firstOrNull { it.obj("team").str("id") == base.home.id }
        val awayStats = teams.firstOrNull { it.obj("team").str("id") == base.away.id }
        val wanted = listOf(
            "possessionPct" to "Topla oynama (%)", "totalShots" to "Şut", "shotsOnTarget" to "İsabetli şut",
            "wonCorners" to "Korner", "foulsCommitted" to "Faul", "yellowCards" to "Sarı kart",
            "redCards" to "Kırmızı kart", "offsides" to "Ofsayt", "saves" to "Kurtarış",
        )
        fun sv(t: JsonObject?, n: String) = t.arr("statistics")?.mapNotNull { it.o() }?.firstOrNull { it.str("name") == n }?.str("displayValue")
        val stats = wanted.mapNotNull { (n, label) ->
            val hv = sv(homeStats, n); val av = sv(awayStats, n)
            if (hv == null && av == null) null else StatRow(label, hv ?: "-", av ?: "-")
        }
        val lineups = root.arr("rosters")?.mapNotNull { it.o() }?.mapNotNull { r ->
            val t = team(r.obj("team")) ?: return@mapNotNull null
            val all = r.arr("roster")?.mapNotNull { it.o() } ?: emptyList()
            fun pl(p: JsonObject) = Player(
                p.obj("athlete").str("displayName") ?: p.obj("athlete").str("fullName") ?: "?",
                p.str("jersey") ?: "", p.obj("position").str("abbreviation") ?: "",
            )
            val starters = all.filter { it["starter"].s() == "true" }.map(::pl)
            val bench = all.filter { it["starter"].s() != "true" }.map(::pl)
            Lineup(t, r.str("formation"), starters, bench)
        } ?: emptyList()
        return Detail(match, events, stats, lineups)
    }
}
