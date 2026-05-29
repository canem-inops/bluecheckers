package dev.canem.bluecheckers.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {

    @Test
    fun `initial board has 12 white men and 12 black men on dark squares`() {
        val b = Board.initial()
        assertEquals(12, b.countMen(PieceColor.WHITE))
        assertEquals(12, b.countMen(PieceColor.BLACK))
        assertEquals(0, b.countKings(PieceColor.WHITE))
        assertEquals(0, b.countKings(PieceColor.BLACK))
    }

    @Test
    fun `initial board has white on rows 0-2 and black on rows 5-7`() {
        val b = Board.initial()
        for (sq in (0 until 64).map { Square.of(it) }) {
            val p = b.pieceAt(sq)
            when (sq.row) {
                0, 1, 2 -> if (sq.isDark) assertEquals(PieceColor.WHITE, p?.color) else assertNull(p)
                3, 4 -> assertNull("middle rows empty at $sq", p)
                5, 6, 7 -> if (sq.isDark) assertEquals(PieceColor.BLACK, p?.color) else assertNull(p)
            }
        }
    }

    @Test
    fun `bottom-left corner is dark (Brazilian orientation)`() {
        assertTrue(Square(0, 0).isDark)
        assertFalse(Square(0, 7).isDark)
        assertFalse(Square(7, 0).isDark)
        assertTrue(Square(7, 7).isDark)
    }

    @Test
    fun `fromGrid round-trips through render`() {
        // Row indices go 7 (top) down to 0 (bottom). Dark squares are where row+col is even.
        val grid = """
            .b.b.b.b
            b.b.b.b.
            .b.b.b.b
            _._._._.
            ._._._._
            w.w.w.w.
            .w.w.w.w
            w.w.w.w.
        """.trimIndent()
        val b = Board.fromGrid(grid)
        assertEquals(Board.initial(), b)
        assertEquals(grid + "\n", b.render())
    }

    @Test
    fun `set replaces and clears cells immutably`() {
        val original = Board.initial()
        val updated = original.set(
            mapOf(
                Square(2, 0) to null,
                Square(3, 1) to Piece(PieceColor.WHITE, isKing = true),
            )
        )
        assertNull(updated.pieceAt(Square(2, 0)))
        assertNotNull(updated.pieceAt(Square(3, 1)))
        assertTrue(updated.pieceAt(Square(3, 1))!!.isKing)
        // Original unchanged.
        assertNotNull(original.pieceAt(Square(2, 0)))
        assertNull(original.pieceAt(Square(3, 1)))
    }
}
