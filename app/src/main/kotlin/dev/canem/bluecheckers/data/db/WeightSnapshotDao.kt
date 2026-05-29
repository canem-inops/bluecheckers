package dev.canem.bluecheckers.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.canem.bluecheckers.data.entity.WeightSnapshot

@Dao
interface WeightSnapshotDao {

    @Query("SELECT * FROM weight_snapshot WHERE id = ${WeightSnapshot.SINGLETON_ID}")
    suspend fun get(): WeightSnapshot?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(snapshot: WeightSnapshot)

    @Query("DELETE FROM weight_snapshot")
    suspend fun clearAll()
}
