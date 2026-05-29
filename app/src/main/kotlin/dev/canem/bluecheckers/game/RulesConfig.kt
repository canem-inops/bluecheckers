package dev.canem.bluecheckers.game

/**
 * Tuning knobs that affect ambiguous rule edges. Defaults follow CBD (Confederação
 * Brasileira de Damas). Setting [tournamentDrawCounters] = true switches to the
 * FMJD-64 tiered draw counters.
 */
data class RulesConfig(
    /** CBD: 20 consecutive plies with no capture and no man move => draw. */
    val drawPliesWithoutProgress: Int = 20,
    /** Three-fold repetition triggers a draw. */
    val drawOnThreefoldRepetition: Boolean = true,
    /** When true, use FMJD-64 tiered endgame counters in place of [drawPliesWithoutProgress]. */
    val tournamentDrawCounters: Boolean = false,
) {
    companion object {
        val DEFAULT = RulesConfig()
    }
}
