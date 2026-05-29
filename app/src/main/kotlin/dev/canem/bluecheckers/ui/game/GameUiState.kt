package dev.canem.bluecheckers.ui.game

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.GameOutcome
import dev.canem.bluecheckers.game.Move
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Square

/**
 * Everything the [GameScreen] needs to render. Immutable so Compose can recompose
 * cheaply on structural equality.
 *
 * [destinations] maps each square the selected piece can move to onto the legal
 * Move that reaches it. When two legal capture chains end on the same destination
 * but capture different pieces, only one of them is kept — the player gets one of
 * the equivalent-length sequences; this is a rare edge case in practice.
 */
data class GameUiState(
    val board: Board,
    val sideToMove: PieceColor,
    val playerColor: PieceColor,
    val level: Int,
    val selectedSquare: Square? = null,
    val destinations: Map<Square, Move> = emptyMap(),
    val lastMove: Move? = null,
    val outcome: GameOutcome = GameOutcome.Ongoing,
    val aiThinking: Boolean = false,
    val anyCaptureAvailable: Boolean = false,
    /**
     * When non-null, the board shown is the pre-move state and the UI should
     * animate the move along [Move.path]. The ViewModel commits the new board
     * once the animation finishes.
     */
    val animatingMove: Move? = null,
    /** False disables move animation; the move applies instantly. */
    val animationsEnabled: Boolean = true,
    /** False suppresses the red highlight on mandatory captures. */
    val showCaptureHints: Boolean = true,
    /** True when the Undo button should be enabled and visible. */
    val canUndo: Boolean = false,
    /** True for 2-player same-device mode (no AI). */
    val twoPlayerMode: Boolean = false,
) {
    val isPlayersTurn: Boolean
        get() = outcome == GameOutcome.Ongoing && animatingMove == null && !aiThinking &&
            (twoPlayerMode || sideToMove == playerColor)

    val isAnimating: Boolean get() = animatingMove != null
}
