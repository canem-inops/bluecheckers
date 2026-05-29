package dev.canem.bluecheckers.data.repository

import dev.canem.bluecheckers.ai.Weights
import dev.canem.bluecheckers.data.db.WeightSnapshotDao
import dev.canem.bluecheckers.data.entity.WeightSnapshot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists and loads the single global [Weights] used by the AI. Training during
 * any game updates this shared snapshot so progress is cumulative across levels.
 */
@Singleton
class AiRepository @Inject constructor(
    private val dao: WeightSnapshotDao,
) {

    suspend fun loadWeights(): Weights {
        val snapshot = dao.get() ?: return Weights.DEFAULT
        if (snapshot.schemaVersion != Weights.SCHEMA_VERSION) return Weights.DEFAULT
        return runCatching { Weights(parse(snapshot.weightsCsv)) }.getOrDefault(Weights.DEFAULT)
    }

    suspend fun loadGamesTrained(): Int = dao.get()?.gamesTrained ?: 0

    suspend fun saveWeights(weights: Weights, gamesTrained: Int) {
        dao.upsert(
            WeightSnapshot(
                id = WeightSnapshot.SINGLETON_ID,
                schemaVersion = Weights.SCHEMA_VERSION,
                weightsCsv = weights.values.joinToString(",") { it.toString() },
                gamesTrained = gamesTrained,
                updatedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun resetAll() {
        dao.clearAll()
    }

    private fun parse(csv: String): DoubleArray =
        csv.split(',').map { it.toDouble() }.toDoubleArray()
}
