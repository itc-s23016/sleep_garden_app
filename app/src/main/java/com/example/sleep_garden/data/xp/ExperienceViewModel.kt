package com.example.sleep_garden.data.xp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleep_garden.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExperienceViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ExperienceRepository(AppDatabase.get(app))

    val sessions = repo.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary = repo.observeSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * effectiveDuration = SleepScreen で半減された値
     */
    fun onWakeConfirm(
        sleepAtMillis: Long,
        wakeAtMillis: Long,
        effectiveDuration: Int,
        note: String? = null,
        onResult: (XpResult) -> Unit,
        onError: (Throwable) -> Unit = {}
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            val result = repo.recordSleep(
                sleepAtMillis = sleepAtMillis,
                wakeAtMillis = wakeAtMillis,
                effectiveDurationMin = effectiveDuration,
                note = note
            )
            withContext(Dispatchers.Main) { onResult(result) }
        } catch (t: Throwable) {
            withContext(Dispatchers.Main) { onError(t) }
        }
    }
}
