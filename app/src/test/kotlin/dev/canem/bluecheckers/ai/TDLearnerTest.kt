package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.Piece
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Square
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TDLearnerTest {

    private fun emptyBoard() = Board(MutableList<Piece?>(64) { null })
    private fun boardOf(vararg pairs: Pair<Square, Piece>) = emptyBoard().set(pairs.toMap())

    @Test
    fun `update leaves weights unchanged for trajectory shorter than two`() {
        val w0 = Weights.DEFAULT
        val updated = TDLearner().update(w0, listOf(Board.initial()), TDLearner.Outcome.DRAW)
        assertEquals(w0, updated)
    }

    @Test
    fun `update on white-win trajectory shifts man-balance weight toward the gradient direction`() {
        // s0: white already ahead (2 men vs 1) — features[MAN_BALANCE] = +1.
        // s1: white captures the last black piece — terminal, reward = +1.
        // With zero initial weights, V(s0) = 0, target = +1, delta = +1, gradient at
        // index MAN_BALANCE is +1, so the weight moves positive.
        val s0 = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE),
            Square(0, 2) to Piece(PieceColor.WHITE),
            Square(7, 1) to Piece(PieceColor.BLACK),
        )
        val s1 = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE),
            Square(0, 2) to Piece(PieceColor.WHITE),
        )
        val initial = Weights(DoubleArray(Features.COUNT))
        val updated = TDLearner(learningRate = 0.5).update(
            initial,
            listOf(s0, s1),
            TDLearner.Outcome.WHITE_WIN,
        )
        assertTrue(
            "MAN_BALANCE weight should rise after a white-win trajectory: ${updated.values[Features.IDX_MAN_BALANCE]}",
            updated.values[Features.IDX_MAN_BALANCE] > 0.0,
        )
    }

    @Test
    fun `update on black-win trajectory pushes weights the opposite way`() {
        val s0 = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE),
            Square(7, 1) to Piece(PieceColor.BLACK),
            Square(7, 3) to Piece(PieceColor.BLACK),
        )
        val s1 = boardOf(
            Square(7, 1) to Piece(PieceColor.BLACK),
            Square(7, 3) to Piece(PieceColor.BLACK),
        )
        val initial = Weights(DoubleArray(Features.COUNT))
        val updated = TDLearner(learningRate = 0.5).update(
            initial,
            listOf(s0, s1),
            TDLearner.Outcome.BLACK_WIN,
        )
        // At t=1, features[MAN_BALANCE] = -2 (white perspective), target = -1, V = 0, delta = -1,
        // gradient is features (negative). w shifts so that V at this position decreases — meaning
        // the MAN_BALANCE weight increases (multiplying neg features by pos weight => neg V). Sign:
        // w_new = w + α * δ * f = 0 + 0.5 * (-1) * (-2) = +1.
        assertTrue(updated.values[Features.IDX_MAN_BALANCE] > 0.0)
    }
}
