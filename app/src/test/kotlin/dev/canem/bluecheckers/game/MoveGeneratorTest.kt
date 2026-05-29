package dev.canem.bluecheckers.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveGeneratorTest {

    private fun emptyBoard() = Board(MutableList<Piece?>(64) { null })

    private fun boardOf(vararg pieces: Pair<Square, Piece>): Board =
        emptyBoard().set(pieces.toMap())

    private fun stateOf(board: Board, side: PieceColor): GameState =
        GameState(
            board = board,
            sideToMove = side,
            plyCount = 0,
            pliesSinceProgress = 0,
            history = listOf(PositionKey(board.cells, side)),
        )

    @Test
    fun `initial position gives white seven quiet moves`() {
        val moves = MoveGenerator.legalMoves(Board.initial(), PieceColor.WHITE)
        assertEquals(7, moves.size)
        assertTrue(moves.all { !it.isCapture })
        val from20 = moves.filter { it.from == Square(2, 0) }
        assertEquals(1, from20.size)
        assertEquals(Square(3, 1), from20.single().to)
        val from22 = moves.filter { it.from == Square(2, 2) }.map { it.to }.toSet()
        assertEquals(setOf(Square(3, 1), Square(3, 3)), from22)
    }

    @Test
    fun `initial position gives black seven quiet moves moving down the board`() {
        val moves = MoveGenerator.legalMoves(Board.initial(), PieceColor.BLACK)
        assertEquals(7, moves.size)
        moves.forEach { assertTrue("black moves down: $it", it.to.row < it.from.row) }
    }

    @Test
    fun `man must capture when capture available`() {
        val board = boardOf(
            Square(3, 3) to Piece(PieceColor.WHITE),
            Square(4, 4) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertEquals(1, moves.size)
        val m = moves.single()
        assertTrue(m.isCapture)
        assertEquals(Square(3, 3), m.from)
        assertEquals(Square(5, 5), m.to)
        assertEquals(listOf(Square(4, 4)), m.captured)
    }

    @Test
    fun `man captures backward as well as forward`() {
        // White man at (5,3), black man at (4,4) — backward jump for white lands at (3,5).
        val board = boardOf(
            Square(5, 3) to Piece(PieceColor.WHITE),
            Square(4, 4) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertEquals(1, moves.size)
        val m = moves.single()
        assertTrue(m.isCapture)
        assertEquals(Square(5, 3), m.from)
        assertEquals(Square(3, 5), m.to)
    }

    @Test
    fun `lei da maioria forces choosing the longest capture sequence`() {
        // White man at (2,2). Single-capture option: capture (3,1) → land (4,0).
        // Two-capture option: capture (3,3) → land (4,4) → capture (5,5) → land (6,6).
        // Only the 2-capture chain is legal.
        val board = boardOf(
            Square(2, 2) to Piece(PieceColor.WHITE),
            Square(3, 1) to Piece(PieceColor.BLACK),
            Square(3, 3) to Piece(PieceColor.BLACK),
            Square(5, 5) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertEquals(1, moves.size)
        val m = moves.single()
        assertEquals(2, m.captureCount)
        assertEquals(Square(2, 2), m.from)
        assertEquals(Square(6, 6), m.to)
        assertEquals(setOf(Square(3, 3), Square(5, 5)), m.captured.toSet())
    }

    @Test
    fun `man passing through promotion row mid-chain does NOT promote`() {
        // (5,3) → (7,5) → (5,7): touches row 7 mid-chain but ends at (5,7), no promotion.
        val board = boardOf(
            Square(5, 3) to Piece(PieceColor.WHITE),
            Square(6, 4) to Piece(PieceColor.BLACK),
            Square(6, 6) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertEquals(1, moves.size)
        val m = moves.single()
        assertEquals(2, m.captureCount)
        assertEquals(Square(5, 7), m.to)
        assertFalse("must not promote when chain ends off promotion row", m.promoted)
    }

    @Test
    fun `man stopping on promotion row at end of move promotes`() {
        // White man at (6,0): only one forward square exists, (7,1). Lands and promotes.
        val board = boardOf(Square(6, 0) to Piece(PieceColor.WHITE))
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertEquals(1, moves.size)
        val m = moves.single()
        assertEquals(Square(7, 1), m.to)
        assertTrue(m.promoted)
        val next = applyMove(stateOf(board, PieceColor.WHITE), m)
        assertTrue(next.board.pieceAt(Square(7, 1))!!.isKing)
    }

    @Test
    fun `king moves flying any number of empty squares`() {
        val board = boardOf(Square(3, 3) to Piece(PieceColor.WHITE, isKing = true))
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        // From (3,3): NE=4, NW=3, SE=3, SW=3 -> 13 total quiet moves.
        assertEquals(13, moves.size)
        assertTrue(moves.all { !it.isCapture })
    }

    @Test
    fun `king captures at a distance and may land on any empty square beyond`() {
        val board = boardOf(
            Square(1, 1) to Piece(PieceColor.WHITE, isKing = true),
            Square(4, 4) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        val landings = moves.map { it.to }.toSet()
        assertEquals(setOf(Square(5, 5), Square(6, 6), Square(7, 7)), landings)
        moves.forEach { assertEquals(listOf(Square(4, 4)), it.captured) }
    }

    @Test
    fun `king cannot jump same piece twice in chain`() {
        // King at (0,0), single opponent at (2,2). After capturing it, the king cannot
        // continue capturing it again — chain ends after exactly one capture.
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE, isKing = true),
            Square(2, 2) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertTrue(moves.isNotEmpty())
        moves.forEach { assertEquals(1, it.captureCount) }
    }

    @Test
    fun `king two-capture chain with direction change`() {
        // King at (0,0). Capture black at (3,3), land at (4,4). Then capture black at (4,6)
        // on the perpendicular diagonal, land at (4,7)? — wait, (4,4) + (0,+2) is not diagonal.
        // Use: King at (0,0), black at (3,3), black at (5,3). After landing on (4,4), the
        // diagonal (4,4)→(5,3) hits black at (5,3); landing at (6,2) is empty. Chain length 2.
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE, isKing = true),
            Square(3, 3) to Piece(PieceColor.BLACK),
            Square(5, 3) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        // Lei da maioria => only 2-capture chains.
        moves.forEach { assertEquals(2, it.captureCount) }
        assertTrue(moves.any { it.to == Square(6, 2) })
    }

    @Test
    fun `friendly piece blocks king path`() {
        // King at (0,0), friendly white man at (3,3). King NE direction stops at (2,2).
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE, isKing = true),
            Square(3, 3) to Piece(PieceColor.WHITE),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        // King quiet moves along NE: (1,1) and (2,2). Other directions: SE has nothing inbounds
        // beyond row 0 except (-1, 1) blocked; SW none; NW none (cols -1). So NE only.
        val kingMoves = moves.filter { it.from == Square(0, 0) }
        val landings = kingMoves.map { it.to }.toSet()
        assertEquals(setOf(Square(1, 1), Square(2, 2)), landings)
    }

    @Test
    fun `stalemate means opponent wins`() {
        // White man at (0,0), boxed in: only forward square (1,1) is black, with (2,2) black too.
        // No quiet move, no capture (landing (2,2) blocked by black).
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE),
            Square(1, 1) to Piece(PieceColor.BLACK),
            Square(2, 2) to Piece(PieceColor.BLACK),
        )
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        assertTrue("white should have no moves: $moves", moves.isEmpty())
        val outcome = Referee.outcome(stateOf(board, PieceColor.WHITE), moves)
        assertTrue(outcome is GameOutcome.Win)
        assertEquals(PieceColor.BLACK, (outcome as GameOutcome.Win).winner)
    }

    @Test
    fun `applyMove removes captured pieces, promotes if needed, and switches side`() {
        val board = boardOf(
            Square(3, 3) to Piece(PieceColor.WHITE),
            Square(4, 4) to Piece(PieceColor.BLACK),
        )
        val state = stateOf(board, PieceColor.WHITE)
        val m = MoveGenerator.legalMoves(board, PieceColor.WHITE).single()
        val next = applyMove(state, m)
        assertEquals(PieceColor.BLACK, next.sideToMove)
        assertNull(next.board.pieceAt(Square(3, 3)))
        assertNull(next.board.pieceAt(Square(4, 4)))
        assertEquals(PieceColor.WHITE, next.board.pieceAt(Square(5, 5))?.color)
        assertEquals(1, next.plyCount)
        assertEquals(0, next.pliesSinceProgress)
    }

    @Test
    fun `draw by plies-without-progress fires at configured threshold`() {
        // Hand-craft a state past the threshold and verify the Referee declares a draw.
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE, isKing = true),
            Square(7, 7) to Piece(PieceColor.BLACK, isKing = true),
        )
        val state = GameState(
            board = board,
            sideToMove = PieceColor.WHITE,
            plyCount = 10,
            pliesSinceProgress = 4,
            history = listOf(PositionKey(board.cells, PieceColor.WHITE)),
            rules = RulesConfig(drawPliesWithoutProgress = 4, drawOnThreefoldRepetition = false),
        )
        val moves = MoveGenerator.legalMoves(state.board, state.sideToMove)
        assertTrue(moves.isNotEmpty())
        val outcome = Referee.outcome(state, moves)
        assertTrue("expected Draw, got $outcome", outcome is GameOutcome.Draw)
    }

    @Test
    fun `pliesSinceProgress counter increments only on quiet king moves`() {
        // Two kings shuffling: king move should NOT reset the counter.
        val board = boardOf(
            Square(0, 0) to Piece(PieceColor.WHITE, isKing = true),
            Square(0, 2) to Piece(PieceColor.BLACK, isKing = true),
        )
        val state = GameState(
            board = board,
            sideToMove = PieceColor.WHITE,
            plyCount = 0,
            pliesSinceProgress = 0,
            history = listOf(PositionKey(board.cells, PieceColor.WHITE)),
        )
        val mv = MoveGenerator.legalMoves(state.board, state.sideToMove)
            .first { !it.isCapture }
        val next = applyMove(state, mv)
        assertEquals(1, next.pliesSinceProgress)
    }

    @Test
    fun `man with no captures available has only forward quiet moves`() {
        val board = boardOf(Square(3, 3) to Piece(PieceColor.WHITE))
        val moves = MoveGenerator.legalMoves(board, PieceColor.WHITE)
        // White man at (3,3) — quiet moves to (4,2) and (4,4) only.
        assertEquals(setOf(Square(4, 2), Square(4, 4)), moves.map { it.to }.toSet())
    }
}
