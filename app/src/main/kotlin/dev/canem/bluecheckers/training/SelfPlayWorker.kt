package dev.canem.bluecheckers.training

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.canem.bluecheckers.ai.AlphaBetaAgent
import dev.canem.bluecheckers.ai.Evaluator
import dev.canem.bluecheckers.ai.TDLearner
import dev.canem.bluecheckers.ai.Weights
import dev.canem.bluecheckers.data.repository.AiRepository
import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.GameOutcome
import dev.canem.bluecheckers.game.GameState
import dev.canem.bluecheckers.game.MoveGenerator
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Referee
import dev.canem.bluecheckers.game.applyMove
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * Plays a batch of self-play games and applies a TD(0) update after each one,
 * persisting the resulting weights. Both sides use the same evaluator with the
 * current weights; a small ε keeps openings varied.
 *
 * This worker is opt-in (settings toggle) and runs under charging + idle
 * constraints so it never hurts the user's battery or interactive responsiveness.
 */
@HiltWorker
class SelfPlayWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val aiRepository: AiRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.Default) {
        var weights = aiRepository.loadWeights()
        var gamesTrained = aiRepository.loadGamesTrained()
        val learner = TDLearner()
        repeat(GAMES_PER_RUN) { i ->
            if (isStopped) return@withContext Result.success()
            val rng = Random(System.nanoTime() + i)
            val trajectory = mutableListOf<Board>()
            var state = GameState.newGame()
            trajectory += state.board
            val agent = AlphaBetaAgent(
                maxDepth = SELF_PLAY_DEPTH,
                evaluator = Evaluator(weights),
                epsilon = SELF_PLAY_EPSILON,
                timeBudgetMillis = SELF_PLAY_BUDGET_MS,
            )
            var plies = 0
            while (plies < MAX_PLIES_PER_GAME) {
                val moves = MoveGenerator.legalMoves(state.board, state.sideToMove)
                val outcome = Referee.outcome(state, moves)
                if (outcome != GameOutcome.Ongoing) break
                val mv = agent.chooseMove(state, moves, rng)
                state = applyMove(state, mv)
                trajectory += state.board
                plies++
            }
            val finalOutcome = Referee.outcome(state)
            val winner = (finalOutcome as? GameOutcome.Win)?.winner
            weights = learner.update(weights, trajectory, TDLearner.outcomeFor(winner))
            gamesTrained++
        }
        aiRepository.saveWeights(weights, gamesTrained)
        Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "blue-checkers-self-play"
        private const val GAMES_PER_RUN = 8
        private const val SELF_PLAY_DEPTH = 3
        private const val SELF_PLAY_EPSILON = 0.10
        private const val SELF_PLAY_BUDGET_MS = 300L
        private const val MAX_PLIES_PER_GAME = 250
    }
}
