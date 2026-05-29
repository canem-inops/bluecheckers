package dev.canem.bluecheckers.game

/**
 * A complete legal move from one square to another.
 *
 * - For a quiet move, [path] is `[from, to]` and [captured] is empty.
 * - For a capture chain, [path] contains every landing square in order including
 *   [from] and the final landing, and [captured] lists the captured squares in
 *   the order they were jumped. `path.size == captured.size + 1`.
 *
 * [promoted] is true iff this move ends with the piece being crowned (a man that
 * stops on its promotion row). A man that touches the promotion row mid-chain
 * but continues capturing does NOT promote (Brazilian rule).
 */
data class Move(
    val from: Square,
    val to: Square,
    val path: List<Square>,
    val captured: List<Square>,
    val promoted: Boolean,
) {
    val isCapture: Boolean get() = captured.isNotEmpty()
    val captureCount: Int get() = captured.size

    init {
        require(path.first() == from) { "path must start at from" }
        require(path.last() == to) { "path must end at to" }
        require(path.size == captured.size + 1 || (captured.isEmpty() && path.size == 2)) {
            "path length must be captured count + 1 (or 2 for a quiet move)"
        }
    }
}
