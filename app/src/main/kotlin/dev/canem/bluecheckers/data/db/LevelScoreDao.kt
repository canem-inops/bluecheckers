package dev.canem.bluecheckers.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.canem.bluecheckers.data.entity.LevelScore
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelScoreDao {

    @Query("SELECT * FROM level_score ORDER BY level ASC")
    fun observeAll(): Flow<List<LevelScore>>

    @Query("SELECT * FROM level_score WHERE level = :level")
    suspend fun getForLevel(level: Int): LevelScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: LevelScore)

    @Query("DELETE FROM level_score")
    suspend fun clearAll()
}
