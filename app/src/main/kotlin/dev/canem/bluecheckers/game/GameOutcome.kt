package dev.canem.bluecheckers.game

sealed interface GameOutcome {
    data object Ongoing : GameOutcome
    data class Win(val winner: PieceColor, val reason: WinReason) : GameOutcome
    data class Draw(val reason: DrawReason) : GameOutcome
}

enum class WinReason {
    OPPONENT_HAS_NO_PIECES,
    OPPONENT_HAS_NO_MOVES,
    RESIGNATION,
}

enum class DrawReason {
    PLIES_WITHOUT_PROGRESS,
    THREEFOLD_REPETITION,
    AGREEMENT,
}
