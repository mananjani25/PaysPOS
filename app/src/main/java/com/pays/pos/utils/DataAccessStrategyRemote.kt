package com.pays.pos.utils

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.pays.pos.utils.statusUtils.Resource
import com.pays.pos.utils.statusUtils.Status
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun <T> performGetOperationNew(
/*databaseQuery: () -> LiveData<T>,*/
    networkCall: suspend () -> Resource<T>
    /* saveCallResult: suspend (A) -> Unit*/
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        /*val source = databaseQuery.invoke().map { Resource.success(it) }
        emitSource(source)*/

        try {
            val responseStatus = networkCall.invoke()
            if (responseStatus.status == Status.SUCCESS) {
                // saveCallResult(responseStatus.data!!)
                // emitSource(source)
                emit(Resource.success(responseStatus.data!!))

            } else if (responseStatus.status == Status.ERROR) {
                emit(Resource.error(responseStatus.message!!))
                //emitSource(source)
            }
        } catch (e: Exception) {
            Log.d("exception123", "::" + e.message)
        }
    }


fun <T> performGetOperationDatabase(
    databaseQuery: () -> LiveData<T>
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        val source = databaseQuery.invoke().map { Resource.success(it) }
        emitSource(source)

    }

fun <T, A> performGetOperation(
    databaseQuery: () -> LiveData<T>,
    networkCall: suspend () -> Resource<A>,
    saveCallResult: suspend (A) -> Unit
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        val source = databaseQuery.invoke().map { Resource.success(it) }
        emitSource(source)

        val responseStatus = networkCall.invoke()
        if (responseStatus.status == Status.SUCCESS) {
            responseStatus.data?.let { saveCallResult(it) }

        } else if (responseStatus.status == Status.ERROR) {
            emit(Resource.error(responseStatus.message!!))
            emitSource(source)
        }
    }


fun <A> performGetOperationNetwork(
    networkCall: suspend () -> Resource<A>,
    saveCallResult: suspend (A) -> Unit
): LiveData<Resource<A>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        val responseStatus = networkCall.invoke()
        if (responseStatus.status == Status.SUCCESS) {
            saveCallResult(responseStatus.data!!)

        } else if (responseStatus.status == Status.ERROR) {
            emit(Resource.error(responseStatus.message!!))
        }
    }

fun <T> performGetOperationNew1(
    networkCall: suspend () -> Resource<T>,
    saveCallResult: suspend (T) -> Unit
): LiveData<Resource<T>> =
    liveData(Dispatchers.IO) {
        emit(Resource.loading())
        try {
            val responseStatus = networkCall.invoke()
            if (responseStatus.status == Status.SUCCESS) {
                saveCallResult(responseStatus.data!!)
                emit(Resource.success(responseStatus.data!!))

            } else if (responseStatus.status == Status.ERROR) {
                emit(Resource.error(responseStatus.message!!))
            }
        } catch (e: Exception) {
            Log.d("exception123", "::" + e.message)
        }
    }

fun <T> CoroutineScope.executeAsyncTask(
    onPreExecute: () -> Unit,
    doInBackground: () -> T,
    onPostExecute: (T) -> Unit
) = launch {
    onPreExecute()
    val result = withContext(Dispatchers.IO) { // runs in background thread without blocking the Main Thread
        doInBackground()
    }
    onPostExecute(result)
}