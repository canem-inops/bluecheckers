package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.GameState
import dev.canem.bluecheckers.game.Move
import kotlin.random.Random

/** Picks a uniformly random legal move. Level 0 baseline. */
class RandomAgent : Agent {
    override fun chooseMove(state: GameState, legalMoves: List<Move>, random: Random): Move {
        require(legalMoves.isNotEmpty()) { "no legal moves to choose from" }
        return legalMoves.random(random)
    }
}
