package dev.canem.bluecheckers.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.canem.bluecheckers.data.db.AppDatabase
import dev.canem.bluecheckers.data.db.LevelScoreDao
import dev.canem.bluecheckers.data.db.WeightSnapshotDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "bluecheckers.db")
            // Pre-1.0 — schema changes wipe local data rather than maintaining migrations.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideLevelScoreDao(db: AppDatabase): LevelScoreDao = db.levelScoreDao()

    @Provides
    fun provideWeightSnapshotDao(db: AppDatabase): WeightSnapshotDao = db.weightSnapshotDao()
}
