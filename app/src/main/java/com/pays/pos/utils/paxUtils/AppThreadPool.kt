package com.pays.pos.utils.paxUtils

import android.os.Handler
import java.util.concurrent.*

class AppThreadPool private constructor() {
    private val threadPool: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val threadPoolTwo: ExecutorService = Executors.newCachedThreadPool()
    private val mainThreadHandler: Handler?

    private class LazyHolder {
        val INSTANCE = AppThreadPool()
    }

    init {
        mainThreadHandler = Handler()
    }

    fun <T> postTask(callable: Callable<T>, callback: FinishInMainThreadCallback<T>?) {
        threadPool.execute {
            try {
                val result = callable.call()
                mainThreadHandler!!.post { callback?.onFinish(result as T) }
            } catch (e: Exception) {
                e.printStackTrace()
                throw RuntimeException(e)
            }
        }
    }

    fun <T> postTask(
        callable: Callable<T>,
        timeInMilliSec: Long,
        callback: FinishInMainThreadCallback<T>?
    ) {
        threadPool.schedule({
            try {
                val result = callable.call()
                mainThreadHandler!!.post { callback?.onFinish(result as T) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, timeInMilliSec, TimeUnit.MILLISECONDS)
    }

    fun runOnUiThread(runnable: Runnable?) {
        if (mainThreadHandler == null) {
            throw IllegalThreadStateException("Your POSLink does not init on main thread.")
        }
        mainThreadHandler.post(runnable!!)
    }

    fun runOnUiThreadDelay(runnable: Runnable?, timeout: Int) {
        mainThreadHandler!!.postDelayed(runnable!!, timeout.toLong())
    }

    fun runInBackground(runnable: Runnable?) {
        threadPool.submit(runnable)
    }

    fun runInOtherThread(runnable: Runnable?) {
        threadPoolTwo.submit(runnable)
    }

    interface FinishInMainThreadCallback<T> {
        fun onFinish(result: T)
    }
    companion object
}