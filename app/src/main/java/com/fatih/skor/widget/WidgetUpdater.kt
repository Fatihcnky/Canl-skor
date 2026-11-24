package com.fatih.skor.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    /** Widget'ları arka planda yaklaşık 15 dakikada bir (Android'in izin verdiği en sık aralık) yeniler. */
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<WidgetWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx.applicationContext)
            .enqueueUniquePeriodicWork("widgets", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun updateAll(ctx: Context) {
        val app = ctx.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            MatchesWidget().updateAll(app)
            StandingsWidget().updateAll(app)
        }
    }
}

class WidgetWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        MatchesWidget().updateAll(applicationContext)
        StandingsWidget().updateAll(applicationContext)
        return Result.success()
    }
}
