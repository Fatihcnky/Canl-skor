package com.fatih.skor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fatih.skor.data.Detail
import com.fatih.skor.data.Espn
import com.fatih.skor.data.LEAGUES
import com.fatih.skor.data.League
import com.fatih.skor.data.Match
import com.fatih.skor.data.StandGroup
import com.fatih.skor.data.Store
import com.fatih.skor.widget.GoalService
import com.fatih.skor.widget.WidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScoreVm(app: Application) : AndroidViewModel(app) {
    val store = Store(app)
    val day = MutableStateFlow(LocalDate.now())
    val matches = MutableStateFlow<List<Match>>(emptyList())
    val loading = MutableStateFlow(false)
    val failed = MutableStateFlow(false)
    val favs = MutableStateFlow(store.favs())
    val favMatches = MutableStateFlow<List<Match>>(emptyList())
    val widgetLeague = MutableStateFlow(store.widgetLeague)
    val alertMode = MutableStateFlow(store.alertMode)

    val standLeague = MutableStateFlow(LEAGUES.first())
    val standings = MutableStateFlow<List<StandGroup>?>(null)
    val standLoading = MutableStateFlow(false)

    private var job: Job? = null

    init {
        refresh(false)
        loadStandings()
        loadFavMatches()
        // Canlı maç varken (veya bugüne bakarken) arka planda yavaşça yenile
        viewModelScope.launch {
            while (true) {
                delay(45_000)
                if (matches.value.any { it.live } || day.value == LocalDate.now()) refresh(true)
            }
        }
    }

    fun setDay(d: LocalDate) { day.value = d; refresh(false) }

    fun refresh(silent: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (!silent) loading.value = true
            val d = day.value
            val r = Espn.day(d)
            failed.value = r == null
            if (r != null) matches.value = r
            loading.value = false
        }
    }

    fun loadStandings(l: League = standLeague.value) {
        standLeague.value = l
        viewModelScope.launch {
            standLoading.value = true
            standings.value = null
            standings.value = Espn.standings(l)
            standLoading.value = false
        }
    }

    fun loadFavMatches() {
        viewModelScope.launch {
            val today = LocalDate.now()
            favMatches.value = Espn.range(today.minusDays(2), today.plusDays(7)).orEmpty()
        }
    }

    fun toggleFav(id: String, name: String) {
        favs.value = store.toggleFav(id, name)
        WidgetUpdater.updateAll(getApplication())
    }

    fun setAlert(mode: Int) {
        store.alertMode = mode
        alertMode.value = mode
        GoalService.sync(getApplication())
    }

    fun setWidgetLeague(slug: String) {
        store.widgetLeague = slug
        widgetLeague.value = slug
        WidgetUpdater.updateAll(getApplication())
    }

    suspend fun detail(m: Match): Detail? = Espn.detail(m.league, m.id, m)
}
