package dev.canem.bluecheckers.data.repository

import dev.canem.bluecheckers.data.db.LevelScoreDao
import dev.canem.bluecheckers.data.entity.LevelScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayerResult { WIN, LOSS, DRAW }

/**
 * Aggregate progress data derived from the per-level score rows.
 *
 * Unlock model: the player must win [ProgressRepository.WINS_TO_UNLOCK_NEXT]
 * games at level N to unlock level N+1. [winsAtUnlocked] is how many wins the
 * player has at the current challenge level so they can see "3/5 wins" on home.
 */
data class ProgressSummary(
    val unlockedLevel: Int,
    val winsAtUnlocked: Int,
    val winsToUnlockNext: Int,
    val lastPlayedLevel: Int?,
    val highestBeatenLevel: Int?,
    val totalGames: Int,
    val totalWins: Int,
    val totalLosses: Int,
    val totalDraws: Int,
) {
    /** True once the player has earned enough wins to unlock the next level. */
    val readyToUnlock: Boolean get() = winsAtUnlocked >= winsToUnlockNext

    companion object {
        val EMPTY = ProgressSummary(
            unlockedLevel = 0,
            winsAtUnlocked = 0,
            winsToUnlockNext = ProgressRepository.WINS_TO_UNLOCK_NEXT,
            lastPlayedLevel = null,
            highestBeatenLevel = null,
            totalGames = 0,
            totalWins = 0,
            totalLosses = 0,
            totalDraws = 0,
        )
    }
}

/**
 * Tracks player progress under the multi-win-to-unlock model.
 *
 * Levels are unbounded. Winning [WINS_TO_UNLOCK_NEXT] games at level N unlocks
 * level N+1. This makes the difficulty curve more gradual than a single-win
 * unlock and gives the AI more games to learn from at each level before the
 * search-depth jump.
 */
@Singleton
class ProgressRepository @Inject constructor(
    private val dao: LevelScoreDao,
) {

    fun observeSummary(): Flow<ProgressSummary> = dao.observeAll().map { rows ->
        if (rows.isEmpty()) return@map ProgressSummary.EMPTY
        // The "fully beaten" levels are those where the player already has
        // enough wins. The current challenge level sits one above those.
        val fullyBeaten = rows
            .filter { it.wins >= WINS_TO_UNLOCK_NEXT }
            .maxOfOrNull { it.level }
        val unlocked = (fullyBeaten?.plus(1)) ?: 0
        val winsAtUnlocked = rows.firstOrNull { it.level == unlocked }?.wins ?: 0
        val highestBeaten = rows.filter { it.wins > 0 }.maxOfOrNull { it.level }
        val lastPlayed = rows.maxByOrNull { it.lastPlayedAt }?.level
        ProgressSummary(
            unlockedLevel = unlocked,
            winsAtUnlocked = winsAtUnlocked,
            winsToUnlockNext = WINS_TO_UNLOCK_NEXT,
            lastPlayedLevel = lastPlayed,
            highestBeatenLevel = highestBeaten,
            totalGames = rows.sumOf { it.wins + it.losses + it.draws },
            totalWins = rows.sumOf { it.wins },
            totalLosses = rows.sumOf { it.losses },
            totalDraws = rows.sumOf { it.draws },
        )
    }

    fun observeAllScores(): Flow<List<LevelScore>> = dao.observeAll()

    suspend fun recordResult(level: Int, result: PlayerResult) {
        val current = dao.getForLevel(level) ?: LevelScore(level = level)
        val now = System.currentTimeMillis()
        val updated = when (result) {
            PlayerResult.WIN -> current.copy(wins = current.wins + 1, lastPlayedAt = now)
            PlayerResult.LOSS -> current.copy(losses = current.losses + 1, lastPlayedAt = now)
            PlayerResult.DRAW -> current.copy(draws = current.draws + 1, lastPlayedAt = now)
        }
        dao.upsert(updated)
    }

    suspend fun resetAll() {
        dao.clearAll()
    }

    companion object {
        /** Wins required at level N to unlock level N+1. */
        const val WINS_TO_UNLOCK_NEXT: Int = 5
    }
}
