package dev.canem.bluecheckers.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Singleton row holding the global linear-evaluator weights. There is exactly one
 * row (primary key always [SINGLETON_ID]); training during any game updates this
 * single set of weights so progress is cumulative across all levels.
 *
 * Stored as a comma-separated decimal string in [weightsCsv]. [schemaVersion]
 * lets loading code detect incompatibility after a future feature-vector change.
 */
@Entity(tableName = "weight_snapshot")
data class WeightSnapshot(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val schemaVersion: Int,
    val weightsCsv: String,
    val gamesTrained: Int,
    val updatedAt: Long,
) {
    companion object {
        const val SINGLETON_ID: Int = 0
    }
}
