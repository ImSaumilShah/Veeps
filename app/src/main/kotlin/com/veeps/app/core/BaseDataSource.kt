package com.veeps.app.core

import android.text.Html
import androidx.core.text.HtmlCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import com.veeps.app.R
import com.veeps.app.application.Veeps
import com.veeps.app.data.network.NoConnectivityException
import com.veeps.app.util.APIConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import retrofit2.Response
import java.io.IOException

abstract class BaseDataSource {

    protected suspend fun <T> getResult(
        tag: String, call: suspend () -> Response<T>,
    ): Resource<T> {
        try {
            val response = call()
            when (response.isSuccessful) {
                true -> {
                    when (response.code()) {
                        200 -> {
                            response.body()?.let {
                                when (tag) {
                                    APIConstants.FETCH_AUTHENTICATION_DETAILS -> {
                                        return Resource.success(tag, response.body()!!)
                                    }

                                    else -> {
                                        return Resource.success(tag, response.body()!!)
                                    }
                                }
                            }
                                ?: return if (tag == APIConstants.VALIDATE_APP_VERSIONS)
                                    Resource.successWithNullResponse(tag, null)
                                else Resource.error(
                                    tag, Veeps.appContext.getString(R.string.unknown_error)
                                )
                        }

                        else -> {
                            if (response.code() == 201 && tag == APIConstants.SUBSCRIPTION_MAPPING) {
                                return Resource.successWithNullResponse(tag, response.body())
                            } else if (tag == APIConstants.REMOVE_WATCH_LIST_EVENTS) {
                                return Resource.successWithNullResponse(tag, response.body())
                            } else {
                                response.body()?.let {
                                    when (tag) {
                                        else -> {
                                            return Resource.success(tag, response.body()!!)
                                        }
                                    }
                                } ?: return Resource.error(
                                    tag, Veeps.appContext.getString(R.string.unknown_error)
                                )
                            }
                        }
                    }
                }

                else -> {
                    when (response.code()) {
                        400 -> {
                            when (tag) {
                                APIConstants.AUTHENTICATION_POLLING -> {
                                    var error = Veeps.appContext.getString(R.string.unknown_error)
                                    response.errorBody()?.let { errorBody ->
                                        val errorObject = JSONObject(errorBody.string())
                                        if (errorObject.has("error_description")) {
                                            error = Html.fromHtml(
                                                errorObject.getString("error"),
                                                HtmlCompat.FROM_HTML_MODE_LEGACY
                                            ).toString()
                                        } else if (errorObject.has("error")) {
                                            error = Html.fromHtml(
                                                errorObject.getString("error"),
                                                HtmlCompat.FROM_HTML_MODE_LEGACY
                                            ).toString()
                                        }
                                    }
                                    return Resource.error(tag, error)
                                }

                                APIConstants.VALIDATE_APP_VERSIONS -> {
                                    var errorMessage: String =
                                        Veeps.appContext.getString(R.string.unknown_error)

                                    response.errorBody()?.let { errorBody ->
                                        val errorBodyObject = JSONObject(errorBody.string())
                                        if (errorBodyObject.has("errors")) {
                                            val errorObjectData = errorBodyObject.getString("errors")
                                            val errorObject = JSONObject(errorObjectData)
                                            if (errorObject.has("message")) {
                                                errorMessage = errorObject.getString("message")
                                            }
                                            if (errorObject.has("error")) {
                                                errorMessage = errorObject.getString("error")
                                            }
                                        }
                                    }
                                    return Resource.error(tag, errorMessage)
                                }

                                else -> {
                                    var error: String =
                                        Veeps.appContext.getString(R.string.unknown_error)
                                    response.errorBody()?.let { errorBody ->
                                        val errorObject = JSONObject(errorBody.string())
                                        if (errorObject.has("message")) {
                                            error = Html.fromHtml(
                                                errorObject.getString("message"),
                                                HtmlCompat.FROM_HTML_MODE_LEGACY
                                            ).toString()
                                        }
                                    }
                                    return Resource.error(tag, error)
                                }
                            }
                        }

                        else -> {
                            var error: String = Veeps.appContext.getString(R.string.unknown_error)
                            response.errorBody()?.let { errorBody ->
                                val errorObject = JSONObject(errorBody.string())
                                if (errorObject.has("message")) {
                                    error = Html.fromHtml(
                                        errorObject.getString("message"),
                                        HtmlCompat.FROM_HTML_MODE_LEGACY
                                    ).toString()
                                }
                            }
                            return Resource.error(tag, error)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return when (e) {
                is NoConnectivityException -> {
                    Resource.error(tag, Veeps.appContext.getString(R.string.no_connectivity_error))
                }

                is IOException -> {
                    Resource.error(tag, Veeps.appContext.getString(R.string.no_connectivity_error))
                }

                else -> {
                    Resource.error(tag, e.message ?: e.toString())
                }
            }
        }
    }

    fun <T> performFlowOperation(
        tag: String, networkCall: suspend () -> Resource<T>,
    ): Flow<Resource<T?>> = flow {
        try {
            emit(Resource.loading(tag, response = null))
            val responseStatus = networkCall.invoke()
            emit(responseStatus)
        } catch (e: Exception) {
            emit(Resource.error(tag, e.message ?: e.toString()))
        }
    }.flowOn(Dispatchers.Main)

    fun <T> performOperation(
        tag: String,
        networkCall: suspend () -> Resource<T>,
    ): LiveData<Resource<T?>> =
        liveData(Dispatchers.IO) {
            try {
                emit(Resource.loading(tag, response = null))
                val responseStatus = networkCall.invoke()
                emit(responseStatus)
            } catch (e: Exception) {
                emit(Resource.error<T>(tag, e.message ?: e.toString()))
            }
        }

    data class Resource<out T>(
        val callStatus: CallStatus, val response: T?, val message: String?, val tag: String,
    ) {
        enum class CallStatus {
            SUCCESS, ERROR, LOADING
        }

        companion object {
            fun <T> success(tag: String, response: T): Resource<T> {
                return Resource(CallStatus.SUCCESS, response, null, tag)
            }

            fun <T> successWithNullResponse(tag: String, response: T?): Resource<T> {
                return Resource(CallStatus.SUCCESS, response, null, tag)
            }

            fun <T> error(tag: String, message: String?): Resource<T> {
                return Resource(CallStatus.ERROR, null, message, tag)
            }

            fun <T> loading(tag: String, response: T? = null): Resource<T> {
                return Resource(CallStatus.LOADING, response, null, tag)
            }
        }
    }
}
