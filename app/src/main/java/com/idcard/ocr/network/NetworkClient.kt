package com.idcard.ocr.network

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Network client configuration and factory
 */
object NetworkClient {

    // Configure your backend URL here
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 60L
    private const val WRITE_TIMEOUT = 60L

    /**
     * Create OkHttpClient with logging and timeouts
     */
    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    /**
     * Create Retrofit instance
     */
    fun createRetrofit(baseUrl: String = BASE_URL): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Create ApiService instance
     */
    fun createApiService(retrofit: Retrofit = createRetrofit()): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}

/**
 * Result wrapper for API operations
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data class Exception(val throwable: Throwable) : ApiResult<Nothing>()
}

/**
 * Extension function to handle API response
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> retrofit2.Response<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            response.body()?.let {
                ApiResult.Success(it)
            } ?: ApiResult.Error("Empty response", response.code())
        } else {
            ApiResult.Error(
                response.errorBody()?.string() ?: "Unknown error",
                response.code()
            )
        }
    } catch (e: java.io.IOException) {
        ApiResult.Error("Network error: ${e.message}")
    } catch (e: Exception) {
        ApiResult.Exception(e)
    }
}
