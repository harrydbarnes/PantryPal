package com.example.pantrypal.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class WidgetUpdateWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            PantryPalWidgetProvider.updateWidgetsNow(applicationContext)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Widget update failed", error)
            Result.retry()
        }
    }

    private companion object {
        const val TAG = "WidgetUpdateWorker"
    }
}
