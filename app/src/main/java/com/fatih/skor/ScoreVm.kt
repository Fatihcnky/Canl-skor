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

    /** Favori/zil değişince artar; ekranlar buna göre yeniden çizilir. */
    val rev = MutableStateFlow(0)
    /** Bildirim veya widget'tan açılması istenen maç (+ sekme). */
    val openReq = MutableStateFlow<Pair<Match, Int>?>(null)

    private fun changed() { rev.value++; GoalService.sync(getApplication()); WidgetUpdater.updateAll(getApplication()) }

    fun starMatch(m: Match) { store.setFavMatch(m.id, !store.favMatch(m.id)); changed() }
    fun bellMatch(m: Match) { store.setMatchBell(m.id, !store.bellOn(m)); changed() }
    fun starLeague(slug: String) { store.setFavLeague(slug, !store.favLeague(slug)); changed() }
    fun bellLeague(slug: String) { store.setLeagueBell(slug, !store.leagueBell(slug)); changed() }
    fun setFlag(key: String, v: Boolean) { store.setFlag(key, v); rev.value++ }
    fun setRemind(min: Int) { store.remindMin = min; rev.value++ }

    fun open(league: String?, id: String?, tab: Int) {
        if (league == null || id == null) return
        viewModelScope.launch {
            val l = com.fatih.skor.data.leagueOf(league)
            val m = Espn.matchById(l, id) ?: matches.value.firstOrNull { it.id == id } ?: return@launch
            openReq.value = m to tab
        }
    }

    init {
        refresh(false)
        loadStandings()
        loadFavMatches()
        // Canlı maç varken (veya bugüne bakarken) arka planda yavaşça yenile
        viewModelScope.launch {
            while (true) {
                delay(45_000)
                refresh(true)
            }
        }
    }


    fun refresh(silent: Boolean) {
        job?.cancel()
        job = viewModelScope.launch {
            if (!silent) loading.value = true
            val today = LocalDate.now()
            val r = Espn.range(today.minusDays(2), today.plusDays(6))
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
        changed()
    }

    fun setAlert(mode: Int) {
        store.alertMode = mode
        alertMode.value = mode
        changed()
    }

    fun setWidgetLeague(slug: String) {
        store.widgetLeague = slug
        widgetLeague.value = slug
        WidgetUpdater.updateAll(getApplication())
    }

    suspend fun detail(m: Match): Detail? = Espn.detail(m.league, m.id, m)
}
