package dev.canem.bluecheckers.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.canem.bluecheckers.ai.Agent
import dev.canem.bluecheckers.ai.TDLearner
import dev.canem.bluecheckers.ai.Weights
import dev.canem.bluecheckers.ai.agentForLevel
import dev.canem.bluecheckers.data.repository.AiRepository
import dev.canem.bluecheckers.data.repository.PlayerResult
import dev.canem.bluecheckers.data.repository.Prefs
import dev.canem.bluecheckers.data.repository.PrefsRepository
import dev.canem.bluecheckers.data.repository.ProgressRepository
import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.GameOutcome
import dev.canem.bluecheckers.game.GameState
import dev.canem.bluecheckers.game.Move
import dev.canem.bluecheckers.game.MoveGenerator
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Referee
import dev.canem.bluecheckers.game.RulesConfig
import dev.canem.bluecheckers.game.Square
import dev.canem.bluecheckers.game.WinReason
import dev.canem.bluecheckers.game.applyMove
import dev.canem.bluecheckers.ui.navigation.Routes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

/**
 * Drives one game. Single source of truth for game state; the UI is a pure
 * function of [uiState].
 *
 * Modes:
 *  - vs AI at a given level (ARG_LEVEL >= 0). White is the human; black is the AI.
 *  - two-player same-device (ARG_LEVEL = [Routes.LEVEL_TWO_PLAYER]). Every move
 *    is by the human at the device; no AI runs.
 *
 * Moves animate: [playAndCommit] sets `animatingMove`, awaits the UI's
 * [onAnimationDone] callback, then commits the new board state.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val aiRepository: AiRepository,
    private val progressRepository: ProgressRepository,
    private val prefsRepository: PrefsRepository,
    private val soundPlayer: SoundPlayer,
) : ViewModel() {

    private val argLevel: Int = savedStateHandle.get<Int>(Routes.ARG_LEVEL) ?: 0
    private val twoPlayerMode: Boolean = argLevel < 0
    private val level: Int = if (twoPlayerMode) 0 else argLevel
    private val humanColor: PieceColor = PieceColor.WHITE
    private val random: Random = Random(System.currentTimeMillis())
    private val aiMinDelayMillis: Long = 250L

    private var rulesConfig: RulesConfig = RulesConfig.DEFAULT
    private var gameState: GameState = GameState.newGame(rulesConfig)
    private var aiAgent: Agent = agentForLevel(level)
    private var aiWeights: Weights = Weights.DEFAULT
    private var gamesTrained: Int = 0
    private val trajectory: MutableList<Board> = mutableListOf(gameState.board)
    private var resultRecorded: Boolean = false
    private val tdLearner = TDLearner()

    /** Pre-move snapshot used by [undo]; cleared after each player commit. */
    private var preUndoState: GameState? = null
    private var preUndoTrajectorySize: Int = 0

    private var pendingAnimationDone: CompletableDeferred<Unit>? = null
    private var partialChain: List<Square> = emptyList()
    private var partialChainCandidates: List<Move> = emptyList()

    private var unlockedLevel: Int = 0
    private var prefs: Prefs = Prefs.DEFAULT

    private val _uiState = MutableStateFlow(buildUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Read prefs and progress once at game start.
            prefs = prefsRepository.prefsFlow.first()
            unlockedLevel = progressRepository.observeSummary().first().unlockedLevel
            applyTournamentRulesIfNeeded()
            aiWeights = aiRepository.loadWeights()
            gamesTrained = aiRepository.loadGamesTrained()
            aiAgent = agentForLevel(level, aiWeights)
            publishUi()
            if (!twoPlayerMode && gameState.sideToMove != humanColor) runAiTurn()
        }
        // Observe prefs flow live so animation/hint toggles take effect immediately.
        viewModelScope.launch {
            prefsRepository.prefsFlow.collect { newPrefs ->
                prefs = newPrefs
                _uiState.value = _uiState.value.copy(
                    animationsEnabled = newPrefs.animationsEnabled,
                    showCaptureHints = newPrefs.showCaptureHints,
                    canUndo = computeCanUndo(),
                )
            }
        }
    }

    fun onSquareTap(sq: Square) {
        if (!_uiState.value.isPlayersTurn) return

        if (partialChain.isNotEmpty()) {
            val nextIdx = partialChain.size
            val advancing = partialChainCandidates.filter {
                it.path.size > nextIdx && it.path[nextIdx] == sq
            }
            if (advancing.isNotEmpty()) {
                partialChain = partialChain + sq
                partialChainCandidates = advancing
                val terminal = partialChainCandidates.filter { it.path.size == partialChain.size }
                if (terminal.size == partialChainCandidates.size && terminal.isNotEmpty()) {
                    val move = terminal.first()
                    resetSelection()
                    viewModelScope.launch { playHumanMove(move) }
                    return
                }
                publishPartialChain()
                return
            }
            resetSelection()
        }

        val piece = gameState.board.pieceAt(sq)
        val mover = gameState.sideToMove
        if (piece != null && piece.color == mover) {
            val movesFromSq = MoveGenerator.legalMoves(gameState.board, mover)
                .filter { it.from == sq }
            if (movesFromSq.isEmpty()) {
                resetSelection()
                return
            }
            partialChain = listOf(sq)
            partialChainCandidates = movesFromSq
            publishPartialChain()
            return
        }
        resetSelection()
    }

    fun onAnimationDone() {
        pendingAnimationDone?.complete(Unit)
        pendingAnimationDone = null
    }

    fun restart() {
        pendingAnimationDone?.complete(Unit)
        pendingAnimationDone = null

        gameState = GameState.newGame(rulesConfig)
        resultRecorded = false
        trajectory.clear()
        trajectory += gameState.board
        partialChain = emptyList()
        partialChainCandidates = emptyList()
        preUndoState = null
        publishUi()
        viewModelScope.launch {
            if (!twoPlayerMode && gameState.sideToMove != humanColor) runAiTurn()
        }
    }

    fun resign() {
        if (_uiState.value.outcome != GameOutcome.Ongoing) return
        val loser = gameState.sideToMove
        val outcome = GameOutcome.Win(loser.opponent, WinReason.RESIGNATION)
        _uiState.value = _uiState.value.copy(
            outcome = outcome,
            selectedSquare = null,
            destinations = emptyMap(),
            animatingMove = null,
        )
        viewModelScope.launch { recordOutcomeIfNeeded(outcome) }
    }

    fun undo() {
        if (!computeCanUndo()) return
        val saved = preUndoState ?: return
        gameState = saved
        while (trajectory.size > preUndoTrajectorySize) {
            trajectory.removeAt(trajectory.size - 1)
        }
        preUndoState = null
        partialChain = emptyList()
        partialChainCandidates = emptyList()
        _uiState.value = _uiState.value.copy(
            board = gameState.board,
            sideToMove = gameState.sideToMove,
            lastMove = null,
            selectedSquare = null,
            destinations = emptyMap(),
            outcome = GameOutcome.Ongoing,
            aiThinking = false,
            animatingMove = null,
        )
        refreshOutcomeAndCaptures()
        _uiState.value = _uiState.value.copy(canUndo = computeCanUndo())
    }

    private suspend fun playHumanMove(move: Move) {
        // Snapshot for undo BEFORE applying the human's move.
        if (!twoPlayerMode) {
            preUndoState = gameState
            preUndoTrajectorySize = trajectory.size
        }
        playAndCommit(move)
        val outcome = _uiState.value.outcome
        if (outcome != GameOutcome.Ongoing) {
            recordOutcomeIfNeeded(outcome)
            return
        }
        if (!twoPlayerMode && gameState.sideToMove != humanColor) {
            runAiTurn()
            recordOutcomeIfNeeded(_uiState.value.outcome)
        }
    }

    private suspend fun runAiTurn() {
        _uiState.value = _uiState.value.copy(aiThinking = true)
        val moves = MoveGenerator.legalMoves(gameState.board, gameState.sideToMove)
        if (moves.isEmpty()) {
            _uiState.value = _uiState.value.copy(aiThinking = false)
            refreshOutcomeAndCaptures()
            return
        }
        val started = System.currentTimeMillis()
        val mv = withContext(Dispatchers.Default) {
            aiAgent.chooseMove(gameState, moves, random)
        }
        val elapsed = System.currentTimeMillis() - started
        if (elapsed < aiMinDelayMillis) delay(aiMinDelayMillis - elapsed)
        _uiState.value = _uiState.value.copy(aiThinking = false)
        playAndCommit(mv)
    }

    private suspend fun playAndCommit(move: Move) {
        if (_uiState.value.animationsEnabled) {
            val done = CompletableDeferred<Unit>()
            pendingAnimationDone = done
            _uiState.value = _uiState.value.copy(
                animatingMove = move,
                selectedSquare = null,
                destinations = emptyMap(),
            )
            done.await()
        }
        gameState = applyMove(gameState, move)
        trajectory += gameState.board
        _uiState.value = _uiState.value.copy(
            board = gameState.board,
            sideToMove = gameState.sideToMove,
            lastMove = move,
            animatingMove = null,
            selectedSquare = null,
            destinations = emptyMap(),
            canUndo = computeCanUndo(),
        )
        refreshOutcomeAndCaptures()
        if (prefs.soundEnabled) {
            if (move.isCapture) soundPlayer.playCapture() else soundPlayer.playMove()
        }
        if (_uiState.value.outcome != GameOutcome.Ongoing && prefs.soundEnabled) {
            soundPlayer.playOutcome()
        }
    }

    private suspend fun recordOutcomeIfNeeded(outcome: GameOutcome) {
        if (resultRecorded || outcome == GameOutcome.Ongoing) return
        resultRecorded = true
        if (twoPlayerMode) return // 2P games don't change progress or train weights.
        val result = when (outcome) {
            is GameOutcome.Win -> if (outcome.winner == humanColor) PlayerResult.WIN else PlayerResult.LOSS
            is GameOutcome.Draw -> PlayerResult.DRAW
            GameOutcome.Ongoing -> return
        }
        val winnerForLearning = when (outcome) {
            is GameOutcome.Win -> outcome.winner
            else -> null
        }
        val snapshot = trajectory.toList()
        val gamesTrainedAtEnd = gamesTrained + 1
        progressRepository.recordResult(level, result)
        if (level > 0) {
            val newWeights = tdLearner.update(
                weights = aiWeights,
                trajectory = snapshot,
                outcome = TDLearner.outcomeFor(winnerForLearning),
            )
            aiWeights = newWeights
            gamesTrained = gamesTrainedAtEnd
            aiRepository.saveWeights(newWeights, gamesTrainedAtEnd)
        }
    }

    private fun refreshOutcomeAndCaptures() {
        val moves = MoveGenerator.legalMoves(gameState.board, gameState.sideToMove)
        val outcome = Referee.outcome(gameState, moves)
        _uiState.value = _uiState.value.copy(
            outcome = outcome,
            anyCaptureAvailable = moves.any { it.isCapture },
        )
    }

    private fun computeCanUndo(): Boolean {
        if (twoPlayerMode) return false
        if (!prefs.undoEnabled) return false
        if (preUndoState == null) return false
        if (_uiState.value.aiThinking || _uiState.value.animatingMove != null) return false
        if (_uiState.value.outcome != GameOutcome.Ongoing) return false
        // Disable at the current "challenge" level so the score stays fair.
        return level != unlockedLevel
    }

    private fun resetSelection() {
        partialChain = emptyList()
        partialChainCandidates = emptyList()
        _uiState.value = _uiState.value.copy(selectedSquare = null, destinations = emptyMap())
    }

    private fun publishPartialChain() {
        val nextIdx = partialChain.size
        val landings = LinkedHashMap<Square, Move>()
        for (m in partialChainCandidates) {
            if (m.path.size > nextIdx) {
                val next = m.path[nextIdx]
                landings.putIfAbsent(next, m)
            }
        }
        _uiState.value = _uiState.value.copy(
            selectedSquare = partialChain.firstOrNull(),
            destinations = landings,
        )
    }

    private fun applyTournamentRulesIfNeeded() {
        if (prefs.tournamentRules == rulesConfig.tournamentDrawCounters) return
        rulesConfig = rulesConfig.copy(tournamentDrawCounters = prefs.tournamentRules)
        // Resetting mid-game would be jarring; only apply at next newGame.
    }

    private fun publishUi() {
        _uiState.value = buildUiState()
        refreshOutcomeAndCaptures()
    }

    private fun buildUiState() = GameUiState(
        board = gameState.board,
        sideToMove = gameState.sideToMove,
        playerColor = humanColor,
        level = level,
        animationsEnabled = prefs.animationsEnabled,
        showCaptureHints = prefs.showCaptureHints,
        twoPlayerMode = twoPlayerMode,
        canUndo = computeCanUndo(),
    )
}
