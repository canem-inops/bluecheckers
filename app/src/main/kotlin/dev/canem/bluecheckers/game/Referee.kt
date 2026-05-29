package dev.canem.bluecheckers.game

/**
 * Stateless judge that, given a state and its legal moves, returns the outcome.
 * Caller should compute legal moves once and pass them in to avoid duplicate work.
 */
object Referee {

    fun outcome(state: GameState, legalMoves: List<Move>): GameOutcome {
        // No pieces for side-to-move => opponent already captured them all.
        if (state.board.count(state.sideToMove) == 0) {
            return GameOutcome.Win(state.sideToMove.opponent, WinReason.OPPONENT_HAS_NO_PIECES)
        }
        // No legal moves but pieces present => stalemate-as-loss for side-to-move.
        if (legalMoves.isEmpty()) {
            return GameOutcome.Win(state.sideToMove.opponent, WinReason.OPPONENT_HAS_NO_MOVES)
        }
        // Draws.
        if (state.pliesSinceProgress >= state.rules.drawPliesWithoutProgress) {
            return GameOutcome.Draw(DrawReason.PLIES_WITHOUT_PROGRESS)
        }
        if (state.rules.drawOnThreefoldRepetition) {
            val currentKey = state.positionKey()
            val occurrences = state.history.count { it == currentKey }
            if (occurrences >= 3) {
                return GameOutcome.Draw(DrawReason.THREEFOLD_REPETITION)
            }
        }
        return GameOutcome.Ongoing
    }

    fun outcome(state: GameState): GameOutcome =
        outcome(state, MoveGenerator.legalMoves(state.board, state.sideToMove))
}
