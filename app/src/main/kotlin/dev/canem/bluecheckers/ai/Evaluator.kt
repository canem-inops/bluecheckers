package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.PieceColor

/**
 * Static position evaluator. Returns a score for [sideToMove] — positive = good
 * for the side to move, negative = bad. The returned magnitude is bounded by
 * [TERMINAL_SCORE] minus the search depth so terminal scores always beat heuristic ones.
 */
class Evaluator(val weights: Weights) {

    fun evaluate(board: Board, sideToMove: PieceColor): Double {
        val features = Features.compute(board)
        val whiteScore = weights.evaluate(features)
        return if (sideToMove == PieceColor.WHITE) whiteScore else -whiteScore
    }

    companion object {
        const val TERMINAL_SCORE = 1_000_000.0
    }
}
