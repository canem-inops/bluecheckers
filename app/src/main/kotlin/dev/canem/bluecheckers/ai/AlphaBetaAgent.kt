package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.GameOutcome
import dev.canem.bluecheckers.game.GameState
import dev.canem.bluecheckers.game.Move
import dev.canem.bluecheckers.game.MoveGenerator
import dev.canem.bluecheckers.game.Referee
import dev.canem.bluecheckers.game.applyMove
import kotlin.random.Random

/**
 * Negamax alpha-beta with iterative deepening, a wall-clock budget, and a
 * configurable ε-greedy exploration knob. Captures are tried first to maximize
 * cutoffs. A bounded transposition table caches (zobrist, depth) → score with
 * EXACT/LOWER/UPPER bound information, which both lets us skip already-searched
 * sub-trees and improves move ordering at deeper iterations.
 *
 * The TT is allocated per call to [chooseMove]. A per-game shared TT would be
 * faster but adds shared mutable state; the per-call cost is small at the
 * depths we run at.
 */
class AlphaBetaAgent(
    val maxDepth: Int,
    val evaluator: Evaluator,
    val epsilon: Double = 0.0,
    val timeBudgetMillis: Long = 1_000L,
    val ttCapacity: Int = 1 shl 16,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : Agent {

    private enum class Bound { EXACT, LOWER, UPPER }

    private class TTEntry(
        var key: Long = 0L,
        var depth: Int = -1,
        var score: Double = 0.0,
        var bound: Bound = Bound.EXACT,
    )

    private class TT(capacity: Int) {
        private val mask: Int = (Integer.highestOneBit(capacity).coerceAtLeast(2)) - 1
        private val table: Array<TTEntry?> = arrayOfNulls(mask + 1)

        fun probe(key: Long): TTEntry? {
            val e = table[(key.toInt() and mask)] ?: return null
            return if (e.key == key) e else null
        }

        fun store(key: Long, depth: Int, score: Double, bound: Bound) {
            val idx = key.toInt() and mask
            val existing = table[idx]
            // Replacement policy: prefer deeper or new entries.
            if (existing == null) {
                table[idx] = TTEntry(key, depth, score, bound)
            } else if (depth >= existing.depth || existing.key != key) {
                existing.key = key
                existing.depth = depth
                existing.score = score
                existing.bound = bound
            }
        }
    }

    override fun chooseMove(state: GameState, legalMoves: List<Move>, random: Random): Move {
        require(legalMoves.isNotEmpty()) { "no legal moves" }
        if (legalMoves.size == 1) return legalMoves.single()
        if (epsilon > 0.0 && random.nextDouble() < epsilon) {
            return legalMoves.random(random)
        }

        val deadline = nowMillis() + timeBudgetMillis
        val tt = TT(ttCapacity)
        var best = legalMoves.first()
        for (depth in 1..maxDepth) {
            val result = searchRoot(state, legalMoves, depth, deadline, tt)
            if (result.aborted) break
            best = result.bestMove
        }
        return best
    }

    private data class RootResult(val bestMove: Move, val aborted: Boolean)

    private fun searchRoot(
        state: GameState,
        moves: List<Move>,
        depth: Int,
        deadline: Long,
        tt: TT,
    ): RootResult {
        var alpha = -Evaluator.TERMINAL_SCORE * 2
        val beta = Evaluator.TERMINAL_SCORE * 2
        var bestMove = moves.first()
        var bestScore = -Double.MAX_VALUE
        for (move in orderMoves(moves)) {
            if (nowMillis() >= deadline) return RootResult(bestMove, aborted = true)
            val next = applyMove(state, move)
            val score = -negamax(next, depth - 1, -beta, -alpha, deadline, tt)
            if (score > bestScore) {
                bestScore = score
                bestMove = move
                if (score > alpha) alpha = score
            }
        }
        return RootResult(bestMove, aborted = false)
    }

    private fun negamax(
        state: GameState,
        depth: Int,
        alphaIn: Double,
        beta: Double,
        deadline: Long,
        tt: TT,
    ): Double {
        val key = Zobrist.hash(state.board, state.sideToMove)
        val cached = tt.probe(key)
        if (cached != null && cached.depth >= depth) {
            when (cached.bound) {
                Bound.EXACT -> return cached.score
                Bound.LOWER -> if (cached.score >= beta) return cached.score
                Bound.UPPER -> if (cached.score <= alphaIn) return cached.score
            }
        }

        val moves = MoveGenerator.legalMoves(state.board, state.sideToMove)
        val outcome = Referee.outcome(state, moves)
        when (outcome) {
            is GameOutcome.Win -> {
                val loserToMove = outcome.winner != state.sideToMove
                val terminal = if (loserToMove) -(Evaluator.TERMINAL_SCORE - depth)
                else (Evaluator.TERMINAL_SCORE - depth)
                tt.store(key, depth, terminal, Bound.EXACT)
                return terminal
            }
            is GameOutcome.Draw -> {
                tt.store(key, depth, 0.0, Bound.EXACT)
                return 0.0
            }
            GameOutcome.Ongoing -> Unit
        }
        if (depth <= 0) {
            val eval = evaluator.evaluate(state.board, state.sideToMove)
            tt.store(key, depth, eval, Bound.EXACT)
            return eval
        }
        if (nowMillis() >= deadline) {
            return evaluator.evaluate(state.board, state.sideToMove)
        }

        var alpha = alphaIn
        var best = -Double.MAX_VALUE
        for (move in orderMoves(moves)) {
            val next = applyMove(state, move)
            val score = -negamax(next, depth - 1, -beta, -alpha, deadline, tt)
            if (score > best) best = score
            if (best > alpha) alpha = best
            if (alpha >= beta) break
        }
        val bound = when {
            best <= alphaIn -> Bound.UPPER
            best >= beta -> Bound.LOWER
            else -> Bound.EXACT
        }
        tt.store(key, depth, best, bound)
        return best
    }

    private fun orderMoves(moves: List<Move>): List<Move> =
        moves.sortedWith(
            compareByDescending<Move> { it.isCapture }
                .thenByDescending { it.captureCount }
        )
}
