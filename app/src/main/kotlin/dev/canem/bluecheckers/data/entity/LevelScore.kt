package dev.canem.bluecheckers.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-level outcome record. Used for the home-screen score table and for the
 * win-to-unlock rule: level N+1 is unlocked when this row exists with wins > 0.
 */
@Entity(tableName = "level_score")
data class LevelScore(
    @PrimaryKey val level: Int,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val lastPlayedAt: Long = 0L,
)
