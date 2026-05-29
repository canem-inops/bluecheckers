package dev.canem.bluecheckers.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.canem.bluecheckers.R

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToggleRow(
                label = stringResource(R.string.settings_animations),
                description = stringResource(R.string.settings_animations_desc),
                checked = prefs.animationsEnabled,
                onChange = viewModel::setAnimations,
            )
            ToggleRow(
                label = stringResource(R.string.settings_hints),
                description = stringResource(R.string.settings_hints_desc),
                checked = prefs.showCaptureHints,
                onChange = viewModel::setHints,
            )
            ToggleRow(
                label = stringResource(R.string.settings_undo),
                description = stringResource(R.string.settings_undo_desc),
                checked = prefs.undoEnabled,
                onChange = viewModel::setUndo,
            )
            ToggleRow(
                label = stringResource(R.string.settings_tournament),
                description = stringResource(R.string.settings_tournament_desc),
                checked = prefs.tournamentRules,
                onChange = viewModel::setTournament,
            )
            ToggleRow(
                label = stringResource(R.string.settings_sound),
                description = stringResource(R.string.settings_sound_desc),
                checked = prefs.soundEnabled,
                onChange = viewModel::setSound,
            )
            ToggleRow(
                label = stringResource(R.string.settings_self_play),
                description = stringResource(R.string.settings_self_play_desc),
                checked = prefs.selfPlayEnabled,
                onChange = viewModel::setSelfPlay,
            )
        }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
