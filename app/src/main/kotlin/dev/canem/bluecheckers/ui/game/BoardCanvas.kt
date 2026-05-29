package dev.canem.bluecheckers.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import dev.canem.bluecheckers.game.Board
import dev.canem.bluecheckers.game.Move
import dev.canem.bluecheckers.game.Piece
import dev.canem.bluecheckers.game.PieceColor
import dev.canem.bluecheckers.game.Square
import dev.canem.bluecheckers.ui.theme.BoardDark
import dev.canem.bluecheckers.ui.theme.BoardLight
import dev.canem.bluecheckers.ui.theme.CaptureHighlight
import dev.canem.bluecheckers.ui.theme.KingMarker
import dev.canem.bluecheckers.ui.theme.LastMoveHighlight
import dev.canem.bluecheckers.ui.theme.PieceBlack
import dev.canem.bluecheckers.ui.theme.PieceBlackEdge
import dev.canem.bluecheckers.ui.theme.PieceWhite
import dev.canem.bluecheckers.ui.theme.PieceWhiteEdge
import dev.canem.bluecheckers.ui.theme.SelectionHighlight

/**
 * 8x8 board, rendered from white's perspective (row 0 at the bottom).
 *
 * When [animatingMove] is non-null the canvas shows [board] (the pre-move state)
 * and slides the moving piece along the move's path. Each path segment animates
 * over [segmentDurationMs]. Captured pieces fade out as the moving piece passes
 * them. Once the full animation finishes, [onAnimationDone] is invoked exactly
 * once so the ViewModel can commit the new board.
 */
