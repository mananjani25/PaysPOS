package com.pays.pos.utils.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.jetbrains.annotations.NotNull

class PrinterWorkExecutor(@NotNull context: Context, @NotNull params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val printerList = inputData.getString("printerList")
        val printData = inputData.getString("printData")




        return Result.failure()
    }
}