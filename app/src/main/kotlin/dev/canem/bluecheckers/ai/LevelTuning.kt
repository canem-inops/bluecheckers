package dev.canem.bluecheckers.ai

/**
 * Parameters used to construct the AI agent for a given level.
 *
 * Levels are unbounded (1, 2, 3, ... forever) so the player can always be
 * unlocking the next one. The (depth, ε, time-budget) tuning scales up to
 * level [DEPTH_CAP_LEVEL] and then **saturates** — every level past the cap
 * is mechanically identical at the search level. The AI's playing strength
 * past the cap improves only through cumulative training of the shared
 * weight snapshot.
 *
 * This is a deliberate design choice. A linear evaluator over a handful of
 * features has a model-class ceiling no amount of training breaks, and
 * alpha-beta search on a phone hits a practical ceiling around depth 10.
 * Past the cap the level number is best read as "games of perseverance".
 */
data class LevelTuning(
    val level: Int,
    val maxDepth: Int,
    val epsilon: Double,
    val timeBudgetMillis: Long,
) {

    val isRandom: Boolean get() = level == 0
    val isSaturated: Boolean get() = level >= DEPTH_CAP_LEVEL

    companion object {
        /** Past this level the search tuning stays constant. */
        const val DEPTH_CAP_LEVEL = 12

        fun forLevel(level: Int): LevelTuning {
            require(level >= 0) { "level must be >= 0, was $level" }
            return when (level) {
                0 -> LevelTuning(0, maxDepth = 1, epsilon = 1.00, timeBudgetMillis = 100L)
                1 -> LevelTuning(1, maxDepth = 2, epsilon = 0.35, timeBudgetMillis = 200L)
                2 -> LevelTuning(2, maxDepth = 3, epsilon = 0.20, timeBudgetMillis = 350L)
                3 -> LevelTuning(3, maxDepth = 3, epsilon = 0.10, timeBudgetMillis = 500L)
                4 -> LevelTuning(4, maxDepth = 4, epsilon = 0.05, timeBudgetMillis = 700L)
                5 -> LevelTuning(5, maxDepth = 5, epsilon = 0.02, timeBudgetMillis = 900L)
                6 -> LevelTuning(6, maxDepth = 6, epsilon = 0.00, timeBudgetMillis = 1_200L)
                7 -> LevelTuning(7, maxDepth = 7, epsilon = 0.00, timeBudgetMillis = 1_500L)
                8 -> LevelTuning(8, maxDepth = 8, epsilon = 0.00, timeBudgetMillis = 2_000L)
                9 -> LevelTuning(9, maxDepth = 9, epsilon = 0.00, timeBudgetMillis = 2_500L)
                10 -> LevelTuning(10, maxDepth = 9, epsilon = 0.00, timeBudgetMillis = 2_800L)
                11 -> LevelTuning(11, maxDepth = 10, epsilon = 0.00, timeBudgetMillis = 3_000L)
                else -> LevelTuning(level, maxDepth = 10, epsilon = 0.00, timeBudgetMillis = 3_000L)
            }
        }
    }
}

/**
 * Build the agent that plays at a given level using the supplied weights.
 * Level 0 ignores weights entirely (it's a pure random agent).
 */
fun agentForLevel(level: Int, weights: Weights = Weights.DEFAULT): Agent {
    val tuning = LevelTuning.forLevel(level)
    if (tuning.isRandom) return RandomAgent()
    return AlphaBetaAgent(
        maxDepth = tuning.maxDepth,
        evaluator = Evaluator(weights),
        epsilon = tuning.epsilon,
        timeBudgetMillis = tuning.timeBudgetMillis,
    )
}
