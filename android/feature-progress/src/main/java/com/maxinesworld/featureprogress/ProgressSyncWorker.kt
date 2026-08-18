package com.maxinesworld.featureprogress

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

class ProgressSyncWorker(
    appContext: Context,
    params: WorkerParameters,
    private val syncRepository: ProgressSyncRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val childId = inputData.getString(KEY_CHILD_ID) ?: return Result.failure()
        val lastTimestamp = inputData.getLong(KEY_LAST_TIMESTAMP, 0L)

        return when (val result = syncRepository.sync(childId, lastTimestamp)) {
            is SyncResult.Success -> {
                val output = workDataOf(
                    "pushed" to result.pushedCount,
                    "pulled" to result.pulledCount
                )
                Result.success(output)
            }
            is SyncResult.Error -> {
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "maxines_progress_periodic_sync"
        const val KEY_CHILD_ID = "child_id"
        const val KEY_LAST_TIMESTAMP = "last_timestamp"

        fun schedulePeriodicSync(context: Context, childId: String) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<ProgressSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_CHILD_ID to childId))
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }
    }
}
