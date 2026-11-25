package com.fatih.skor.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.fatih.skor.MainActivity
import com.fatih.skor.R
import com.fatih.skor.data.Espn
import com.fatih.skor.data.Match
import com.fatih.skor.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

/**
 * Favori / zil açık maçlar için bildirimler. Sunucu olmadığı için uygulama kendisi arka planda skorları yoklar.
 * Android, arka planda çalışan servisin sessiz bir bildirimi olmasını zorunlu kılar; kanal en düşük önemde
 * (durum çubuğunda simge görünmez, metin yok).
 */
class GoalService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannels(this)
        ServiceCompat.startForeground(this, 1, ongoing(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        if (job == null) job = scope.launch { loop() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun ongoing(): Notification = NotificationCompat.Builder(this, CH_SERVICE)
        .setSmallIcon(R.drawable.ic_notif)
        .setContentTitle("Skor")
        .setOngoing(true)
        .setSilent(true)
        .setShowWhen(false)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setVisibility(NotificationCompat.VISIBILITY_SECRET)
        .setContentIntent(openApp(this))
        .build()

    private val scores = HashMap<String, Pair<Int, Int>>()
    private val phases = HashMap<String, String>()
    private val seenEvents = HashMap<String, MutableSet<String>>()

    private suspend fun loop() {
        val store = Store(this)
        while (scope.isActive) {
            if (store.alertMode == 0 || !store.hasTargets()) { stopSelf(); return }
            val today = LocalDate.now()
            val all = Espn.range(today.minusDays(1), today.plusDays(1))
            var wait = 60_000L
            if (all != null) {
                val rel = all.filter { store.bellOn(it) }
                for (m in rel) runCatching { process(store, m) }
                val now = Instant.now()
                val horizon = (maxOf(store.remindMin, 90) + 5) * 60L
                wait = when {
                    rel.any { it.live } -> 30_000L
                    rel.any { it.state == "pre" && it.time != null && it.time.isBefore(now.plusSeconds(horizon)) } -> 60_000L
                    else -> 300_000L
                }
            }
            delay(wait)
        }
    }

    private fun phaseOf(m: Match): String {
        val t = m.statusText.lowercase()
        return when {
            m.state == "in" && (t.contains("ht") || t.contains("half")) && !t.contains("1st") && !t.contains("2nd") -> "ht"
            else -> m.state
        }
    }

    private suspend fun process(store: Store, m: Match) {
        val cur = (m.homeScore ?: 0) to (m.awayScore ?: 0)
        val score = "${m.home.short} ${cur.first}-${cur.second} ${m.away.short}"
        val phase = phaseOf(m)
        val prevPhase = phases.put(m.id, phase)
        val prevScore = scores.put(m.id, cur)

        if (prevPhase != null) {
            if (prevPhase == "pre" && phase == "in" && store.flag("start")) notify(m, "Maç başladı", "${m.home.name} - ${m.away.name}", "start")
            if (prevPhase == "in" && phase == "ht" && store.flag("ht")) notify(m, "Devre arası", score, "ht")
            if (prevPhase == "ht" && phase == "in" && store.flag("h2")) notify(m, "İkinci yarı başladı", score, "h2")
            if ((prevPhase == "in" || prevPhase == "ht") && phase == "post" && store.flag("end")) notify(m, "Maç sonucu", score, "end")
        }

        if (m.live) {
            var detail: com.fatih.skor.data.Detail? = null
            if (prevScore != null && cur.first + cur.second > prevScore.first + prevScore.second && store.flag("goal")) {
                detail = Espn.detail(m.league, m.id, m)
                val g = detail?.events?.lastOrNull { it.kind == "goal" }
                notify(m, "⚽ GOL! $score", listOfNotNull(g?.text, m.clock.ifBlank { null }).joinToString(" · ").ifBlank { m.league.name }, "goal${cur.first}-${cur.second}")
            }
            if (store.flag("sub") || store.flag("card")) {
                val d = detail ?: Espn.detail(m.league, m.id, m)
                if (d != null) {
                    val keys = d.events.map { "${it.kind}|${it.minute}|${it.text}|${it.teamId}" }
                    val seen = seenEvents[m.id]
                    if (seen == null) seenEvents[m.id] = keys.toMutableSet()
                    else d.events.forEach { e ->
                        val k = "${e.kind}|${e.minute}|${e.text}|${e.teamId}"
                        if (seen.add(k)) {
                            val who = if (e.teamId == m.home.id) m.home.short else m.away.short
                            when {
                                e.kind == "yellow" && store.flag("card") -> notify(m, "🟨 Sarı kart · ${e.minute}", "${e.text} ($who)", k)
                                e.kind == "red" && store.flag("card") -> notify(m, "🟥 Kırmızı kart · ${e.minute}", "${e.text} ($who)", k)
                                e.kind == "sub" && store.flag("sub") -> notify(m, "🔁 Oyuncu değişikliği · ${e.minute}", "$who: ${e.text}" + (e.text2?.let { " ← $it" } ?: ""), k)
                            }
                        }
                    }
                }
            }
        }

        if (m.state == "pre" && m.time != null) {
            val mins = java.time.Duration.between(Instant.now(), m.time).toMinutes()
            val rm = store.remindMin
            if (store.flag("remind") && mins in 1..rm.toLong() && !store.sent("rm:${m.id}:$rm")) {
                store.markSent("rm:${m.id}:$rm")
                notify(m, "⏰ $mins dk sonra maç", "${m.home.name} - ${m.away.name} · ${m.timeText()}", "remind")
            }
            if (store.flag("lineup") && mins in -5..90 && !store.sent("lu:${m.id}")) {
                val d = Espn.detail(m.league, m.id, m)
                if (d != null && d.lineups.size >= 2 && d.lineups.all { it.starters.isNotEmpty() }) {
                    store.markSent("lu:${m.id}")
                    notify(m, "Kadrolar açıklandı", "${m.home.name} - ${m.away.name}: ilk 11'leri gör", "lineup", tab = 3)
                }
            }
        }
    }

    private fun Match.timeText(): String = time?.atZone(java.time.ZoneId.systemDefault())?.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) ?: ""

    private fun notify(m: Match, title: String, text: String, kind: String, tab: Int = 0) {
        val n = NotificationCompat.Builder(this, CH_GOALS)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openMatch(this, m.league.slug, m.id, tab))
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify((m.id + kind).hashCode(), n)
        } // izin verilmediyse sessizce geçer
    }

    companion object {
        const val CH_GOALS = "goals"
        const val CH_SERVICE = "service"

        fun ensureChannels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_GOALS, "Gol ve maç bildirimleri", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Arka plan (sessiz)", NotificationManager.IMPORTANCE_MIN))
        }

        fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun openMatch(ctx: Context, league: String, id: String, tab: Int = 0): PendingIntent = PendingIntent.getActivity(
            ctx, (id + tab).hashCode(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("mleague", league).putExtra("mid", id).putExtra("tab", tab),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun sync(ctx: Context) {
            val app = ctx.applicationContext
            val st = Store(app)
            if (st.alertMode != 0 && st.hasTargets()) {
                runCatching { ContextCompat.startForegroundService(app, Intent(app, GoalService::class.java)) }
            } else {
                app.stopService(Intent(app, GoalService::class.java))
            }
        }
    }
}
