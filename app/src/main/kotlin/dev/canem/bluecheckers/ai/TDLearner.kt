package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.PieceColor

/**
 * Simple TD(0) learner that updates a linear evaluator from the trajectory of one
 * completed game.
 *
 * The value function V(s) = features(s) · w is trained from a fixed (white)
 * perspective. The terminal reward is +1 for a white win, -1 for a white loss
 * and 0 for a draw. For each ply t we apply
 *
 *     δ_t = clip(target_t − V(s_t), [-1, 1])
 *     w  ← w + α · δ_t · features(s_t)
 *
 * where target_t is V(s_{t+1}) for non-terminal next states and the terminal
 * reward when s_{t+1} is the final state. Delta-clipping keeps the update
 * stable when the evaluator's raw score is far from the [-1, 1] target range.
 */
class TDLearner(
    val learningRate: Double = 1e-4,
    private val deltaClip: Double = 1.0,
) {

    enum class Outcome { WHITE_WIN, BLACK_WIN, DRAW }

    /**
     * Returns the updated weights given the [trajectory] of board positions
     * (starting from the initial position; each subsequent entry is the board
     * AFTER one ply was applied) and the final [outcome].
     */
    fun update(weights: Weights, trajectory: List<Board>, outcome: Outcome): Weights {
        if (trajectory.size < 2) return weights
        val w = weights.values.copyOf()
        val features = Array(trajectory.size) { Features.compute(trajectory[it]) }
        val values = DoubleArray(trajectory.size) { i ->
            var s = 0.0
            val f = features[i]
            for (j in f.indices) s += f[j] * w[j]
            s
        }
        val terminalReward = when (outcome) {
            Outcome.WHITE_WIN -> 1.0
            Outcome.BLACK_WIN -> -1.0
            Outcome.DRAW -> 0.0
        }
        for (t in 0 until trajectory.size - 1) {
            val target = if (t == trajectory.size - 2) terminalReward else values[t + 1]
            val delta = (target - values[t]).coerceIn(-deltaClip, deltaClip)
            val f = features[t]
            for (i in w.indices) {
                w[i] += learningRate * delta * f[i]
            }
        }
        return Weights(w)
    }

    companion object {
        fun outcomeFor(winner: PieceColor?): Outcome = when (winner) {
            PieceColor.WHITE -> Outcome.WHITE_WIN
            PieceColor.BLACK -> Outcome.BLACK_WIN
            null -> Outcome.DRAW
        }
    }
}
