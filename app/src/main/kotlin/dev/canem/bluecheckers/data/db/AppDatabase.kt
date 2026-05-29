package dev.canem.bluecheckers.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.canem.bluecheckers.data.entity.LevelScore
import dev.canem.bluecheckers.data.entity.WeightSnapshot

@Database(
    entities = [LevelScore::class, WeightSnapshot::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun levelScoreDao(): LevelScoreDao
    abstract fun weightSnapshotDao(): WeightSnapshotDao
}
