package com.android.pos.utils.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.jetbrains.annotations.NotNull

class QueueWorker(@NotNull context: Context, @NotNull params: WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {



        return Result.success()
    }
}