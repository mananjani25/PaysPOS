package com.pays.pos.utils.workmanager

import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ThreadPoolManager private constructor() {
    private val service: ExecutorService
    fun executeTask(runnable: Runnable?) {
        service.execute(runnable)
    }

    fun executeTasks(list: LinkedList<Runnable?>) {
        for (runnable in list) {
            service.execute(runnable)
        }
    }

    companion object {
        val instance = ThreadPoolManager()
    }

    init {
        val num = Runtime.getRuntime().availableProcessors() * 20
        service = Executors.newFixedThreadPool(num)
    }
}