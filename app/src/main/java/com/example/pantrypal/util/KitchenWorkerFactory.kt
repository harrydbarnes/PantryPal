package com.example.pantrypal.util

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.pantrypal.data.repository.KitchenRepository

class KitchenWorkerFactory(private val repository: KitchenRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ExpirationWorker::class.java.name -> ExpirationWorker(appContext, workerParameters, repository)
            else -> null
        }
    }
}
