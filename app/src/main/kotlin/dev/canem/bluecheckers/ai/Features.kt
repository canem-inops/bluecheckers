package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Square

/**
 * Position features used by the linear evaluator. All features are computed for the
 * WHITE perspective: positive = good for white. The evaluator flips sign for black.
 *
 * The order of features must match [DEFAULT_WEIGHTS] in [Weights] and must never be
 * reordered without bumping the on-disk schema version, because trained weight
 * snapshots are persisted as DoubleArray in this order.
 */
object Features {

    const val COUNT = 7

    const val IDX_MAN_BALANCE = 0
    const val IDX_KING_BALANCE = 1
    const val IDX_ADVANCEMENT = 2
    const val IDX_CENTER = 3
    const val IDX_BACK_RANK = 4
    const val IDX_EDGE = 5
    const val IDX_TRAPPED_KING = 6

    private val CENTER_SQUARES = setOf(
        Square(3, 3), Square(3, 5), Square(4, 2), Square(4, 4),
    )

    /** Computes the feature vector from white's perspective. Length = [COUNT]. */
    fun compute(board: Board): DoubleArray {
        val f = DoubleArray(COUNT)
        var whiteMen = 0
        var blackMen = 0
        var whiteKings = 0
        var blackKings = 0
        var whiteAdvancement = 0
        var blackAdvancement = 0
        var center = 0
        var whiteBackRank = 0
        var blackBackRank = 0
        var edge = 0
        var whiteTrappedKings = 0
        var blackTrappedKings = 0

        for (i in 0 until 64) {
            val piece = board.cells[i] ?: continue
            val sq = Square.of(i)
            val side = if (piece.color == PieceColor.WHITE) +1 else -1
            if (piece.isKing) {
                if (piece.color == PieceColor.WHITE) whiteKings++ else blackKings++
                if (isTrappedKing(board, sq, piece.color)) {
                    if (piece.color == PieceColor.WHITE) whiteTrappedKings++ else blackTrappedKings++
                }
            } else {
                if (piece.color == PieceColor.WHITE) {
                    whiteMen++
                    whiteAdvancement += sq.row
                    if (sq.row == 0) whiteBackRank++
                } else {
                    blackMen++
                    blackAdvancement += (7 - sq.row)
                    if (sq.row == 7) blackBackRank++
                }
            }
            if (sq in CENTER_SQUARES) center += side
            if (sq.col == 0 || sq.col == 7) edge += side
        }

        f[IDX_MAN_BALANCE] = (whiteMen - blackMen).toDouble()
        f[IDX_KING_BALANCE] = (whiteKings - blackKings).toDouble()
        f[IDX_ADVANCEMENT] = (whiteAdvancement - blackAdvancement).toDouble()
        f[IDX_CENTER] = center.toDouble()
        f[IDX_BACK_RANK] = (whiteBackRank - blackBackRank).toDouble()
        f[IDX_EDGE] = edge.toDouble()
        f[IDX_TRAPPED_KING] = -(whiteTrappedKings - blackTrappedKings).toDouble()
        return f
    }

    private fun isTrappedKing(board: Board, sq: Square, color: PieceColor): Boolean {
        // Heuristic: a king is "trapped" if all four diagonal adjacencies are blocked
        // by friendly pieces or board edge. Cheap approximation, not the full mobility.
        var openDirs = 0
        for ((dr, dc) in listOf(-1 to -1, -1 to +1, +1 to -1, +1 to +1)) {
            val r = sq.row + dr
            val c = sq.col + dc
            if (r !in 0..7 || c !in 0..7) continue
            val neighbor = board.pieceAt(r, c)
            if (neighbor == null || neighbor.color != color) {
                openDirs++
                break
            }
        }
        return openDirs == 0
    }
}
