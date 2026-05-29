package dev.canem.bluecheckers.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.canem.bluecheckers.R
import dev.canem.bluecheckers.data.repository.ProgressSummary

@Composable
fun HomeScreen(
    onStartGame: (Int) -> Unit,
    onOpenRules: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onStart2Player: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onStartGame(state.summary.unlockedLevel) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.home_start_level, state.summary.unlockedLevel))
        }
        if (state.summary.totalGames > 0) {
            Text(
                text = stringResource(
                    R.string.home_progress_at_unlocked,
                    state.summary.winsAtUnlocked,
                    state.summary.winsToUnlockNext,
                    state.summary.unlockedLevel,
                ),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.summary.lastPlayedLevel?.let { last ->
            OutlinedButton(
                onClick = { onStartGame(last) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.home_play_again, last)) }
        }

        OutlinedButton(
            onClick = onStart2Player,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.home_two_player)) }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = statsLabel(state.summary),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onOpenRules) { Text(stringResource(R.string.home_rules)) }
            TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.home_settings)) }
            TextButton(onClick = { showResetDialog = true }) {
                Text(stringResource(R.string.home_reset_progress))
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_dialog_title)) },
            text = { Text(stringResource(R.string.reset_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAllProgress()
                    showResetDialog = false
                }) { Text(stringResource(R.string.reset_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun statsLabel(summary: ProgressSummary): String {
    if (summary.totalGames == 0) return stringResource(R.string.home_no_games)
    return summary.highestBeatenLevel?.let { highest ->
        stringResource(
            R.string.home_stats_with_highest,
            summary.totalGames,
            summary.totalWins,
            summary.totalLosses,
            summary.totalDraws,
            highest,
        )
    } ?: stringResource(
        R.string.home_stats,
        summary.totalGames,
        summary.totalWins,
        summary.totalLosses,
        summary.totalDraws,
    )
}
