package dev.canem.bluecheckers.ai

import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.GameState
import dev.canem.bluecheckers.game.Move
import dev.canem.bluecheckers.game.MoveGenerator
import dev.canem.bluecheckers.game.Piece
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.PositionKey
import dev.canem.bluecheckers.game.Square
import dev.canem.bluecheckers.game.applyMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AlphaBetaAgentTest {

    private fun emptyBoard() = Board(MutableList<Piece?>(64) { null })
    private fun boardOf(vararg pairs: Pair<Square, Piece>) = emptyBoard().set(pairs.toMap())
    private fun stateOf(board: Board, side: PieceColor) = GameState(
        board, side, 0, 0, listOf(PositionKey(board.cells, side)),
    )

    @Test
    fun `agent picks the only available move`() {
        val agent = AlphaBetaAgent(maxDepth = 4, evaluator = Evaluator(Weights.DEFAULT))
        val board = boardOf(Square(6, 0) to Piece(PieceColor.WHITE))
        val state = stateOf(board, PieceColor.WHITE)
        val moves = MoveGenerator.legalMoves(board, state.sideToMove)
        assertEquals(1, moves.size)
        assertEquals(moves.single(), agent.chooseMove(state, moves, Random(42)))
    }

    @Test
    fun `agent prefers the immediate capture over a quiet move`() {
        // White man at (3,3) with a capture of black at (4,4) landing on (5,5),
        // plus a quiet move from a second white man elsewhere. AB at any depth
        // already has the capture forced by lei da maioria, so legalMoves contains
        // only the capture — this just verifies the agent picks it.
        val board = boardOf(
            Square(3, 3) to Piece(PieceColor.WHITE),
            Square(4, 4) to Piece(PieceColor.BLACK),
            Square(1, 1) to Piece(PieceColor.WHITE),
        )
        val state = stateOf(board, PieceColor.WHITE)
        val moves = MoveGenerator.legalMoves(board, state.sideToMove)
        assertTrue(moves.all { it.isCapture })
        val chosen = AlphaBetaAgent(2, Evaluator(Weights.DEFAULT))
            .chooseMove(state, moves, Random(1))
        assertTrue(chosen.isCapture)
    }

    @Test
    fun `agent finds a forced two-move win against a single piece`() {
        // White king at (0,0). Black man at (1,1) with no escape moves except being captured.
        // After white captures, black has no pieces => Win for white.
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE, isKing = true),
            Square(1, 1) to Piece(PieceColor.BLACK),
        )
        val state = stateOf(board, PieceColor.WHITE)
        val moves = MoveGenerator.legalMoves(board, state.sideToMove)
        val agent = AlphaBetaAgent(maxDepth = 3, evaluator = Evaluator(Weights.DEFAULT))
        val chosen = agent.chooseMove(state, moves, Random(7))
        assertTrue("agent should pick the capture: $chosen", chosen.isCapture)
        val next = applyMove(state, chosen)
        assertEquals(0, next.board.count(PieceColor.BLACK))
    }

    @Test
    fun `level 0 produces a RandomAgent and plays a legal move from the initial position`() {
        val agent = agentForLevel(0)
        val state = GameState.newGame()
        val moves = MoveGenerator.legalMoves(state.board, state.sideToMove)
        val move = agent.chooseMove(state, moves, Random(0))
        assertTrue("level 0 must be RandomAgent", agent is RandomAgent)
        assertTrue(move in moves)
    }

    @Test
    fun `trained-weights agent beats random over many games most of the time`() {
        // Sanity check: AB at depth 3 with default weights should beat a random opponent
        // most of the time (we accept >=70% to keep variance low in CI).
        val wins = playMatch(
            n = 30,
            seedBase = 100,
            whiteAgent = AlphaBetaAgent(maxDepth = 3, evaluator = Evaluator(Weights.DEFAULT), timeBudgetMillis = 200L),
            blackAgent = RandomAgent(),
            maxPliesPerGame = 200,
        )
        assertTrue("AB should beat random >= 21/30: got $wins", wins >= 21)
    }

    /** Returns the number of WHITE wins out of [n] games (draws count as 0.5 rounded down for this purpose). */
    private fun playMatch(
        n: Int,
        seedBase: Long,
        whiteAgent: Agent,
        blackAgent: Agent,
        maxPliesPerGame: Int,
    ): Int {
        var whiteWins = 0
        for (i in 0 until n) {
            val rng = Random(seedBase + i)
            var state = GameState.newGame()
            while (true) {
                val moves = MoveGenerator.legalMoves(state.board, state.sideToMove)
                val outcome = dev.canem.bluecheckers.game.Referee.outcome(state, moves)
                if (outcome is dev.canem.bluecheckers.game.GameOutcome.Win) {
                    if (outcome.winner == PieceColor.WHITE) whiteWins++
                    break
                }
                if (outcome is dev.canem.bluecheckers.game.GameOutcome.Draw) break
                val agent = if (state.sideToMove == PieceColor.WHITE) whiteAgent else blackAgent
                val mv = agent.chooseMove(state, moves, rng)
                state = applyMove(state, mv)
                if (state.plyCount >= maxPliesPerGame) break
            }
        }
        return whiteWins
    }
}
