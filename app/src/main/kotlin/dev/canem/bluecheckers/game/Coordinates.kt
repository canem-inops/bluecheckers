package dev.canem.bluecheckers.game

/**
 * A square on the 8x8 board. Rows are 0 (white's home rank) to 7 (black's home rank);
 * columns are 0 (left from white's view) to 7 (right). Play occurs only on dark squares,
 * which are those where (row + col) is even — this puts a dark square in each player's
 * bottom-left corner, satisfying the Brazilian-rules "long dark diagonal on each player's
 * left" requirement.
 */
data class Square(val row: Int, val col: Int) {
    init {
        require(row in 0..7 && col in 0..7) { "out of board: ($row, $col)" }
    }

    val isDark: Boolean get() = (row + col) and 1 == 0
    val index: Int get() = row * 8 + col

    operator fun plus(delta: Pair<Int, Int>): Square? {
        val r = row + delta.first
        val c = col + delta.second
        return if (r in 0..7 && c in 0..7) Square(r, c) else null
    }

    override fun toString(): String = "(${row},${col})"

    companion object {
        fun of(index: Int): Square = Square(index / 8, index % 8)
    }
}

enum class PieceColor {
    WHITE, BLACK;

    val opponent: PieceColor get() = if (this == WHITE) BLACK else WHITE
}

/**
 * Diagonal-row delta a regular man (pedra) of this color uses for non-capture moves.
 * Captures may go in either direction regardless.
 */
fun PieceColor.forwardRowDelta(): Int = if (this == PieceColor.WHITE) +1 else -1

data class Piece(val color: PieceColor, val isKing: Boolean = false) {
    fun crowned(): Piece = if (isKing) this else Piece(color, isKing = true)
}

/** The row on which a man of [color] promotes when it stops there. */
fun promotionRow(color: PieceColor): Int = if (color == PieceColor.WHITE) 7 else 0
