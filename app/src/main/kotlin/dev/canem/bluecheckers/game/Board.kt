package dev.canem.bluecheckers.game

/**
 * Immutable 8x8 board. Pieces only live on dark squares; light squares are always null.
 * Equality is structural over the 64 cells so the board doubles as a position key for
 * repetition detection.
 */
data class Board(val cells: List<Piece?>) {

    init {
        require(cells.size == 64) { "board must have 64 cells, was ${cells.size}" }
    }

    fun pieceAt(sq: Square): Piece? = cells[sq.index]
    fun pieceAt(row: Int, col: Int): Piece? = cells[row * 8 + col]

    fun isEmpty(sq: Square): Boolean = cells[sq.index] == null

    fun set(updates: Map<Square, Piece?>): Board {
        if (updates.isEmpty()) return this
        val mutable = cells.toMutableList()
        for ((sq, p) in updates) mutable[sq.index] = p
        return Board(mutable)
    }

    fun piecesOf(color: PieceColor): Sequence<Pair<Square, Piece>> = sequence {
        for (i in 0 until 64) {
            val p = cells[i] ?: continue
            if (p.color == color) yield(Square.of(i) to p)
        }
    }

    fun count(color: PieceColor): Int {
        var n = 0
        for (i in 0 until 64) {
            val p = cells[i] ?: continue
            if (p.color == color) n++
        }
        return n
    }

    fun countKings(color: PieceColor): Int {
        var n = 0
        for (i in 0 until 64) {
            val p = cells[i] ?: continue
            if (p.color == color && p.isKing) n++
        }
        return n
    }

    fun countMen(color: PieceColor): Int = count(color) - countKings(color)

    companion object {
        fun initial(): Board {
            val list = MutableList<Piece?>(64) { null }
            for (row in 0..2) {
                for (col in 0..7) {
                    if ((row + col) and 1 == 0) list[row * 8 + col] = Piece(PieceColor.WHITE)
                }
            }
            for (row in 5..7) {
                for (col in 0..7) {
                    if ((row + col) and 1 == 0) list[row * 8 + col] = Piece(PieceColor.BLACK)
                }
            }
            return Board(list)
        }

        /**
         * Construct a board from a multiline grid string. Useful for tests.
         * Row 7 is the first line, row 0 is the last (so it reads like a board from black's view).
         * Chars: '.' light/empty (light squares only), '_' empty dark square,
         * 'w' white man, 'W' white king, 'b' black man, 'B' black king.
         */
        fun fromGrid(grid: String): Board {
            val rows = grid.trim().lines().map { it.trim() }
            require(rows.size == 8) { "grid must have 8 rows, got ${rows.size}" }
            val list = MutableList<Piece?>(64) { null }
            for ((i, line) in rows.withIndex()) {
                val row = 7 - i
                require(line.length == 8) { "row $row has ${line.length} chars, need 8" }
                for (col in 0..7) {
                    val c = line[col]
                    val piece = when (c) {
                        '.', '_' -> null
                        'w' -> Piece(PieceColor.WHITE)
                        'W' -> Piece(PieceColor.WHITE, isKing = true)
                        'b' -> Piece(PieceColor.BLACK)
                        'B' -> Piece(PieceColor.BLACK, isKing = true)
                        else -> error("unknown char '$c' at row $row col $col")
                    }
                    list[row * 8 + col] = piece
                }
            }
            return Board(list)
        }
    }

    /** Render for debugging in the same format `fromGrid` consumes. */
    fun render(): String = buildString {
        for (i in 0..7) {
            val row = 7 - i
            for (col in 0..7) {
                val isDark = (row + col) and 1 == 0
                val p = cells[row * 8 + col]
                append(
                    when {
                        !isDark -> '.'
                        p == null -> '_'
                        p.color == PieceColor.WHITE && !p.isKing -> 'w'
                        p.color == PieceColor.WHITE && p.isKing -> 'W'
                        p.color == PieceColor.BLACK && !p.isKing -> 'b'
                        else -> 'B'
                    }
                )
            }
            append('\n')
        }
    }
}
