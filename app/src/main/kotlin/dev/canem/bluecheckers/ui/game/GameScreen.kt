package dev.canem.bluecheckers.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.canem.bluecheckers.R
import dev.canem.bluecheckers.game.DrawReason
import dev.canem.bluecheckers.game.GameOutcome
import dev.canem.bluecheckers.game.PieceColor

@Composable
fun GameScreen(
    level: Int,
    onExit: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusBar(state)
        BoardCanvas(
            board = state.board,
            selected = state.selectedSquare,
            destinations = state.destinations.keys,
            lastMove = state.lastMove,
            highlightCaptures = state.anyCaptureAvailable && state.showCaptureHints,
            animatingMove = state.animatingMove,
            onSquareTap = viewModel::onSquareTap,
            onAnimationDone = viewModel::onAnimationDone,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onExit, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.game_home))
            }
            OutlinedButton(onClick = viewModel::restart, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.game_restart))
            }
            if (state.canUndo) {
                OutlinedButton(
                    onClick = viewModel::undo,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.game_undo)) }
            }
            OutlinedButton(
                onClick = viewModel::resign,
                modifier = Modifier.weight(1f),
                enabled = state.outcome == GameOutcome.Ongoing,
            ) { Text(stringResource(R.string.game_resign)) }
        }
    }

    if (state.outcome != GameOutcome.Ongoing) {
        OutcomeDialog(
            outcome = state.outcome,
            playerColor = state.playerColor,
            level = state.level,
            twoPlayerMode = state.twoPlayerMode,
            onPlayAgain = viewModel::restart,
            onHome = onExit,
        )
    }
}

@Composable
private fun StatusBar(state: GameUiState) {
    val outcome = state.outcome
    val text = when {
        outcome is GameOutcome.Win -> when {
            state.twoPlayerMode -> when (outcome.winner) {
                PieceColor.WHITE -> stringResource(R.string.game_dialog_2p_win_title_white)
                PieceColor.BLACK -> stringResource(R.string.game_dialog_2p_win_title_black)
            }
            outcome.winner == state.playerColor -> stringResource(R.string.game_you_win)
            else -> stringResource(R.string.game_you_lose)
        }
        outcome is GameOutcome.Draw -> stringResource(R.string.game_draw)
        state.aiThinking -> stringResource(R.string.game_ai_thinking)
        state.twoPlayerMode -> {
            val base = when (state.sideToMove) {
                PieceColor.WHITE -> stringResource(R.string.game_to_move_white)
                PieceColor.BLACK -> stringResource(R.string.game_to_move_black)
            }
            base + if (state.anyCaptureAvailable) stringResource(R.string.game_capture_mandatory) else ""
        }
        state.sideToMove == state.playerColor -> {
            stringResource(R.string.game_your_move) +
                if (state.anyCaptureAvailable) stringResource(R.string.game_capture_mandatory) else ""
        }
        else -> stringResource(R.string.game_opponents_move)
    }
    val header = if (state.twoPlayerMode) stringResource(R.string.game_two_player)
    else stringResource(R.string.game_level, state.level)
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(header, style = MaterialTheme.typography.labelMedium)
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun OutcomeDialog(
    outcome: GameOutcome,
    playerColor: PieceColor,
    level: Int,
    twoPlayerMode: Boolean,
    onPlayAgain: () -> Unit,
    onHome: () -> Unit,
) {
    val title: String
    val body: String
    when (outcome) {
        is GameOutcome.Win -> {
            if (twoPlayerMode) {
                title = when (outcome.winner) {
                    PieceColor.WHITE -> stringResource(R.string.game_dialog_2p_win_title_white)
                    PieceColor.BLACK -> stringResource(R.string.game_dialog_2p_win_title_black)
                }
                body = stringResource(R.string.game_dialog_2p_body)
            } else {
                val isPlayerWin = outcome.winner == playerColor
                title = stringResource(
                    if (isPlayerWin) R.string.game_dialog_you_win_title
                    else R.string.game_dialog_you_lose_title
                )
                body = stringResource(
                    if (isPlayerWin) R.string.game_dialog_you_win_body
                    else R.string.game_dialog_you_lose_body,
                    level,
                )
            }
        }
        is GameOutcome.Draw -> {
            title = stringResource(R.string.game_dialog_draw_title)
            body = when (outcome.reason) {
                DrawReason.PLIES_WITHOUT_PROGRESS -> stringResource(R.string.game_dialog_draw_no_progress)
                DrawReason.THREEFOLD_REPETITION -> stringResource(R.string.game_dialog_draw_repetition)
                DrawReason.AGREEMENT -> stringResource(R.string.game_dialog_draw_agreement)
            }
        }
        GameOutcome.Ongoing -> return
    }
    AlertDialog(
        onDismissRequest = { /* modal */ },
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onPlayAgain) { Text(stringResource(R.string.game_play_again)) } },
        dismissButton = { TextButton(onClick = onHome) { Text(stringResource(R.string.game_home)) } },
    )
}
