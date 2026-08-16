package com.maxinesworld.corenetwork

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiClient @Inject constructor(
    private val okHttpClient: OkHttpClient? = null,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val defaultOkHttp by lazy {
        okHttpClient ?: OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createSyncService(baseUrl: String = DEFAULT_BASE_URL): SyncApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(defaultOkHttp)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
        return retrofit.create(SyncApiService::class.java)
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://10.10.10.33"
    }
}
