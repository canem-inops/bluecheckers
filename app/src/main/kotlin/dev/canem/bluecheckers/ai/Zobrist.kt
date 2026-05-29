package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.PieceColor
import kotlin.random.Random

/**
 * 64-bit Zobrist hash of (board, side-to-move). Each (square, piece-type) is
 * assigned a fixed random long at startup; the hash of a board is the XOR of
 * the entries for every occupied square plus a side-to-move term.
 *
 * Random numbers are seeded deterministically so two app instances produce the
 * same hashes — handy if we ever want to persist a transposition table.
 */
object Zobrist {

    private const val WHITE_MAN = 0
    private const val WHITE_KING = 1
    private const val BLACK_MAN = 2
    private const val BLACK_KING = 3
    private const val PIECE_TYPES = 4

    private val pieceSquareKeys: LongArray = LongArray(PIECE_TYPES * 64).also { arr ->
        val r = Random(0xB10ECECCEA5L)
        for (i in arr.indices) arr[i] = r.nextLong()
    }

    /** Single key XORed in when it is black's turn. */
    private val blackToMoveKey: Long = Random(0xC0FFEE1234L).nextLong()

    fun hash(board: Board, sideToMove: PieceColor): Long {
        var h = 0L
        val cells = board.cells
        for (i in 0 until 64) {
            val p = cells[i] ?: continue
            val pt = when {
                p.color == PieceColor.WHITE && !p.isKing -> WHITE_MAN
                p.color == PieceColor.WHITE && p.isKing -> WHITE_KING
                p.color == PieceColor.BLACK && !p.isKing -> BLACK_MAN
                else -> BLACK_KING
            }
            h = h xor pieceSquareKeys[pt * 64 + i]
        }
        if (sideToMove == PieceColor.BLACK) h = h xor blackToMoveKey
        return h
    }
}
