package com.obscura.wallpapers.wallpaper.cycler

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.obscura.wallpapers.core.data.model.AutoChangeFrequency
import com.obscura.wallpapers.core.data.model.WallpaperTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WallpaperCyclerWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    workerParameters: WorkerParameters,
    private val cyclerController: CyclerController,
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val TAG = "WallpaperCyclerWorker"
        const val WORK_NAME = "auto_wallpaper_work"
        const val KEY_TARGET = "target_screen"
        const val INTERVAL_DAILY = "daily"
        const val INTERVAL_12_HOURS = "12_hours"
        const val INTERVAL_6_HOURS = "6_hours"
        const val INTERVAL_1_HOUR = "1_hour"
        const val WALLPAPER_TYPE_HOME = "home"
        const val WALLPAPER_TYPE_LOCK = "lock"
        const val WALLPAPER_TYPE_BOTH = "both"
        const val SOURCE_FAVORITES = "favorites"
        const val SOURCE_CATEGORY = "category"

        fun schedule(
            workManager: WorkManager, frequency: AutoChangeFrequency, wifiOnly: Boolean
        ) {
            workManager.cancelUniqueWork(WORK_NAME)
            val constraints = Constraints.Builder().apply {
                if (wifiOnly) {
                    setRequiredNetworkType(NetworkType.UNMETERED)
                } else {
                    setRequiredNetworkType(NetworkType.CONNECTED)
                }
                setRequiresBatteryNotLow(true)
            }.build()
            if (frequency == AutoChangeFrequency.EACH_UNLOCK) {
                Log.d(TAG, "Using broadcast receiver for EACH_UNLOCK frequency")
                return
            }
            val hours = when (frequency) {
                AutoChangeFrequency.DAILY -> 24L
                AutoChangeFrequency.TWELVE_HOURS -> 12L
                AutoChangeFrequency.SIX_HOURS -> 6L
                AutoChangeFrequency.HOURLY -> 1L
                AutoChangeFrequency.EACH_UNLOCK -> 24L
            }
            val workRequest = PeriodicWorkRequestBuilder<WallpaperCyclerWorker>(
                hours, java.util.concurrent.TimeUnit.HOURS
            ).setConstraints(constraints).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, workRequest
            )
            Log.d(TAG, "Scheduled auto wallpaper change every $hours hours")
        }

        fun runOnce(
            workManager: WorkManager, target: WallpaperTarget = WallpaperTarget.BOTH
        ) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val inputData = Data.Builder().putString(KEY_TARGET, target.name).build()
            val workRequest = OneTimeWorkRequestBuilder<WallpaperCyclerWorker>().setConstraints(constraints).setInputData(inputData).build()
            workManager.enqueue(workRequest)
            Log.d(TAG, "Scheduled one-time wallpaper change")
        }

        fun scheduleAutoWallpaperChange(
            context: Context,
            interval: String,
            source: String,
            category: String? = null,
            wallpaperType: String = WALLPAPER_TYPE_BOTH,
            wifiOnly: Boolean = true
        ) {
            val intervalHours = when (interval) {
                INTERVAL_DAILY -> 24
                INTERVAL_12_HOURS -> 12
                INTERVAL_6_HOURS -> 6
                INTERVAL_1_HOUR -> 1
                else -> 24
            }
            val inputData = Data.Builder()
                .putString("source", source)
                .putString("category", category)
                .putString("wallpaperType", wallpaperType)
                .build()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
            val workRequest = PeriodicWorkRequestBuilder<WallpaperCyclerWorker>(
                intervalHours.toLong(), java.util.concurrent.TimeUnit.HOURS
            ).setConstraints(constraints).setInputData(inputData).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, workRequest
            )
            Log.d(TAG, "Scheduled auto wallpaper change - Interval: $interval, Source: $source")
        }

        fun cancelAutoWallpaperChange(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d(TAG, "Cancelled auto wallpaper change")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) @androidx.annotation.RequiresPermission(
        android.Manifest.permission.POST_NOTIFICATIONS
    ) {
        try {
            val ok = cyclerController.performAutoChange()
            if (ok) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
