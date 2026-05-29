package dev.canem.bluecheckers.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canem.bluecheckers.data.repository.AiRepository
import dev.canem.bluecheckers.data.repository.ProgressRepository
import dev.canem.bluecheckers.data.repository.ProgressSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(val summary: ProgressSummary = ProgressSummary.EMPTY)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val progressRepository: ProgressRepository,
    private val aiRepository: AiRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = progressRepository.observeSummary()
        .map { HomeUiState(summary = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeUiState(),
        )

    fun resetAllProgress() {
        viewModelScope.launch {
            progressRepository.resetAll()
            aiRepository.resetAll()
        }
    }
}
