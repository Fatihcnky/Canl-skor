package com.fatih.skor.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class League(val slug: String, val name: String, val country: String)

val LEAGUES = listOf(
    League("tur.1", "Süper Lig", "Türkiye"),
    League("uefa.champions", "UEFA Şampiyonlar Ligi", "Avrupa"),
    League("uefa.europa", "UEFA Avrupa Ligi", "Avrupa"),
    League("uefa.europa.conf", "UEFA Konferans Ligi", "Avrupa"),
    League("eng.1", "Premier Lig", "İngiltere"),
    League("esp.1", "La Liga", "İspanya"),
    League("ita.1", "Serie A", "İtalya"),
    League("ger.1", "Bundesliga", "Almanya"),
    League("fra.1", "Ligue 1", "Fransa"),
)

fun leagueOf(slug: String) = LEAGUES.firstOrNull { it.slug == slug } ?: LEAGUES.first()

data class Team(val id: String, val name: String, val short: String, val logo: String?)

data class Match(
    val id: String,
    val league: League,
    val time: Instant?,
    val state: String, // pre | in | post
    val statusText: String,
    val clock: String,
    val home: Team,
    val away: Team,
    val homeScore: Int?,
    val awayScore: Int?,
    val venue: String?,
) {
    val live get() = state == "in"
    val finished get() = state == "post"
    fun localDate(): LocalDate? = time?.atZone(ZoneId.systemDefault())?.toLocalDate()
}

data class StandRow(
    val rank: Int,
    val team: Team,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val gd: Int,
    val points: Int,
    val color: String?,
)

data class StandGroup(val name: String, val rows: List<StandRow>)

data class Ev(
    val minute: String, val kind: String, val text: String, val teamId: String?,
    val text2: String? = null, val score: String? = null, val note: String? = null,
)
data class StatRow(val label: String, val home: String, val away: String)
data class Player(val name: String, val jersey: String, val pos: String, val place: Int = 0)
data class Lineup(val team: Team, val formation: String?, val starters: List<Player>, val bench: List<Player>)
data class Info(
    val venue: String?, val city: String?, val referee: String?, val attendance: String?, val broadcasts: List<String>,
)
data class FormGame(val result: String, val score: String, val oppName: String, val oppLogo: String?)
data class Comment(val minute: String, val text: String)

data class Detail(
    val match: Match?,
    val events: List<Ev>,
    val stats: List<StatRow>,
    val lineups: List<Lineup>,
    val info: Info? = null,
    val formHome: List<FormGame> = emptyList(),
    val formAway: List<FormGame> = emptyList(),
    val comments: List<Comment> = emptyList(),
)
