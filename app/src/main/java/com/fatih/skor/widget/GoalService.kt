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
import com.fatih.skor.data.LEAGUES
import com.fatih.skor.data.Match
import com.fatih.skor.data.Store
import com.fatih.skor.data.leagueOf
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
 * Gol/maç başladı/maç bitti bildirimleri. Sunucu olmadığı için uygulama kendisi arka planda
 * (kalıcı bir "takip açık" bildirimiyle) skorları yoklar: canlı maç varken 30 sn, yoksa seyrek.
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
        .setContentTitle("Gol takibi açık")
        .setContentText("Canlı skorlar arka planda izleniyor")
        .setOngoing(true)
        .setContentIntent(openApp(this))
        .build()

    private suspend fun loop() {
        val store = Store(this)
        val seen = HashMap<String, Pair<Int, Int>>()
        val states = HashMap<String, String>()
        while (scope.isActive) {
            val mode = store.alertMode
            if (mode == 0) { stopSelf(); return }
            val today = LocalDate.now()
            val leagues = if (mode == 2) listOf(leagueOf(store.widgetLeague)) else LEAGUES
            val all = Espn.range(today.minusDays(1), today.plusDays(1), leagues)
            var wait = 60_000L
            if (all != null) {
                val favs = store.favs()
                val rel = if (mode == 2) all else all.filter { it.home.id in favs || it.away.id in favs }
                for (m in rel) check(m, seen, states)
                val now = Instant.now()
                wait = when {
                    rel.any { it.live } -> 30_000L
                    rel.any { it.state == "pre" && it.time != null && it.time.isBefore(now.plusSeconds(1800)) } -> 60_000L
                    else -> 300_000L
                }
            }
            delay(wait)
        }
    }

    private suspend fun check(m: Match, seen: HashMap<String, Pair<Int, Int>>, states: HashMap<String, String>) {
        val prevState = states.put(m.id, m.state)
        val cur = (m.homeScore ?: 0) to (m.awayScore ?: 0)
        val prev = seen.put(m.id, cur)
        val score = "${m.home.short} ${cur.first}-${cur.second} ${m.away.short}"
        if (prevState == "pre" && m.live) notify(m, "Maç başladı", "${m.home.name} - ${m.away.name}", 1)
        if (prevState == "in" && m.finished) notify(m, "Maç bitti", score, 3)
        if (m.live && prev != null && cur.first + cur.second > prev.first + prev.second) {
            // Golü atan oyuncuyu maç detayından bulmayı dene (olmazsa yalnız skor)
            val scorer = runCatching {
                Espn.detail(m.league, m.id, m)?.events?.lastOrNull { it.kind == "goal" }?.text
            }.getOrNull()
            notify(m, "⚽ GOL! $score", listOfNotNull(scorer, m.clock.ifBlank { null }).joinToString(" · ").ifBlank { m.league.name }, 2)
        }
    }

    private fun notify(m: Match, title: String, text: String, kind: Int) {
        val n = NotificationCompat.Builder(this, CH_GOALS)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(this))
            .build()
        runCatching {
            NotificationManagerCompat.from(this).notify((m.id + kind + System.currentTimeMillis() / 1000).hashCode(), n)
        } // izin verilmediyse sessizce geçer
    }

    companion object {
        const val CH_GOALS = "goals"
        const val CH_SERVICE = "service"

        fun ensureChannels(ctx: Context) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CH_GOALS, "Gol ve maç bildirimleri", NotificationManager.IMPORTANCE_HIGH))
            nm.createNotificationChannel(NotificationChannel(CH_SERVICE, "Gol takibi (arka plan)", NotificationManager.IMPORTANCE_LOW))
        }

        fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun sync(ctx: Context) {
            val app = ctx.applicationContext
            if (Store(app).alertMode != 0) {
                runCatching { ContextCompat.startForegroundService(app, Intent(app, GoalService::class.java)) }
            } else {
                app.stopService(Intent(app, GoalService::class.java))
            }
        }
    }
}
