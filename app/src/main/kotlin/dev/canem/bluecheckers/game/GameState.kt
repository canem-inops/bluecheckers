package dev.canem.bluecheckers.game

/**
 * Position key used for repetition detection. Side-to-move is part of the key:
 * the same board with different sides to move is a different position.
 */
data class PositionKey(val cells: List<Piece?>, val sideToMove: PieceColor)

/**
 * Full game state. Immutable — every move produces a new instance.
 *
 * [pliesSinceProgress] counts plies since the last capture or man move.
 * Used together with [RulesConfig.drawPliesWithoutProgress] for the CBD draw rule.
 *
 * [history] holds the position after every ply (and the initial position) for
 * three-fold-repetition detection.
 */
data class GameState(
    val board: Board,
    val sideToMove: PieceColor,
    val plyCount: Int,
    val pliesSinceProgress: Int,
    val history: List<PositionKey>,
    val rules: RulesConfig = RulesConfig.DEFAULT,
) {

    fun positionKey(): PositionKey = PositionKey(board.cells, sideToMove)

    companion object {
        fun newGame(rules: RulesConfig = RulesConfig.DEFAULT): GameState {
            val board = Board.initial()
            val side = PieceColor.WHITE
            return GameState(
                board = board,
                sideToMove = side,
                plyCount = 0,
                pliesSinceProgress = 0,
                history = listOf(PositionKey(board.cells, side)),
                rules = rules,
            )
        }
    }
}

/**
 * Apply [move] to [state] producing the next state. Caller must have obtained
 * [move] from `MoveGenerator.legalMoves` — applying an unverified move is undefined.
 */
fun applyMove(state: GameState, move: Move): GameState {
    val board = state.board
    val piece = board.pieceAt(move.from)
        ?: error("no piece at ${move.from} to move")
    require(piece.color == state.sideToMove) { "piece at ${move.from} is not the side to move" }

    val finalPiece = if (move.promoted) piece.crowned() else piece
    val updates = HashMap<Square, Piece?>(move.captured.size + 2)
    updates[move.from] = null
    for (sq in move.captured) updates[sq] = null
    updates[move.to] = finalPiece
    val newBoard = board.set(updates)
    val newSide = state.sideToMove.opponent

    // Reset the "no progress" counter on any capture or any move of a man.
    // (A man move that also promotes still counts as a man move.)
    val resetCounter = move.isCapture || !piece.isKing
    val nextPliesNoProgress = if (resetCounter) 0 else state.pliesSinceProgress + 1

    val newKey = PositionKey(newBoard.cells, newSide)
    val newHistory = state.history + newKey
    return state.copy(
        board = newBoard,
        sideToMove = newSide,
        plyCount = state.plyCount + 1,
        pliesSinceProgress = nextPliesNoProgress,
        history = newHistory,
    )
}
