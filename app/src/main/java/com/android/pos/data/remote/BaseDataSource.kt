package com.android.pos.data.remote

import android.util.Log
import com.android.pos.utils.extensions.NoInternetException
import com.android.pos.utils.statusUtils.Resource
import org.json.JSONObject
import retrofit2.HttpException
import java.net.SocketTimeoutException

enum class ErrorCodesNew(val code: Int) {
    SocketTimeOut(-1)
}

abstract class BaseDataSource {


    protected suspend fun <T> getResult(call: suspend () -> T): Resource<T> {
        return try {
            Resource.success(call.invoke())
        } catch (e: NoInternetException) {
            handleException(e)
        } catch (e: Exception) {
            Log.d("exception", "::" + e.message)
            handleException(e)
        }
    }

    private fun <T : Any> handleException(e: Exception): Resource<T> {
        return when (e) {
            is HttpException -> Resource.error(
                getErrorMessage(
                    e.response()?.errorBody()?.string(),
                    e.code()
                ), null
            )
            is SocketTimeoutException -> Resource.error(
                getErrorMessage("", ErrorCodesNew.SocketTimeOut.code),
                null
            )

            else -> Resource.error(
                getErrorMessage(
                    "",
                    Int.MAX_VALUE,
                    message = (if (e.message == null) "" else e.message).toString()
                ), null
            )
        }
    }


    private fun getErrorMessage(errorBody: String?, code: Int, message: String = ""): String {
        return when (code) {
            ErrorCodesNew.SocketTimeOut.code -> "Timeout"
            401 -> "$code Unauthorised"
            404 -> "$code Not found"
            400 -> showNetworkError(errorBody!!)
            410 -> showNetworkError(errorBody!!)
            500 -> showNetworkError(errorBody!!)
            else -> message
        }
    }

    private fun showNetworkError(errorBody: String): String {
//                   {"data":{},"type":"Error","status":400,"message":"Invalid Email or Password"}
        return try {
            val jsonObject = JSONObject(errorBody.trim())
            if (jsonObject.has("error")) {
                jsonObject.getString("error")
            } else {
                jsonObject.getString("message")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            "Server Error"
        }

    }

}