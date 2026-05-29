package dev.canem.bluecheckers.game

/**
 * Legal-move generation under Brazilian rules:
 *  - Mandatory capture.
 *  - Lei da maioria (quantity only — kings count the same as men).
 *  - Men capture forward AND backward; kings fly any distance.
 *  - During a capture chain, captured pieces remain as inert blockers until the
 *    chain ends (no double-jump of the same piece; the moving piece may not pass
 *    or land on a square occupied by a not-yet-removed captured piece).
 *  - A man that touches the promotion row mid-chain does NOT promote; it must
 *    continue along man-movement rules. Promotion happens only when the chain
 *    ends with the piece stopping on the promotion row.
 */
object MoveGenerator {

    private val DIRS = listOf(-1 to -1, -1 to +1, +1 to -1, +1 to +1)

    fun legalMoves(board: Board, sideToMove: PieceColor): List<Move> {
        val captures = mutableListOf<Move>()
        for ((sq, piece) in board.piecesOf(sideToMove)) {
            if (piece.isKing) {
                findKingCaptures(board, sideToMove, sq, sq, emptyList(), listOf(sq), captures)
            } else {
                findManCaptures(board, sideToMove, sq, sq, emptyList(), listOf(sq), captures)
            }
        }
        if (captures.isNotEmpty()) {
            // Lei da maioria — choose only the longest sequences.
            val maxLen = captures.maxOf { it.captureCount }
            return captures.filter { it.captureCount == maxLen }
        }
        val quiet = mutableListOf<Move>()
        for ((sq, piece) in board.piecesOf(sideToMove)) {
            if (piece.isKing) quiet += quietMovesForKing(board, sq)
            else quiet += quietMovesForMan(board, sideToMove, sq)
        }
        return quiet
    }

    private fun isTraversable(board: Board, s: Square, originalStart: Square): Boolean {
        if (s == originalStart) return true
        return board.pieceAt(s) == null
    }

    private fun findManCaptures(
        board: Board,
        color: PieceColor,
        from: Square,
        originalStart: Square,
        captured: List<Square>,
        pathSoFar: List<Square>,
        out: MutableList<Move>,
    ) {
        var extended = false
        for ((dr, dc) in DIRS) {
            val midR = from.row + dr
            val midC = from.col + dc
            val landR = from.row + 2 * dr
            val landC = from.col + 2 * dc
            if (midR !in 0..7 || midC !in 0..7) continue
            if (landR !in 0..7 || landC !in 0..7) continue
            val mid = Square(midR, midC)
            val midPiece = board.pieceAt(mid) ?: continue
            if (midPiece.color == color) continue
            if (mid in captured) continue
            val land = Square(landR, landC)
            if (!isTraversable(board, land, originalStart)) continue
            if (land in captured) continue
            extended = true
            findManCaptures(
                board, color, land, originalStart,
                captured + mid,
                pathSoFar + land,
                out,
            )
        }
        if (!extended && captured.isNotEmpty()) {
            val end = pathSoFar.last()
            val promoted = end.row == promotionRow(color)
            out += Move(
                from = pathSoFar.first(),
                to = end,
                path = pathSoFar,
                captured = captured,
                promoted = promoted,
            )
        }
    }

    private fun findKingCaptures(
        board: Board,
        color: PieceColor,
        from: Square,
        originalStart: Square,
        captured: List<Square>,
        pathSoFar: List<Square>,
        out: MutableList<Move>,
    ) {
        var extended = false
        for ((dr, dc) in DIRS) {
            // Walk diagonal looking for the first non-traversable square.
            var r = from.row + dr
            var c = from.col + dc
            var hit: Square? = null
            while (r in 0..7 && c in 0..7) {
                val s = Square(r, c)
                if (s !in captured && isTraversable(board, s, originalStart)) {
                    r += dr; c += dc
                    continue
                }
                hit = s
                break
            }
            if (hit == null) continue
            // The hit square holds a piece (captured blocker, friendly, or capturable opponent).
            val hitPiece = board.pieceAt(hit) ?: continue
            if (hitPiece.color == color) continue
            if (hit in captured) continue
            // Capturable opponent — try every landing square beyond.
            var landR = hit.row + dr
            var landC = hit.col + dc
            while (landR in 0..7 && landC in 0..7) {
                val land = Square(landR, landC)
                if (land in captured) break
                if (!isTraversable(board, land, originalStart)) break
                extended = true
                findKingCaptures(
                    board, color, land, originalStart,
                    captured + hit,
                    pathSoFar + land,
                    out,
                )
                landR += dr; landC += dc
            }
        }
        if (!extended && captured.isNotEmpty()) {
            val end = pathSoFar.last()
            out += Move(
                from = pathSoFar.first(),
                to = end,
                path = pathSoFar,
                captured = captured,
                promoted = false,
            )
        }
    }

    private fun quietMovesForMan(board: Board, color: PieceColor, from: Square): List<Move> {
        val out = mutableListOf<Move>()
        val dr = color.forwardRowDelta()
        for (dc in listOf(-1, +1)) {
            val land = (from + (dr to dc)) ?: continue
            if (!board.isEmpty(land)) continue
            val promoted = land.row == promotionRow(color)
            out += Move(
                from = from, to = land,
                path = listOf(from, land),
                captured = emptyList(),
                promoted = promoted,
            )
        }
        return out
    }

    private fun quietMovesForKing(board: Board, from: Square): List<Move> {
        val out = mutableListOf<Move>()
        for ((dr, dc) in DIRS) {
            var r = from.row + dr
            var c = from.col + dc
            while (r in 0..7 && c in 0..7) {
                val land = Square(r, c)
                if (!board.isEmpty(land)) break
                out += Move(
                    from = from, to = land,
                    path = listOf(from, land),
                    captured = emptyList(),
                    promoted = false,
                )
                r += dr; c += dc
            }
        }
        return out
    }
}
