package com.fatih.skor.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Favoriler, bildirim ayarları ve widget ayarı (yalnızca bu cihazda). */
class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("skor", Context.MODE_PRIVATE)

    // ---- Favori takımlar
    fun favs(): Map<String, String> =
        runCatching { Json.decodeFromString<Map<String, String>>(p.getString("favs", "{}") ?: "{}") }.getOrDefault(emptyMap())

    fun toggleFav(id: String, name: String): Map<String, String> {
        val m = favs().toMutableMap()
        if (m.remove(id) == null) m[id] = name
        p.edit().putString("favs", Json.encodeToString(m as Map<String, String>)).apply()
        return m
    }

    var widgetLeague: String
        get() = p.getString("widget_league", "tur.1") ?: "tur.1"
        set(v) { p.edit().putString("widget_league", v).apply() }

    // ---- Bildirim ana şalteri (0 kapalı, 1 açık)
    var alertMode: Int
        get() = p.getInt("alert_mode", 1)
        set(v) { p.edit().putInt("alert_mode", v).apply() }

    // ---- Bildirim türleri
    fun flag(key: String, def: Boolean = true): Boolean = p.getBoolean("n_$key", def)
    fun setFlag(key: String, v: Boolean) { p.edit().putBoolean("n_$key", v).apply() }
    var remindMin: Int
        get() = p.getInt("remind_min", 30)
        set(v) { p.edit().putInt("remind_min", v).apply() }

    // ---- Favori maç / lig ve zil (bildirim) durumu
    private fun set(key: String): Set<String> = p.getStringSet(key, emptySet())?.toSet() ?: emptySet()
    private fun flip(key: String, v: String, on: Boolean) {
        val s = set(key).toMutableSet()
        if (on) s.add(v) else s.remove(v)
        p.edit().putStringSet(key, s).apply()
    }

    fun favMatch(id: String) = id in set("fav_matches")
    fun favLeague(slug: String) = slug in set("fav_leagues")
    fun favMatchIds(): Set<String> = set("fav_matches")
    fun favLeagueSlugs(): Set<String> = set("fav_leagues")

    /** Maça yıldız: maç favoriye girer, zili otomatik açılır. */
    fun setFavMatch(id: String, on: Boolean) {
        flip("fav_matches", id, on)
        flip("bell_on", "m:$id", false); flip("bell_off", "m:$id", false)
    }

    fun setFavLeague(slug: String, on: Boolean) {
        flip("fav_leagues", slug, on)
        flip("bell_on", "l:$slug", false); flip("bell_off", "l:$slug", false)
    }

    fun leagueBell(slug: String): Boolean {
        if ("l:$slug" in set("bell_off")) return false
        return "l:$slug" in set("bell_on") || favLeague(slug)
    }

    fun setLeagueBell(slug: String, on: Boolean) {
        flip("bell_on", "l:$slug", on); flip("bell_off", "l:$slug", !on)
    }

    /** Bu maç için bildirim açık mı? Açık seçim > favori maç/takım > lig zili. */
    fun bellOn(m: Match): Boolean {
        if ("m:${m.id}" in set("bell_off")) return false
        if ("m:${m.id}" in set("bell_on")) return true
        if (favMatch(m.id)) return true
        val f = favs()
        if (m.home.id in f || m.away.id in f) return true
        return leagueBell(m.league.slug)
    }

    fun setMatchBell(id: String, on: Boolean) {
        flip("bell_on", "m:$id", on); flip("bell_off", "m:$id", !on)
    }

    /** Takip edilecek bir şey var mı (servis gerekli mi)? */
    fun hasTargets(): Boolean =
        favs().isNotEmpty() || set("fav_matches").isNotEmpty() || set("fav_leagues").isNotEmpty() ||
            set("bell_on").isNotEmpty()

    // ---- Gönderilmiş bildirimler (tekrarı önlemek için)
    fun sent(key: String) = key in set("sent")
    fun markSent(key: String) {
        val s = set("sent").toMutableSet()
        s.add(key)
        val trimmed = if (s.size > 300) s.toList().takeLast(200).toSet() else s
        p.edit().putStringSet("sent", trimmed).apply()
    }

    fun saveCache(key: String, payload: WidgetPayload) {
        p.edit().putString("cache_$key", Json.encodeToString(payload)).apply()
    }

    fun loadCache(key: String): WidgetPayload? =
        runCatching { Json.decodeFromString<WidgetPayload>(p.getString("cache_$key", null) ?: return null) }.getOrNull()
}

/** Widget satırı: type = day | league | match | stand | msg */
@Serializable
data class WItem(
    val type: String, val a: String = "", val b: String = "", val c: String = "",
    val d: String = "", val e: String = "", val live: Boolean = false,
    val logo1: String? = null, val logo2: String? = null,
    val id: String = "", val league: String = "",
)

@Serializable
data class WidgetPayload(val title: String, val updated: String, val items: List<WItem>)
