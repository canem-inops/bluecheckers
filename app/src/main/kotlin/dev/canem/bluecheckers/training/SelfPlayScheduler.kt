package dev.canem.bluecheckers.training

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules or cancels the periodic [SelfPlayWorker] based on the user's
 * preference. Constraints ensure the worker only fires when the device is
 * charging and idle so it never hurts the user's battery or interactivity.
 */
@Singleton
class SelfPlayScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun setEnabled(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(SelfPlayWorker.UNIQUE_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()
        val request = PeriodicWorkRequestBuilder<SelfPlayWorker>(2, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            SelfPlayWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