@Composable
fun BoardCanvas(
    board: Board,
    selected: Square?,
    destinations: Set<Square>,
    lastMove: Move?,
    highlightCaptures: Boolean,
    animatingMove: Move?,
    onSquareTap: (Square) -> Unit,
    onAnimationDone: () -> Unit,
    modifier: Modifier = Modifier,
    segmentDurationMs: Int = 240,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(animatingMove) {
        if (animatingMove != null) {
            progress.snapTo(0f)
            val totalMs = ((animatingMove.path.size - 1).coerceAtLeast(1) * segmentDurationMs)
            progress.animateTo(1f, animationSpec = tween(durationMillis = totalMs))
            onAnimationDone()
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(board, animatingMove) {
                if (animatingMove != null) return@pointerInput
                detectTapGestures { offset ->
                    val sq = size.width / 8f
                    val col = (offset.x / sq).toInt().coerceIn(0, 7)
                    val row = (7 - (offset.y / sq).toInt()).coerceIn(0, 7)
                    onSquareTap(Square(row, col))
                }
            }
    ) {
        val squareSize = size.width / 8f
        val animatingPiece: Piece? = animatingMove?.let { board.pieceAt(it.from) }
        val hiddenSquares: Set<Square> = computeHiddenSquares(animatingMove, progress.value)

        drawBoardSquares(squareSize)
        drawHighlights(
            squareSize = squareSize,
            selected = selected,
            destinations = destinations,
            lastMove = if (animatingMove == null) lastMove else null,
            highlightCaptures = highlightCaptures,
        )
        drawPieces(board = board, squareSize = squareSize, hidden = hiddenSquares)

        if (animatingMove != null && animatingPiece != null) {
            drawFloatingPiece(
                piece = animatingPiece,
                squareSize = squareSize,
                position = positionAlongPath(animatingMove.path, progress.value),
            )
        }
    }
}

private fun computeHiddenSquares(animatingMove: Move?, progress: Float): Set<Square> {
    if (animatingMove == null) return emptySet()
    val hidden = mutableSetOf<Square>()
    hidden += animatingMove.from
    if (animatingMove.captured.isNotEmpty()) {
        val totalSegments = animatingMove.path.size - 1
        val segmentsDone = (progress * totalSegments).toInt().coerceIn(0, totalSegments)
        // A captured piece on segment k is gone once we've finished segment k.
        for (k in 0 until segmentsDone) {
            if (k < animatingMove.captured.size) hidden += animatingMove.captured[k]
        }
    }
    return hidden
}

/** Returns a fractional (row, col) along the path; used to place the floating piece. */
private fun positionAlongPath(path: List<Square>, progress: Float): Pair<Float, Float> {
    if (path.size == 1) return path.first().row.toFloat() to path.first().col.toFloat()
    val segments = path.size - 1
    val scaled = (progress * segments).coerceIn(0f, segments.toFloat())
    val idx = scaled.toInt().coerceIn(0, segments - 1)
    val t = scaled - idx
    val a = path[idx]
    val b = path[idx + 1]
    val row = a.row + (b.row - a.row) * t
    val col = a.col + (b.col - a.col) * t
    return row to col
}

private fun DrawScope.drawBoardSquares(squareSize: Float) {
    for (row in 0..7) {
        for (col in 0..7) {
            val isDark = (row + col) and 1 == 0
            drawRect(
                color = if (isDark) BoardDark else BoardLight,
                topLeft = Offset(col * squareSize, (7 - row) * squareSize),
                size = Size(squareSize, squareSize),
            )
        }
    }
}

private fun DrawScope.drawHighlights(
    squareSize: Float,
    selected: Square?,
    destinations: Set<Square>,
    lastMove: Move?,
    highlightCaptures: Boolean,
) {
    val lastSquares = lastMove?.path?.toSet().orEmpty()
    for (sq in lastSquares) {
        drawRect(
            color = LastMoveHighlight.copy(alpha = 0.40f),
            topLeft = Offset(sq.col * squareSize, (7 - sq.row) * squareSize),
            size = Size(squareSize, squareSize),
        )
    }
    if (selected != null) {
        drawRect(
            color = SelectionHighlight.copy(alpha = 0.55f),
            topLeft = Offset(selected.col * squareSize, (7 - selected.row) * squareSize),
            size = Size(squareSize, squareSize),
        )
    }
    for (sq in destinations) {
        val color = if (highlightCaptures) CaptureHighlight else SelectionHighlight
        drawCircle(
            color = color.copy(alpha = 0.7f),
            center = Offset(sq.col * squareSize + squareSize / 2f, (7 - sq.row) * squareSize + squareSize / 2f),
            radius = squareSize * 0.15f,
        )
    }
}

private fun DrawScope.drawPieces(board: Board, squareSize: Float, hidden: Set<Square>) {
    for (row in 0..7) {
        for (col in 0..7) {
            val sq = Square(row, col)
            if (sq in hidden) continue
            val piece = board.pieceAt(row, col) ?: continue
            drawPiece(piece, row.toFloat(), col.toFloat(), squareSize)
        }
    }
}

private fun DrawScope.drawFloatingPiece(
    piece: Piece,
    squareSize: Float,
    position: Pair<Float, Float>,
) {
    drawPiece(piece, position.first, position.second, squareSize)
}

private fun DrawScope.drawPiece(piece: Piece, row: Float, col: Float, squareSize: Float) {
    val (fill, edge) = when (piece.color) {
        PieceColor.WHITE -> PieceWhite to PieceWhiteEdge
        PieceColor.BLACK -> PieceBlack to PieceBlackEdge
    }
    val center = Offset(col * squareSize + squareSize / 2f, (7 - row) * squareSize + squareSize / 2f)
    val r = squareSize * 0.40f
    drawCircle(color = fill, radius = r, center = center)
    drawCircle(color = edge, radius = r, center = center, style = Stroke(width = squareSize * 0.04f))
    if (piece.isKing) {
        drawCircle(color = KingMarker, radius = squareSize * 0.18f, center = center)
        drawCircle(
            color = Color.Black.copy(alpha = 0.4f),
            radius = squareSize * 0.18f,
            center = center,
            style = Stroke(width = squareSize * 0.02f),
        )
    }
}
