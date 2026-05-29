package dev.canem.bluecheckers.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canem.bluecheckers.data.repository.Prefs
import dev.canem.bluecheckers.data.repository.PrefsRepository
import dev.canem.bluecheckers.training.SelfPlayScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PrefsRepository,
    private val selfPlayScheduler: SelfPlayScheduler,
) : ViewModel() {

    val state: StateFlow<Prefs> = prefs.prefsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = Prefs.DEFAULT,
    )

    fun setAnimations(v: Boolean) = viewModelScope.launch { prefs.setAnimationsEnabled(v) }
    fun setHints(v: Boolean) = viewModelScope.launch { prefs.setShowCaptureHints(v) }
    fun setUndo(v: Boolean) = viewModelScope.launch { prefs.setUndoEnabled(v) }
    fun setTournament(v: Boolean) = viewModelScope.launch { prefs.setTournamentRules(v) }
    fun setSound(v: Boolean) = viewModelScope.launch { prefs.setSoundEnabled(v) }
    fun setSelfPlay(v: Boolean) = viewModelScope.launch {
        prefs.setSelfPlayEnabled(v)
        selfPlayScheduler.setEnabled(v)
    }
}
