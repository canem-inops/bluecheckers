package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.GameState
import dev.canem.bluecheckers.game.Move
import kotlin.random.Random

/**
 * Anything that can pick a move from a list of legal options.
 *
 * Implementations must be deterministic given a fixed [Random] seed.
 */
interface Agent {
    fun chooseMove(state: GameState, legalMoves: List<Move>, random: Random = Random.Default): Move
}
