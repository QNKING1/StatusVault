package com.statusvault.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.statusvault.app.data.repository.StatusRepository
import com.statusvault.app.data.prefs.SettingsDataStore
import com.statusvault.app.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class StatusScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val statusRepository: StatusRepository,
    private val settingsDataStore: SettingsDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "StatusScanWorker started")

        return try {
            // Only auto-save if enabled
            val autoSave = settingsDataStore.isAutoSave.first()
            val result = statusRepository.refreshStatuses()

            result.onSuccess { statuses ->
                Log.d(TAG, "Found ${statuses.size} statuses")

                if (autoSave) {
                    val unsaved = statuses.filter { !it.isSaved }
                    unsaved.forEach { status ->
                        statusRepository.saveStatus(status)
                    }
                    Log.d(TAG, "Auto-saved ${unsaved.size} statuses")
                }
            }.onFailure { error ->
                Log.e(TAG, "Status scan failed", error)
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "StatusScanWorker"

        fun enqueuePeriodicWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<StatusScanWorker>(
                Constants.STATUS_SCAN_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .addTag(Constants.STATUS_SCAN_WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                Constants.STATUS_SCAN_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )

            Log.d(TAG, "Periodic work enqueued")
        }

        fun cancelWork(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(Constants.STATUS_SCAN_WORK_NAME)
        }
    }
}
