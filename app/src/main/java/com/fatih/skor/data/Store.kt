package com.fatih.skor.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Favori takımlar ve widget ayarı (yalnızca bu cihazda). */
class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("skor", Context.MODE_PRIVATE)

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

    /** 0 = kapalı, 1 = favori takımlar, 2 = widget ligindeki tüm maçlar */
    var alertMode: Int
        get() = p.getInt("alert_mode", 0)
        set(v) { p.edit().putInt("alert_mode", v).apply() }

    fun saveCache(key: String, payload: WidgetPayload) {
        p.edit().putString("cache_$key", Json.encodeToString(payload)).apply()
    }

    fun loadCache(key: String): WidgetPayload? =
        runCatching { Json.decodeFromString<WidgetPayload>(p.getString("cache_$key", null) ?: return null) }.getOrNull()
}

@Serializable
data class WRow(val left: String, val mid: String, val right: String, val live: Boolean = false)

@Serializable
data class WidgetPayload(val title: String, val updated: String, val rows: List<WRow>)
